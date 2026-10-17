plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.boot.game"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.boot.game"
        minSdk = 26
        targetSdk = 36
        versionCode = 54
        versionName = "3.20.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            // Signed with the debug key so `assembleRelease` produces an installable APK out of the box.
            // Swap in your own signing config before publishing to Google Play.
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }

    testOptions {
        // Robolectric runs the Compose UI on the JVM, so UI tests need no emulator.
        unitTests.isIncludeAndroidResources = true
    }
}

// -PfontScale=1.3 and -Pnight redraw every screenshot with big text and/or in dark mode (see DisplayRule).
tasks.withType<Test>().configureEach {
    providers.gradleProperty("fontScale").orNull?.let { systemProperty("boot.fontScale", it) }
    if (providers.gradleProperty("night").isPresent) systemProperty("boot.night", "true")
    // -Paudio=dir writes every tune and sound effect there as a WAV file to listen to (see AudioTest).
    providers.gradleProperty("audio").orNull?.let { systemProperty("boot.audio", it) }
}

dependencies {
    implementation(project(":core"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.graphics.shapes)
    implementation(libs.compose.material.icons)
    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.test.manifest)

    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(platform(libs.compose.bom))
    testImplementation(libs.compose.ui.test.junit4)
}
