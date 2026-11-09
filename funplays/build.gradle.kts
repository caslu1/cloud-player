plugins {
    alias(libs.plugins.android.application)
}

// Small companion app: adds an M3U playlist to a device on funplays.app (MAC + device key).
android {
    namespace = "com.cloudplayer.funplays"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.cloudplayer.funplays"
        minSdk = 21
        targetSdk = 37
        versionCode = 7
        versionName = "1.6"
    }

    signingConfigs {
        // Same fixed debug key as the main app, so updates install over the old version.
        getByName("debug") {
            storeFile = rootProject.file("app/cloudplayer-debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    // Starts MainActivity on a simulated Android in the unit tests, to catch crashes on launch.
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.14.1")
    testImplementation("androidx.test:core:1.6.1")
}

// Show the full error of a failing test in the CI log.
tasks.withType<Test>().configureEach {
    // Robolectric cannot read Java 25 class files yet; run the simulated Android on Java 21.
    javaLauncher.set(javaToolchains.launcherFor { languageVersion.set(JavaLanguageVersion.of(21)) })
    testLogging {
        events("passed", "failed")
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
}
