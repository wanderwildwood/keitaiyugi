# Handheld Games — Privacy Policy

Version 0.1.17. Last reviewed 27 October 2026.

## The short version

The developer collects nothing about you. There is no account, no sign-up, no analytics, no advertising and no crash reporting, and no server belonging to the developer that the app talks to. The games and what you do in them stay on your phone. The one thing the app fetches is a cover picture for a game, from libretro's public thumbnail server, and the request carries the game's title.

## Who is responsible

wander wildwood, the author of this version of the app. Contact: email@wanderthe.dev

Handheld Games is a fork of Lemuroid by Filippo Scognamiglio (GPL-3.0), cut down to Game Boy and rebuilt on Mudita's public MMD library. The emulator core is Gambatte, run through LibretroDroid.

Because no personal data is transmitted to or held by the developer, there is no data controller relationship for the developer to administer. Where the app processes personal data, that processing happens on your own device, under your control.

## What the app stores, and where

On the phone: the library index (the games found in the folder you chose, with their titles), your settings, the save files and save states with their preview pictures, and a cache of cover pictures. No game is bundled, and none is copied off the phone; the app reads game files from the folder you choose in Settings, picked through Android's file picker.

The app allows Android's backup (`allowBackup="true"`) and sets no backup rules of its own, so whatever Android's default backup covers for an app on your phone, including its settings and library index, may be copied into the backup your phone makes, as your phone's backup settings decide. The developer holds no backup and cannot see yours. Uninstalling the app removes what is in its private storage.

## Permissions, and what each is for

From `lemuroid-app/src/main/AndroidManifest.xml`:

- `INTERNET`: for the cover pictures described below.
- `ACCESS_NETWORK_STATE`: so background work knows whether a network is there.
- `POST_NOTIFICATIONS`: the notification shown while the library is scanned or a game is running.
- `READ_EXTERNAL_STORAGE`: to read game files from the folder you point the app at (inherited from Lemuroid).
- `VIBRATE`: haptic feedback from the on-screen controls, and game rumble.
- `FOREGROUND_SERVICE_DATA_SYNC`: the kind of service the library scan and the game service are.

The libraries the app is built on add these, and the store lists them: `FOREGROUND_SERVICE`, `WAKE_LOCK` and `RECEIVE_BOOT_COMPLETED`, used by Android's WorkManager for background work.

The store also lists `com.wanderwildwood.keitaiyugi.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` because it reads the APK. AndroidX declares it for its own internal receivers; it is signature-level and namespaced to this app, so nothing else can hold it and it grants no access to anything on the phone.

There is no microphone, camera, location or contacts permission. Plain http is not permitted (`cleartextTrafficPermitted="false"`).

## Who else receives anything

One host: **thumbnails.libretro.com**, libretro's public cover-art server, over https. When a game's cover is shown in the library, the app asks for its picture. The address asked for contains the game's title and the name of the system (Game Boy); the server also sees your IP address, as any web request does. Nothing else is sent: no identifier, no account. Pictures are kept in a cache on the phone; a cover the server does not have is shown as a plain tile.

The Gambatte core is inside the app. The code that can download a core from GitHub exists in the source, from Lemuroid, but it looks for a bundled core first and finds it, so it does not run in this build.

In About, "feed the llamas" opens a donation page (square.link) and wanderthe.dev opens the author's website, in your browser and only when you press them. The app does not fetch either page and learns nothing about whether you went.

No analytics, advertising or crash-reporting service is contacted. No data is sold, rented, or shared for advertising. There is no profiling, no personalization, and no automated decision-making about you.

## Legal basis (EU / EEA and UK)

Processing on the device is carried out for your own purposes and at your own direction. Where the app contacts an outside service, that request is made at your request and is necessary to provide the feature you asked for (Article 6(1)(b) GDPR); that operator is an independent controller for what it then does, under its own policy.

## Your rights

Under the GDPR and UK GDPR you have rights of access, rectification, erasure, restriction, portability and objection. Under the CCPA/CPRA and comparable North American laws you have rights to know, delete, correct and opt out of sale or sharing.

The developer holds no data about you, so there is nothing to request from them, and nothing has ever been sold or shared. You can exercise every one of these rights directly: the data is on your phone. Clear it in the app or in Android's app settings, or uninstall to remove all of it at once. For what libretro keeps of a request, ask them.

## Children

The app is suitable for all ages and does not knowingly collect anything from anyone, of any age, because the developer does not collect anything.

## Changes

If a later version changes what the app stores or which hosts it contacts, this policy is updated in the same release and the change is described in the release notes. The source is public, so any claim here can be checked against the code: <https://github.com/wanderwildwood/keitaiyugi>. GitHub reports its licence as GPL-3.0.
