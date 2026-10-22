# Changelog

## 1.5.2 — Unreleased

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
