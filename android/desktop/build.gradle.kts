import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.compose)
    id("org.jetbrains.compose")
}

group = "com.boot"
version = "0.1.0"

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(project(":core"))
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
}

compose.desktop {
    application {
        mainClass = "com.boot.desktop.MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Msi, TargetFormat.Exe)
            packageName = "Boot Game Tycoon"
            packageVersion = "0.1.0"
            description = "Boot Game Tycoon — manage your own game studio."
            vendor = "Boot"
            windows {
                menuGroup = "Boot"
                perUserInstall = true
                dirChooser = true
            }
        }
    }
}
