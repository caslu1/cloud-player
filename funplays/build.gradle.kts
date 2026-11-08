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
        versionCode = 2
        versionName = "1.1"
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

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}
