# Changelog

## 1.5.5 — unreleased

- Ktor: capture in-memory `ByteArray` request bodies (any `ByteArrayContent`),
  not only `TextContent`, and log the body's `Content-Type` with the request
  headers. Clients that send serialized bytes, and the calls that fail, now show
  their request body.
- Ktor: capture textual response bodies without `Content-Length` (chunked, or gzip
  that OkHttp/Darwin already decoded) up to `maxBodyBytes`. Unknown-length
  streaming types (`text/event-stream`, NDJSON, JSON sequences) are not read, so
  the caller never waits for an open stream.
- Publish every release tag to JitPack as well as Maven Central:
  `com.github.koai-dev.netlogger-compose:netlogger:<tag>` with Android and iOS
  publications, unsigned on JitPack (`jitpack.yml`). Pushing a release tag also
  starts the Maven Central upload, which checks the tag against `VERSION_NAME`.

## 1.5.4 — 2026-11-17

- Update Kotlin to 2.4.21, Compose Multiplatform to 1.12.2, Android Gradle
  Plugin to 9.5.1 and Gradle to 9.6.0.
- Update AndroidX, Room, Coroutines, Serialization, Ktor and OkHttp dependencies.
- Align the Android sample with the dependencies' minimum `compileSdk` of 37.
- Enable the existing UIKit host-app tests in the shared Xcode scheme.

## 1.5.2

- Fix an iOS `SIGSEGV` when `enableLogcatOutput = true` by writing literal text
  with `println` instead of passing a Kotlin string to variadic `NSLog`.
  Network and general console logs share this fix.
- Add iOS regression tests using the real native console writer, background
  general logging, and Ktor MockEngine requests at INFO and ALL, including
  redaction, Unicode, format markers, and chunking.
- Store Netlogger native source paths relative to the repository root in newly
  compiled KLIBs to avoid embedding the publisher's checkout path in generated
  bridge symbols.
- Document [confirmed 1.5.1 crash evidence and consumer upgrade steps](docs/ios-console-crash.md).
- Add opt-in iOS shake-to-open using UIKit motion events, scene-aware window
  factories, persistent Settings, a host-window forwarding API, and guarded modal
  presentation with manual opening and dismissal.
- Add shared Android/iOS Settings UI tests, Android sensor-to-viewer UI coverage,
  UIKit host-app presentation tests,
  emulator CI and a local UI verification script.
- Fix shared vector icons using Android-only color references, which failed to
  render on iOS; migrate the sample to the scene-based UIKit lifecycle.
- Ignore late Android shake sensor callbacks after the setting is disabled.
