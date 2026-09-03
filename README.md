# Selah

*Scripture in the moments between.*

Selah is a small, private Android app that helps you memorize Bible verses using a habit you
already have — unlocking your phone. After you unlock, it shows a verse for a few seconds, then
quietly disappears. It never replaces your lock screen and never blocks access to your phone. It's a
gentle reminder, not a gate.

> **Selah** (Hebrew): a pause — to stop and reflect.

## How it works

- A lightweight foreground service listens for the device-unlock event (`ACTION_USER_PRESENT`).
- On unlock it presents a verse as an overlay for a configurable few seconds, then auto-dismisses.
- A quiet **Selah** button holds the verse open when you want to reflect longer.

It's a scripture *presenter* — a pause and a moment of reflection, not a quiz or a memorization
drill. (Seeing scripture regularly still helps you remember it — that just isn't the point.)

## Features

- Verse-after-unlock overlay — full-screen, or a small non-blocking card
- **Sets** of verses; choose which set feeds the rotation
- Logos-style Bible browser: a color-coded book grid → chapter → verse, read in context
- Bundled **King James Version** (public domain) to browse and pick from
- **Import** any Bible in the Beblia "Holy-Bible-XML-Format" — bring your own translation
- Type your own verses in any translation
- Configurable display duration (2–60s), how often it appears (every unlock … at most every 2 hours),
  text size, and light/dark theme
- Fully offline — no account, no network, no analytics

## Privacy

Selah runs entirely on your device. It requests **no internet permission**, stores nothing in the
cloud, and collects no data about you.

## Bible text & licensing

- The bundled **King James Version** is public domain.
- Copyrighted translations (ESV, NIV, etc.) are **not** distributed with the app. Import a
  translation you legally have as a Bible XML file, or type verses in yourself.
- This project's **code** is licensed under the GNU GPL-3.0 (see [LICENSE](LICENSE)). The license
  governs the source code, not the scripture text.

## Permissions

| Permission | Why |
|---|---|
| Display over other apps (`SYSTEM_ALERT_WINDOW`) | Draw the verse overlay after unlock |
| Foreground service (`specialUse`) + notifications | Stay ready to catch the unlock event; show the required ongoing notification |
| Run at boot (`RECEIVE_BOOT_COMPLETED`) | Resume after a restart |

No location, contacts, storage, camera, microphone, or network permissions.

## Build

- JDK 21
- Set your Android SDK path in `local.properties` (`sdk.dir=/path/to/Android/Sdk`)
- `./gradlew :app:assembleDebug`
- Toolchain: AGP 9.2.1 · Kotlin 2.2.10 · Jetpack Compose · Room · minSdk 26 / targetSdk 36

## Reliability note

Some manufacturers (Samsung, Xiaomi, Oppo, and others) aggressively stop background apps, which can
prevent the verse from appearing. If it stops working, allow Selah to run in the background / disable
battery optimization and enable autostart for it in your system settings.

## Status

An early, personal project — simple by design. Built for my own use and shared in case it helps
others. Contributions and forks are welcome under the GPL.

## Credits

By **FanStaF**. Bundled KJV from the [Beblia Holy-Bible-XML-Format](https://github.com/Beblia/Holy-Bible-XML-Format)
project (public domain).

## License

[GNU General Public License v3.0](LICENSE).
