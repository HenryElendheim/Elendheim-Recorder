# Elendheim Recorder

A native Android recorder for real sessions on the go. Records from the phone
mic, keeps a library you can rename and delete from, and exports files you
actually own — no cloud, no accounts, no analytics.

## What it does

- One-tap recording from the mic, with a live timer and level meter.
- Long sessions survive screen-off and backgrounding (a foreground service
  keeps the mic alive with an ongoing notification).
- A library of your takes: play with a scrubber, rename, delete, organise into
  folders, and search by name or date (type "July" or "07" to find that month).
- New recordings get a name you set that counts up automatically, with the date
  kept as its own field.
- Export to WAV (lossless master) or MP3 (compact), saved wherever you choose
  or shared through the Android share sheet.
- A settings menu with accessibility options: high contrast, a pitch display
  (C4, F#4 and so on) while recording and while playing back, an optional piano
  roll that visualises a recording's notes over time, and headphone monitoring
  so you can hear yourself as you go.
- Opens with the reusable Elendheim intro.

## Design

Dark-mode first, on purpose. Dark gray (`#2B2B2B`) with a soft red accent
(`#E57373`), the shared look of the Elendheim suite. A high-contrast mode in
settings switches to pure black and white with a brighter accent.

The app has no `INTERNET` permission, so it cannot phone home — you can verify
that in the manifest. Recordings live in app-private storage; exporting copies
files out to your Music folder or wherever you choose to share them.

## Building

Open the project in Android Studio (or run `./gradlew assembleDebug` with the
Android SDK installed). Minimum Android 8.0 (API 26).

- Language: Kotlin
- UI: Jetpack Compose
- Audio: `AudioRecord` -> raw PCM -> WAV, written to disk as it records
- MP3 export: LAME, bundled as a prebuilt native library (no NDK build here)
- Pitch: a YIN-style estimator over the live buffer

## Structure

```
com.elendheim.branding      Portable intro module (drop into any Elendheim app)
com.elendheim.recorder
  audio/     Recorder, WavWriter, RecordingService, PitchDetector
  export/    Mp3Encoder (LAME), Exporter (Files + share sheet)
  library/   RecordingStore (list, rename, delete, folders)
  data/      SettingsStore
  ui/        Compose screens, theme, view model
```

The audio engine and file store know nothing about the UI, so they stay
testable and reusable. The `branding` package has no app-specific
dependencies, so it lifts straight into the next app.

## Roadmap

- Pause and resume, undo-delete trash.
- Per-recording bitrate/sample-rate choices in settings.

## License

MIT. See [LICENSE](LICENSE).
