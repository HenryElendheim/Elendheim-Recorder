# Elendheim Recorder

A native Android recorder for real sessions on the go. Records from the phone
mic, keeps a library you can rename and delete from, and exports files you
actually own — no cloud, no accounts, no analytics.

## What it does

- One-tap recording from the mic, with a live timer and level meter.
- Long sessions survive screen-off and backgrounding (a foreground service
  keeps the mic alive with an ongoing notification).
- A library of your takes: play with a scrubber, rename, and delete.
- Export to WAV (lossless master) or M4A (compact AAC), and share anywhere
  through the Android share sheet.
- Opens with the reusable Elendheim intro — dark gray, soft red.

## Design

Dark-mode first, on purpose. Dark gray (`#2B2B2B`) with a soft red accent
(`#E57373`), the shared look of the Elendheim suite.

The app has no `INTERNET` permission, so it cannot phone home — you can verify
that in the manifest. Recordings live in app-private storage; exporting copies
files out to your Music folder or wherever you choose to share them.

## Building

Open the project in Android Studio (or run `./gradlew assembleDebug` with the
Android SDK installed). Minimum Android 8.0 (API 26).

- Language: Kotlin
- UI: Jetpack Compose
- Audio: `AudioRecord` -> raw PCM -> WAV, written to disk as it records
- Compact export: AAC/M4A via the built-in `MediaCodec` (no NDK)

## Structure

```
com.elendheim.branding      Portable intro module (drop into any Elendheim app)
com.elendheim.recorder
  audio/     Recorder, WavWriter, RecordingService (foreground service)
  export/    AacEncoder, Exporter (MediaStore + share sheet)
  library/   RecordingStore (list, rename, delete)
  ui/        Compose screens, theme, view model
```

The audio engine and file store know nothing about the UI, so they stay
testable and reusable. The `branding` package has no app-specific
dependencies, so it lifts straight into the next app.

## Roadmap

- MP3 export via a bundled LAME encoder (Android has no native MP3 encoder).
- Pause and resume, undo-delete trash, and a small settings screen
  (default format, sample rate, bitrate).

## License

MIT. See [LICENSE](LICENSE).
