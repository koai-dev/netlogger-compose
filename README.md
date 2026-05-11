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

### Step 2. Add the dependency

Add the following to your app-level `build.gradle` or `build.gradle.kts`:

**Groovy (build.gradle):**
```gradle
dependencies {
    implementation 'com.github.koai-dev:netlogger-compose:1.0.0'
}
```

**Kotlin DSL (build.gradle.kts):**
```kotlin
dependencies {
    implementation("com.github.koai-dev:netlogger-compose:1.0.0")
}
```

## Usage

### 1. Initialize the library
In your `Application` class, initialize Netlogger:

```kotlin
class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Netlogger.init(this)
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
There are two ways to open the Netlogger UI:
- **Shake Device**: Shake your phone to trigger the UI (can be configured in Settings).
- **Floating Button**: A floating button automatically appears on resumed activities (can be disabled).
- **Manual Launch**:
  ```kotlin
  val intent = Intent(context, NetloggerActivity::class.java)
  startActivity(intent)
  ```

## Configuration
You can customize Netlogger behavior in the **Settings** screen within the app:
- **Auto-reset logs**: Automatically clear all logs from previous sessions when the app starts.
- **Enable Shake Detector**: Toggle the shake-to-open feature.
- **Shake Sensitivity**: Adjust how hard you need to shake the device.

## License
Copyright 2024 Koai Dev.
Licensed under the Apache License, Version 2.0.
