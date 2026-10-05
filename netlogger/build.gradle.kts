import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kmp.library)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    id("com.google.devtools.ksp")
    id("maven-publish")
}

group = "com.koai"
version = "1.5.0"

kotlin {
    compilerOptions.freeCompilerArgs.add("-Xexpect-actual-classes")
    android {
        namespace = "com.netlogger.lib"
        compileSdk = 36
        minSdk = 24
        androidResources.enable = true
        compilerOptions.jvmTarget.set(JvmTarget.JVM_21)
        withHostTest {}
        withDeviceTest { instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner" }
    }
    listOf(iosArm64(), iosSimulatorArm64()).forEach { target ->
        target.binaries.framework {
            baseName = "Netlogger"
            isStatic = true
        }
    }
    sourceSets {
        commonMain.dependencies {
            implementation(libs.compose.multiplatform.runtime)
            implementation(libs.compose.multiplatform.foundation)
            implementation(libs.compose.multiplatform.material3)
            implementation(libs.compose.multiplatform.ui)
            implementation(libs.compose.multiplatform.resources)
            implementation(libs.compose.multiplatform.preview)
            implementation(libs.compose.multiplatform.icons)
            implementation(libs.multiplatform.lifecycle.viewmodel)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.androidx.room.runtime)
            implementation(libs.androidx.sqlite.bundled)
            api(libs.ktor.client.core)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.ktor.client.mock)
        }
        androidMain.dependencies {
            implementation(libs.androidx.core.ktx)
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.lifecycle.runtime.ktx)
            implementation(libs.material)
            implementation(libs.koin.android)
            implementation(libs.kotlinx.coroutines.android)
            api(libs.okhttp)
        }
        getByName("androidHostTest").dependencies {
            implementation(libs.junit)
            implementation(libs.gson)
            implementation(libs.okhttp.mockwebserver)
        }
        getByName("androidDeviceTest").dependencies {
            implementation(libs.androidx.junit)
            implementation(libs.androidx.espresso.core)
        }
        iosMain.dependencies { implementation(libs.ktor.client.darwin) }
    }
}

compose.resources { packageOfResClass = "com.netlogger.lib.resources" }

ksp { arg("room.schemaLocation", "$projectDir/schemas") }
dependencies {
    add("kspAndroid", libs.androidx.room.compiler)
    add("kspIosArm64", libs.androidx.room.compiler)
    add("kspIosSimulatorArm64", libs.androidx.room.compiler)
}

tasks.register("localBuild") { dependsOn("assemble", "linkDebugFrameworkIosSimulatorArm64") }
