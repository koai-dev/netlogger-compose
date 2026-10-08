import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.gradle.plugins.signing.Sign
import org.gradle.plugins.signing.SigningExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.XCFramework

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kmp.library)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    id("com.google.devtools.ksp")
    alias(libs.plugins.maven.publish)
}

group = providers.gradleProperty("GROUP").get()
version = providers.gradleProperty("VERSION_NAME").get()

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
    val xcf = XCFramework("Netlogger")
    listOf(iosArm64(), iosSimulatorArm64()).forEach { target ->
        target.binaries.framework {
            baseName = "Netlogger"
            isStatic = true
            binaryOption(
                "bundleId",
                "io.github.koai-dev.netlogger"
            )

            xcf.add(this)
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

mavenPublishing {
    publishToMavenCentral(automaticRelease = false)
    signAllPublications()
    pom {
        name.set("Netlogger Compose")
        description.set("A Kotlin Multiplatform network logger with shared Compose UI for Android and iOS.")
        url.set("https://github.com/koai-dev/netlogger-compose")
        licenses {
            license {
                name.set("The Apache License, Version 2.0")
                url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                distribution.set("repo")
            }
        }
        developers {
            developer {
                id.set("koai-dev")
                name.set("koai-dev")
                url.set("https://github.com/koai-dev")
            }
        }
        scm {
            url.set("https://github.com/koai-dev/netlogger-compose")
            connection.set("scm:git:https://github.com/koai-dev/netlogger-compose.git")
            developerConnection.set("scm:git:ssh://git@github.com/koai-dev/netlogger-compose.git")
        }
    }
}

val signingConfiguration = extensions.getByType<SigningExtension>()
if (providers.gradleProperty("signing.useGpgCmd").map { it.toBoolean() }.orElse(false).get()) {
    signingConfiguration.useGpgCmd()
}

val verifyNetloggerSigningCredentials = tasks.register("verifyNetloggerSigningCredentials") {
    group = "publishing"
    description = "Checks that a release signing identity is configured without uploading artifacts."
    doLast {
        if (!project.version.toString().endsWith("-SNAPSHOT") && signingConfiguration.signatory == null) {
            throw GradleException(
                "No GPG signing identity is configured. Central tokens do not configure signing. " +
                    "For a local GPG key, set signing.useGpgCmd=true and signing.gnupg.keyName " +
                    "in ~/.gradle/gradle.properties; alternatively configure signingInMemoryKey " +
                    "or signing.keyId/password/secretKeyRingFile. See docs/maven-central.md."
            )
        }
    }
}
tasks.withType<Sign>().configureEach { dependsOn(verifyNetloggerSigningCredentials) }

publishing.repositories.maven {
    name = "localStaging"
    url = uri(rootProject.layout.buildDirectory.dir("maven-staging"))
}
