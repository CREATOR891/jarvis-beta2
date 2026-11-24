plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.jarvis.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.jarvis.app"
        minSdk = 26
        targetSdk = 34
        versionCode = 2
        versionName = "0.2-beta"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}
