# Netlogger Compose

A powerful and beautiful network logging library for Android, built with Jetpack Compose and Clean Architecture.

## Features
- **Real-time Logging**: Intercept and view all network requests and responses.
- **Beautiful JSON Viewer**: Expandable/collapsible JSON tree with syntax highlighting.
- **Global Search**: Search for any text in log details (URLs, Headers, Bodies) with navigation arrows.
- **Advanced Filtering**: Filter logs by Method (GET, POST, etc.) and Status Code (2xx, 4xx, etc.).
- **Shake to Open**: Instantly open the log list by shaking your device.
- **Floating Button**: Optional floating shortcut for quick access.
- **cURL Export**: Easily copy any request as a cURL command.
- **Auto-reset**: Configurable option to clear old logs on app startup.

## Installation
[![](https://jitpack.io/v/koai-dev/netlogger-compose.svg)](https://jitpack.io/#koai-dev/netlogger-compose)
### Step 1. Add the JitPack repository to your build file

Add it in your root `settings.gradle` or `settings.gradle.kts` at the end of repositories:

**Groovy (settings.gradle):**
```gradle
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
        maven { url 'https://jitpack.io' }
    }
}
```

**Kotlin DSL (settings.gradle.kts):**
```kotlin
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}
```

### Step 2. Add the dependency only to an approved environment

Add the following to your app-level `build.gradle` or `build.gradle.kts`:

**Groovy (build.gradle):**
```gradle
dependencies {
    debugImplementation 'com.github.koai-dev:netlogger-compose:<version>'
}
```

**Kotlin DSL (build.gradle.kts):**
```kotlin
dependencies {
    debugImplementation("com.github.koai-dev:netlogger-compose:<version>")
}
```

For a custom internal or QA flavor, use the matching configuration instead, for example
`internalImplementation` or `qaImplementation`. Do not use the unscoped `implementation`
configuration unless the target environment has explicitly approved network logging.

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
- **Floating Button**: opt in through `NetloggerConfig` or enable it in Settings.
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
- The Netlogger window blocks screenshots and non-secure displays.
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

## Koin integration

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
