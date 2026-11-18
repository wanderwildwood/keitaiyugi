# Handheld Games — Privacy Policy

Version 0.1.40. Last reviewed 17 November 2026.

## The short version

The developer collects nothing about you. There is no account, no sign-up, no analytics, no advertising and no crash reporting, and no server belonging to the developer that the app talks to. The games and what you do in them stay on your phone. The one thing the app fetches from the internet is a cover picture for a game, from libretro's public thumbnail server, and the request carries the game's title. If you give it the address of a game box of your own, it also shares games, saves and cheats with that box, over your own network.

## Where it comes from

Handheld Games is a fork of Lemuroid by Filippo Scognamiglio (GPL-3.0), cut down to Game Boy and rebuilt on Mudita's public MMD library. The emulator core is Gambatte, run through LibretroDroid.

## What the app stores, and where

On the phone: the library index (the games found in the folder you chose, with their titles), your settings, the save files and save states with their preview pictures, and a cache of cover pictures. No game is bundled, and none is copied off the phone; the app reads game files from the folder you choose in Settings, picked through Android's file picker.

**Game box** (Settings) is off until you give it an address. With one set, the app keeps in its own storage the Game Boy games and cheat files it copied from that box, and a copy of any save that a sync replaced (`saves-replaced`), so nothing is lost when a newer save comes in.

**Save a backup of your saves** (Settings) writes the save files, save states and their preview pictures to one zip file you choose through the phone's own file picker, and only when you ask. **Restore saves from a backup** reads such a file the same way, checks all of it first, and only then puts the saves in place, each over the same game's save; a wrong file changes nothing.

Nothing of the app goes into Android's backup or a phone-to-phone transfer (`allowBackup="false"`, and data-extraction rules that leave out everything); Save a backup of your saves is the way to move them. The developer holds no backup and cannot see yours. Uninstalling the app removes what is in its private storage.

## Permissions, and what each is for

From `lemuroid-app/src/main/AndroidManifest.xml`:

- `INTERNET`: for the cover pictures described below, and to reach a game box on your own network if you have set one.
- `ACCESS_NETWORK_STATE`: so background work knows whether a network is there, and so the game box is tried on its own only on Wi-Fi or a wired network; Sync now tries it on whatever network the phone is on.
- `POST_NOTIFICATIONS`: the notification shown while the library is scanned or a game is running.
- `VIBRATE`: haptic feedback from the on-screen controls, and game rumble.
- `FOREGROUND_SERVICE_DATA_SYNC`: the kind of service the library scan and the game service are.

The libraries the app is built on add these, and the store lists them: `FOREGROUND_SERVICE`, `WAKE_LOCK` and `RECEIVE_BOOT_COMPLETED`, used by Android's WorkManager for background work.

The store also lists `com.wanderwildwood.keitaiyugi.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` because it reads the APK. AndroidX declares it for its own internal receivers; it is signature-level and namespaced to this app, so nothing else can hold it and it grants no access to anything on the phone.

There is no microphone, camera, location or contacts permission. Plain http is not permitted (`cleartextTrafficPermitted="false"`).

## Who else receives anything

One host: **thumbnails.libretro.com**, libretro's public cover-art server, over https. When a game's cover is shown in the library, the app asks for its picture. The address asked for contains the game's title and the name of the system (Game Boy); the server also sees your IP address, as any web request does. Nothing else is sent: no identifier, no account. Pictures are kept in a cache on the phone; a cover the server does not have is shown as a plain tile.

**A game box, only if you set one.** In Settings you can give the address of a Batocera box on your own network. The app then reads that box's shared folder (the file share Batocera offers on the home network, as a guest): it copies the Game Boy and Game Boy Color games and their cheat files to the phone, and keeps each game's battery save in step in both directions, when the app opens, when a game opens or closes, and when you press Sync now. When it sends a save it moves the box's older save, and the box's own resume point for that game, into the box's `saves-sync-backup` folder rather than deleting them. All of this goes between the phone and the address you typed and nowhere else; it is not encrypted, as is usual for a file share on a home network. The developer has no part in it.

The Gambatte core is inside the app, and it is the only core the app will load. Lemuroid's code that downloaded a missing core from GitHub has been taken out: a core that is not in the app fails to load instead.

In About, "feed the llamas" opens a donation page (square.link) and wanderthe.dev opens the author's website, in your browser and only when you press them. The app does not fetch either page and learns nothing about whether you went.

No analytics, advertising or crash-reporting service is contacted. No data is sold, rented, or shared for advertising. There is no profiling, no personalization, and no automated decision-making about you.

## Legal basis (EU / EEA and UK)

Processing on the device is carried out for your own purposes and at your own direction. Where the app contacts an outside service, that request is made at your request and is necessary to provide the feature you asked for (Article 6(1)(b) GDPR); that operator is an independent controller for what it then does, under its own policy.

## Children

The app is suitable for all ages and does not knowingly collect anything from anyone, of any age, because the developer does not collect anything.

<!-- shared-about privacy section -->
## Who we are

Handheld Games is made by wander wildwood, an independent developer: one person, not a company. wander wildwood is the data controller for anything the app itself handles, and that is very little. What you put in Handheld Games stays on your phone, or goes to a game box of your own if you have set one. The only things that go out to anyone else are the requests for cover pictures to libretro's cover-picture servers (thumbnails.libretro.com) described above, each carrying a game's title and the name of the system (Game Boy). The developer receives nothing and holds no data about you.

## How to contact us

Write to email@wanderthe.dev about your data, this policy or anything else. The same address is in the app, under About.

## Your rights

If you are in the EU, the EEA or the UK, the GDPR gives you the right to:

- see the personal data held about you (access)
- have it corrected (rectification) or deleted (erasure)
- limit how it is used (restriction)
- object to its use (objection)
- get a copy to take elsewhere (portability)
- withdraw your consent at any time, where something rests on it
- complain to a data protection supervisory authority, for example the one where you live

Similar laws elsewhere, such as the CCPA in California, give you the right to know, delete and correct, and to opt out of the sale or sharing of your data. Nothing from Handheld Games is sold or shared.

Since Handheld Games keeps what it stores only on your phone, you can exercise most of these rights yourself: take a copy of your saves with Save a backup of your saves in Settings, clear the app's data in Android's settings, or uninstall the app, which deletes all of it. For anything you cannot do yourself, write to the address above.

Handheld Games connects to libretro's cover-picture servers (thumbnails.libretro.com). They are separate from this app and from its developer, and what they receive is covered by their own privacy policies; ask them about what they hold.
<!-- /shared-about privacy section -->

## Changes

If a later version changes what the app stores or which hosts it contacts, this policy is updated in the same release and the change is described in the release notes. The source is public, so any claim here can be checked against the code: <https://github.com/wanderwildwood/keitaiyugi>. GitHub reports its license as GPL-3.0.
