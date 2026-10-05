# Netlogger Compose

A network logging library for Android and iOS, built with Kotlin Multiplatform, Compose Multiplatform, Room, and Clean Architecture.

## Features
- **Real-time Logging**: Intercept and view all network requests and responses.
- **Beautiful JSON Viewer**: Expandable/collapsible JSON tree with syntax highlighting.
- **Global Search**: Search for any text in log details (URLs, Headers, Bodies) with navigation arrows.
- **Advanced Filtering**: Filter logs by Method (GET, POST, etc.) and Status Code (2xx, 4xx, etc.).
- **Shake to Open (Android)**: Instantly open the log list by shaking your device.
- **Floating Button (Android)**: Optional floating shortcut for quick access.
- **cURL Export**: Easily copy any request as a cURL command.
- **Auto-reset**: Configurable option to clear old logs on app startup.

## Project structure and local builds

- `netlogger/src/commonMain`: shared models, use cases, Room schema/DAO/repository, redaction, Ktor capture, Compose screens and ViewModels.
- `netlogger/src/androidMain`: Android initialization, Koin integration, OkHttp interceptor, SharedPreferences, activity, shake/FAB and clipboard/console adapters.
- `netlogger/src/iosMain`: Darwin integration, Room database builder, NSUserDefaults settings, UIKit controllers and clipboard/console adapters.
- `app`: Android sample application.
- `iosApp/NetloggerSample.xcodeproj`: iOS sample application hosting shared Compose UI.
- `commonTest`, `androidHostTest`, `iosTest`: shared capture/redaction tests, Android compatibility tests, and native Room storage tests.

Requirements: JDK 21, Android SDK 36 and 36.1, macOS with full Xcode and an iOS Simulator runtime. The iOS minimum deployment target is 16.0; Android minSdk is 24. Native targets are `iosArm64` (devices) and `iosSimulatorArm64` (Apple Silicon simulators).

```bash
# Android APKs and host tests
./gradlew :app:assembleDebug :app:assembleRelease :netlogger:testAndroidHostTest

# iOS frameworks and native tests
export DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer
./gradlew :netlogger:linkDebugFrameworkIosArm64 :netlogger:linkDebugFrameworkIosSimulatorArm64 :netlogger:iosSimulatorArm64Test

# Both platforms, including Debug/Release iOS sample builds
./scripts/verify-builds.sh

# On machines with limited disk space, remove framework intermediates after each build
NETLOGGER_CLEAN_FRAMEWORK_OUTPUTS=1 ./scripts/verify-builds.sh
```

Open `iosApp/NetloggerSample.xcodeproj` in Xcode and select the `NetloggerSample` scheme. Its build phase runs `embedAndSignAppleFrameworkForXcode`; Gradle chooses the device/simulator and Debug/Release framework from Xcode's environment. For running on a physical device, set your development team in Xcode. The verification script compiles device apps without signing and does not produce a distribution archive. The host `Info.plist` must set `CADisableMinimumFrameDurationOnPhone` to `true` for Compose iOS; the sample includes this required setting.

This migration is version `1.5.0` in the source tree; it has not been published. Consume the local module with `implementation(project(":netlogger"))` in a KMP source set, or `debugImplementation(project(":netlogger"))` in an Android host. `./gradlew :netlogger:publishToMavenLocal` publishes KMP metadata and platform artifacts under `com.koai:netlogger:1.5.0`. Older JitPack releases are Android-only.

## Ktor and iOS usage

Initialize on the main thread before creating the client or viewer:

```kotlin
// iOS Kotlin host
Netlogger.init(NetloggerConfig(
    maximumLogLevel = LogLevel.ALL,
    captureBodies = true,
    captureGeneralLogs = true
))
val client = HttpClient(Darwin) {
    Netlogger.configureClient(this)
}
val controller = NetloggerViewController(onClose = { /* dismiss from host */ })
```

On Android, call `Netlogger.init(application, config)` before `Netlogger.configureClient(this)` in a Ktor client. The existing `Netlogger.getInterceptor()` API remains available for OkHttp. Install one capture adapter per client to avoid duplicate entries.

For a common Kotlin host that owns its own repositories, the `com.netlogger.lib.network.netlogger` extension accepts the configuration, `SettingsRepository`, and `INetloggerRepository` directly. The application retains ownership of its HTTP client and should close it normally.

Ktor captures bounded `TextContent` request bodies and known-length textual response bodies when body capture is approved. Binary, compressed, unknown-length and oversized response bodies, and streaming/upload request bodies, are omitted. The original response bytes remain available to the caller; captured values are redacted before entering the bounded queue. API logging and body capture always respect both configured caps and current settings.

The shared viewer supports list/detail, JSON expansion, search, filters, tab ordering, settings and cURL export on both platforms. Shake/FAB shortcuts and screenshot blocking through `secureWindow` are Android-specific; iOS settings hide those shortcuts. iOS has no equivalent to Android `FLAG_SECURE`, so `secureWindow` does not prevent iOS screenshots. iOS copies use a local-only pasteboard entry that expires after two minutes. `PERSISTENT_NO_BACKUP` uses an Application Support directory excluded from iCloud backup on iOS; memory storage is the default on both platforms.

## Usage

### 1. Initialize the library
In your `Application` class, initialize Netlogger:

```kotlin
class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Netlogger.init(
            this,
            NetloggerConfig(
                maximumLogLevel = LogLevel.ALL,
                captureBodies = true,
                captureGeneralLogs = true,
                enableLogcatOutput = true,
                allowShakeDetector = true,
                allowFloatingButton = true,
                tagTabs = listOf("AuthModule", "Database"),
                storage = NetloggerStorage.MEMORY_ONLY,
                maxBodyBytes = 256 * 1024L,
                maxLogEntries = 500,
                retentionMillis = 24 * 60 * 60 * 1000L
            )
        )
    }
}
```

### 2. Add the Interceptor
Attach the Netlogger interceptor to your `OkHttpClient`:

```kotlin
val okHttpClient = OkHttpClient.Builder()
    .addInterceptor(Netlogger.getInterceptor())
    .build()
```

### 3. Open Netlogger
There are three ways to open the Netlogger UI:
- **Shake Device**: opt in through `NetloggerConfig` or enable it in Settings.
- **Floating Button (Android)**: opt in through `NetloggerConfig` or enable it in Settings.
- **Manual Launch**:
  ```kotlin
  val intent = Intent(context, NetloggerActivity::class.java)
  startActivity(intent)
  ```

### 4. Manual Logging (Optional)
You can also manually log general messages, debug info, or errors using `LogUtil`:

```kotlin
LogUtil.debug("TAG", "Your message here")
LogUtil.error("TAG", "Something went wrong")
LogUtil.info("TAG", "Informational message")
```
These logs will appear in the "General" filter category in the log list. Tags listed in
`NetloggerConfig.tagTabs` also appear as quick-filter tabs and show matching General logs.
Long-press and drag any quick-filter tab to reorder it; the order is restored on the next launch.

## Environment isolation (required)

To ensure Netlogger has **zero code footprint** in your production APK, use a scoped dependency
such as `debugImplementation` together with Android source sets. A runtime `BuildConfig.DEBUG`
check alone does not remove the library, manifest entries, or transitive dependencies from an APK.

### 1. Define the Interface (or shared structure)
You will create two files with the **exact same package and name** in different source sets.

#### **In `src/debug/java/.../NetloggerProxy.kt`**
```kotlin
object NetloggerProxy {
    fun init(application: Application) {
        Netlogger.init(
            application,
            NetloggerConfig(
                maximumLogLevel = LogLevel.ALL,
                captureBodies = true,
                captureGeneralLogs = true,
                enableLogcatOutput = true
            )
        )
    }

    fun getInterceptor(): Interceptor {
        return Netlogger.getInterceptor()
    }
}
```

#### **In `src/release/java/.../NetloggerProxy.kt`**
```kotlin
object NetloggerProxy {
    fun init(application: Application) {
        // No-op in release
    }

    fun getInterceptor(): Interceptor? {
        return null // Return null or a dummy interceptor
    }
}
```

### 2. Update Usage
Now your main application code remains clean and doesn't need to check for `BuildConfig.DEBUG`:

```kotlin
// In Application.onCreate
NetloggerProxy.init(this)

// In your OkHttp configuration
val builder = OkHttpClient.Builder()
NetloggerProxy.getInterceptor()?.let { 
    builder.addInterceptor(it) 
}
```

## Secure defaults and configuration

Calling `Netlogger.init(application)` without a configuration is safe by default:

- API logging starts at `NONE`.
- Request and response body capture is disabled.
- Manual/general log capture is disabled.
- Logs are held in memory only.
- Logcat output is disabled by default.
- Shake and floating-button entry points are disabled.
- The Android Netlogger window blocks screenshots and non-secure displays.
- Authorization, cookies, common token/query names, credentials and configured custom fields are redacted.
- Bodies, messages, retained entries and retention duration have hard upper bounds.

When `captureBodies` is enabled, Netlogger still skips one-shot, duplex, binary, unknown-length
and oversized bodies. Add organization-specific names through `additionalRedactedHeaders`,
`additionalRedactedQueryParameters`, and `additionalRedactedBodyFields`.

`maximumLogLevel`, `allowShakeDetector`, and `allowFloatingButton` are environment-level caps.
Values restored from Settings can lower these capabilities but cannot exceed what the integrating
app approved in `NetloggerConfig`.

`enableLogcatOutput = true` is intended only for explicitly approved beta/staging source sets.
Console output uses the same bounded capture as the UI and is redacted again immediately before
calling Logcat. It does not bypass `maximumLogLevel` or `captureBodies`; credentials, cookies,
tokens, passwords, common PII and configured custom fields remain redacted.

## Android Koin integration

Netlogger owns a namespaced Koin module for its database, repositories, use cases, interceptor,
manager and ViewModels. `Netlogger.init(...)` is synchronized and safely coexists with the host:

- If the host has already called `startKoin`, Netlogger only loads its module into that container.
- If no global Koin container exists, Netlogger starts one with the application context and its
  own module.
- Netlogger does not enable Koin's Android logger and does not register or replace the host's
  unqualified `Context`/`Application` definitions.

If the host application owns additional Koin modules, start the host Koin container before calling
`Netlogger.init(...)`. Otherwise, add later host modules with `loadKoinModules(...)` instead of
calling `startKoin` a second time.

`NetloggerStorage.PERSISTENT_NO_BACKUP` is an explicit opt-in. Its database is placed under the
host application's no-backup directory and is pruned by both age and entry count. `MEMORY_ONLY`
remains recommended.

You can also customize runtime behavior in the **Settings** screen:
- **Auto-reset logs**: Automatically clear all logs from previous sessions when the app starts.
- **Enable Shake Detector**: Toggle the shake-to-open feature.
- **Shake Sensitivity**: Adjust how hard you need to shake the device.
- **API Log Level**: Select metadata, headers, or body logging within the limits allowed by `NetloggerConfig`.

## License
Copyright 2024 Koai Dev.
Licensed under the Apache License, Version 2.0.
