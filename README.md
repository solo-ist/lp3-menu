# Menu — a second toolbox for the Light Phone III

A plain Android app that appears in the LightOS toolbox as **Menu** and opens a
curated, renameable list of the full Android apps you'd rather not have
cluttering the real toolbox — Claude, Slack, 1Password, Home Assistant,
Bluesky, Obtainium, the Apple TV remote.

Drawn to match the LightOS toolbox: centred Akkurat Light on black, six rows to
a page, page dots down the right edge, swipe to page.

- **Tap** a row to launch it
- **Long-press** to Rename, Move up, Move down, or Remove
- **`+ add`** at the end lists every launchable app not already in the menu

Entries whose app has been uninstalled disappear on the next resume, so the
list can't rot.

## Why it isn't a light-sdk tool

It can't be. A light-sdk tool is forbidden from launching another app, and this
is enforced at four independent layers:

1. `LightSdkPlugin.kt` scans `tool/src` at Gradle configure time and fails the
   build on `android.content.Intent`, `startActivity(`, `getSystemService(`
   and reflection.
2. There is no reachable `Context` — `SealedLightContext.androidContext` is
   `internal` to `:sdk:client`, and a lint rule blocks `LocalContext.current`.
3. The manifest is generated from `lighttool.toml` with `<queries>` hardcoded
   to the SDK marker, and `QUERY_ALL_PACKAGES` is not among the eleven
   permissions on the allowlist.
4. `LightServiceMethod` exposes 13 methods and none of them launch anything;
   `OpenDialer` is the only handoff, and Light has confirmed
   ([light-sdk#191](https://github.com/lightphone/light-sdk/issues/191)) that
   even opening a system settings screen needs a new Light-authored method.

Production LightOS nevertheless enumerates ordinary `MAIN`/`LAUNCHER`
activities — verified on-device, since Obtainium, Molly Light and the gi-os
tools all appear in the toolbox and none is an SDK tool. So this is an ordinary
Android app, built entirely outside the light-sdk scaffold, and it shows up
like any other.

That mechanism is **not** documented in the public SDK (the emulator lists only
marker-carrying tools), so it could change in any LightOS update. Nothing here
is supported.

## Matching the toolbox

Everything visual lives in [`Style.java`](app/src/main/java/ist/solo/menu/Style.java),
measured against a screenshot of the real toolbox on `582-release-lp3`
(1080×1240 @ 480dpi, scale 3.0):

| | Value | How it was derived |
|---|---|---|
| Row pitch | 63dp | 190px between row tops |
| Text size | 34sp | width-matched: real "Calculator" is 513px for ten glyphs |
| Alignment | centred | every row shares centre x = 540 |
| Rows per page | 6 | counted |
| Dot rail | right edge, 27dp pitch | centre x ≈ 1016 |

**Typeface:** LightOS maps the system `sans-serif` family to **Akkurat LL** in
`/system/etc/fonts.xml`, so asking for `sans-serif-light` yields
`AkkuratLLTT-Light` — the toolbox's real font, with nothing to ship and no
licensing question.

## Permissions

`QUERY_ALL_PACKAGES` only. The list is user-editable, so targets can't be
enumerated at build time the way a single-purpose shim could — it's protection
level `normal` and granted at install. Nothing leaves the device; the menu is a
JSON blob in `SharedPreferences`.

## Build

No dependencies beyond the Android platform — no Kotlin, no Compose, no
AndroidX.

```sh
JAVA_HOME=$(/usr/libexec/java_home) ./gradlew :app:assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

The debug build is signed with the default debug key. Give it a real signing
identity before treating an update as authenticated — see the `lightsdk-dev`
cautionary tale in [SoloPhone's SIDELOADS.md](https://github.com/solo-ist/solophone/blob/main/SIDELOADS.md).

## Status

MVP. Known rough edges:

- The last page centres a short row rather than top-aligning it; unverified
  against how the real toolbox handles a partial final page.
- No reordering beyond one-step Move up / Move down.
- Long labels ellipsize rather than scaling down.
- Page turns are instant — the real toolbox may animate.
