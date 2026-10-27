package com.swordfish.lemuroid.lib.library.db.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.RawQuery
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteQuery
import com.swordfish.lemuroid.lib.library.db.entity.Game

class GameSearchDao(private val internalDao: Internal) {
    object CALLBACK : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            MIGRATION.migrate(db)
        }
    }

    object MIGRATION : Migration(7, 8) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL(
                """
                CREATE VIRTUAL TABLE fts_games USING FTS4(
                  tokenize=unicode61 "remove_diacritics=1",
                  content="games",
                  title);
                """,
            )
            database.execSQL(
                """
                CREATE TRIGGER games_bu BEFORE UPDATE ON games BEGIN
                  DELETE FROM fts_games WHERE docid=old.id;
                END;
                """,
            )
            database.execSQL(
                """
                CREATE TRIGGER games_bd BEFORE DELETE ON games BEGIN
                  DELETE FROM fts_games WHERE docid=old.id;
                END;
                """,
            )
            database.execSQL(
                """
                CREATE TRIGGER games_au AFTER UPDATE ON games BEGIN
                  INSERT INTO fts_games(docid, title) VALUES(new.id, new.title);
                END;
                """,
            )
            database.execSQL(
                """
                CREATE TRIGGER games_ai AFTER INSERT ON games BEGIN
                  INSERT INTO fts_games(docid, title) VALUES(new.id, new.title);
                END;
                """,
            )
            database.execSQL(
                """
                INSERT INTO fts_games(docid, title) SELECT id, title FROM games;
                """,
            )
        }
    }

    fun search(query: String): PagingSource<Int, Game> {
        val match = buildMatchQuery(query)
        // A blank query, or one with nothing but symbols, matches nothing: handing it to MATCH
        // would be a syntax error.
        if (match == null) {
            return internalDao.rawSearch(SimpleSQLiteQuery("SELECT * FROM games WHERE 0"))
        }
        return internalDao.rawSearch(
            SimpleSQLiteQuery(
                """
                SELECT games.*
                    FROM fts_games
                    JOIN games ON games.id = fts_games.docid
                    WHERE fts_games MATCH ?
                    ORDER BY games.title COLLATE NOCASE ASC, games.id DESC
                """,
                arrayOf(match),
            ),
        )
    }

    companion object {
        /**
         * What the user typed, as a query the full-text index accepts: each word (letters and
         * digits only, so quotes, minus, colon, parentheses and the like never reach the parser)
         * becomes a prefix, and all of them have to match. "mar" finds "Mario" as well as "Mar".
         * Null when nothing searchable is left.
         */
        fun buildMatchQuery(raw: String): String? {
            val words =
                raw.split(Regex("[^\\p{L}\\p{N}]+"))
                    .filter { it.isNotEmpty() }
            if (words.isEmpty()) return null
            return words.joinToString(" ") { "$it*" }
        }
    }

    @Dao
    interface Internal {
        @RawQuery(observedEntities = [(Game::class)])
        fun rawSearch(query: SupportSQLiteQuery): PagingSource<Int, Game>
    }
}
