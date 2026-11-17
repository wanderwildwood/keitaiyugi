package com.swordfish.lemuroid.lib.box

import com.hierynomus.msdtyp.AccessMask
import com.hierynomus.mserref.NtStatus
import com.hierynomus.msfscc.FileAttributes
import com.hierynomus.mssmb2.SMB2CreateDisposition
import com.hierynomus.mssmb2.SMB2CreateOptions
import com.hierynomus.mssmb2.SMB2ShareAccess
import com.hierynomus.mssmb2.SMBApiException
import com.hierynomus.protocol.commons.socket.ProxySocketFactory
import com.hierynomus.security.bc.BCSecurityProvider
import com.hierynomus.smbj.SMBClient
import com.hierynomus.smbj.SmbConfig
import com.hierynomus.smbj.auth.AuthenticationContext
import com.hierynomus.smbj.common.SMBRuntimeException
import com.hierynomus.smbj.connection.Connection
import com.hierynomus.smbj.session.Session
import com.hierynomus.smbj.share.DiskShare
import java.io.File
import java.io.IOException
import java.util.EnumSet
import java.util.concurrent.TimeUnit

/**
 * The box's share over SMB, which is how Batocera shares its user data out of the box: a share
 * called `share`, open to guests. Nothing has to be set up on the box for this to reach it.
 *
 * [timeoutSeconds] bounds the connection and every request after it. Before a game opens it is
 * a couple of seconds, so a phone that is away from home starts the game almost at once; a sync
 * asked for by hand waits longer.
 */
class SmbBoxShare private constructor(
    private val client: SMBClient,
    private val connection: Connection,
    private val session: Session,
    private val disk: DiskShare,
) : BoxShare {
    override fun list(folder: String): List<BoxEntry> =
        guarded {
            try {
                disk.list(smb(folder))
                    // A name that could reach outside the folder it is copied into is passed over.
                    .filter { BoxNames.isPlainName(it.fileName) }
                    .map { info ->
                        val isFolder =
                            (info.fileAttributes and FileAttributes.FILE_ATTRIBUTE_DIRECTORY.value) != 0L
                        BoxEntry(
                            name = info.fileName,
                            isFolder = isFolder,
                            size = if (isFolder) 0 else info.endOfFile,
                            modified = info.lastWriteTime.toEpochMillis(),
                        )
                    }
            } catch (e: SMBApiException) {
                if (e.status in MISSING) emptyList() else throw e
            }
        }

    override fun read(path: String): ByteArray =
        guarded {
            openForRead(path).use { file -> file.inputStream.use { readCapped(it, MAX_READ) } }
        }

    override fun download(
        path: String,
        to: File,
    ) = guarded {
        openForRead(path).use { file ->
            file.inputStream.use { input -> to.outputStream().use { copyCapped(input, it, MAX_DOWNLOAD) } }
        }
        Unit
    }

    override fun write(
        path: String,
        data: ByteArray,
    ) = guarded {
        makeFolders(path.substringBeforeLast('/', ""))
        disk.openFile(
            smb(path),
            EnumSet.of(AccessMask.GENERIC_WRITE),
            null,
            SMB2ShareAccess.ALL,
            SMB2CreateDisposition.FILE_OVERWRITE_IF,
            EnumSet.of(SMB2CreateOptions.FILE_NON_DIRECTORY_FILE),
        ).use { file -> file.outputStream.use { it.write(data) } }
    }

    override fun move(
        from: String,
        to: String,
    ) = guarded {
        makeFolders(to.substringBeforeLast('/', ""))
        disk.open(
            smb(from),
            EnumSet.of(AccessMask.DELETE, AccessMask.GENERIC_READ),
            null,
            SMB2ShareAccess.ALL,
            SMB2CreateDisposition.FILE_OPEN,
            null,
        ).use { entry -> entry.rename(smb(to), true) }
    }

    override fun close() {
        // A polite close sends a logoff and waits for an answer; a box that has gone quiet
        // never gives one, so the socket is shut outright.
        runCatching { connection.close(true) }
        runCatching { client.close() }
    }

    private fun openForRead(path: String) =
        disk.openFile(
            smb(path),
            EnumSet.of(AccessMask.GENERIC_READ),
            null,
            SMB2ShareAccess.ALL,
            SMB2CreateDisposition.FILE_OPEN,
            null,
        )

    private fun makeFolders(folder: String) {
        if (folder.isEmpty()) return
        var path = ""
        for (part in folder.split('/')) {
            path = if (path.isEmpty()) part else "$path/$part"
            if (!disk.folderExists(smb(path))) disk.mkdir(smb(path))
        }
    }

    private fun <T> guarded(block: () -> T): T =
        try {
            block()
        } catch (e: SMBRuntimeException) {
            throw BoxUnreachableException(e.message ?: "share", e)
        } catch (e: IOException) {
            if (e is BoxUnreachableException) throw e
            throw BoxUnreachableException(e.message ?: "share", e)
        }

    companion object {
        const val SHARE_NAME = "share"

        // Saves and cheat files are kilobytes and a handheld game a few megabytes; anything
        // past these is not one, and is refused rather than read into memory or onto the card.
        const val MAX_READ = 8L shl 20
        const val MAX_DOWNLOAD = 128L shl 20

        fun readCapped(input: java.io.InputStream, max: Long): ByteArray {
            val out = java.io.ByteArrayOutputStream()
            copyCapped(input, out, max)
            return out.toByteArray()
        }

        fun copyCapped(input: java.io.InputStream, output: java.io.OutputStream, max: Long) {
            val buffer = ByteArray(1 shl 16)
            var total = 0L
            while (true) {
                val n = input.read(buffer)
                if (n < 0) return
                total += n
                if (total > max) throw IOException("file on the box is larger than $max bytes")
                output.write(buffer, 0, n)
            }
        }

        private val MISSING =
            setOf(
                NtStatus.STATUS_OBJECT_NAME_NOT_FOUND,
                NtStatus.STATUS_OBJECT_PATH_NOT_FOUND,
                NtStatus.STATUS_NO_SUCH_FILE,
            )

        fun smb(path: String): String = path.replace('/', '\\')

        private val PORT = Regex("""^[^:]+:(\d{1,5})$""")

        /** [address] is a name or number, with `:port` after it when the box does not use the usual one. */
        fun connect(
            address: String,
            timeoutSeconds: Long,
        ): SmbBoxShare {
            val port = PORT.find(address)?.groupValues?.get(1)?.toIntOrNull()
            val host = if (port != null) address.substringBeforeLast(':') else address
            val client =
                SMBClient(
                    SmbConfig.builder()
                        .withSecurityProvider(BCSecurityProvider())
                        .withSocketFactory(ProxySocketFactory((timeoutSeconds * 1000).toInt()))
                        .withTimeout(timeoutSeconds, TimeUnit.SECONDS)
                        // No read timeout on the socket itself: smbj reads it on its own thread,
                        // which sits idle between requests and takes a timeout there as fatal.
                        .withSoTimeout(0)
                        .withBufferSize(1 shl 18)
                        // Every listing goes to the box: smbj otherwise answers a second listing
                        // of a folder from memory and misses what this app itself just wrote.
                        .withDirectoryLeasingEnabled(false)
                        .build(),
                )
            var connection: Connection? = null
            try {
                connection = if (port != null) client.connect(host, port) else client.connect(host)
                val session = connection.authenticate(AuthenticationContext.guest())
                val disk =
                    session.connectShare(SHARE_NAME) as? DiskShare
                        ?: throw BoxUnreachableException("no share")
                return SmbBoxShare(client, connection, session, disk)
            } catch (e: Exception) {
                runCatching { connection?.close(true) }
                runCatching { client.close() }
                if (e is BoxUnreachableException) throw e
                throw BoxUnreachableException(e.message ?: host, e)
            }
        }
    }
}
