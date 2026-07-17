plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
    id("com.google.devtools.ksp")
    id("maven-publish")
}

android {
    namespace = "com.netlogger.lib"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        minSdk = 24

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    publishing {
        singleVariant("release") {
            withSourcesJar()
            withJavadocJar()
        }
    }
}
val libVersion = "1.4.1"
afterEvaluate {
    publishing {
        publications {
            register<MavenPublication>("release") {
                groupId = "com.koai"
                artifactId = "netlogger-compose"
                version = libVersion

                afterEvaluate {
                    from(components["release"])
                }
            }
        }
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.material)
    implementation(libs.koin.android)
    testImplementation(libs.junit)
    testImplementation(libs.okhttp.mockwebserver)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    // Room
    implementation(libs.androidx.room.runtime)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.room.ktx)

    // ViewModel & Coroutines
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.kotlinx.coroutines.android)

    // Gson
    implementation(libs.gson)

    // OkHttp
    api(libs.okhttp)

    // Icons
    implementation(libs.androidx.compose.material.icons.extended)
}

tasks.register("localBuild") {
    dependsOn("assembleRelease")
}

tasks.register("createReleaseTag") {
    doLast {
        val tagName = "v$libVersion"
        try {
            logger.lifecycle("Creating tag: $tagName")

            providers
                .exec {
                    commandLine("git", "tag", "-a", tagName, "-m", "Release tag $tagName")
                }.result
                .get()

            providers
                .exec {
                    commandLine("git", "push", "origin", tagName)
                }.result
                .get()

            logger.lifecycle("Successfully created and pushed tag: $tagName")
        } catch (e: Exception) {
            throw GradleException("Failed to create/push tag $tagName", e)
        }
    }
}
