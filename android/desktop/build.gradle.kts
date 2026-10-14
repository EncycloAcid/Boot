import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

/**
 * Windows (and other desktop) port of Boot.
 *
 * Reuses the JVM simulation in `:core`. UI is Compose Multiplatform Desktop with a
 * fullscreen sprite shell and floating nav — not a phone Material layout.
 *
 * Run (any OS with a display):  ./gradlew :desktop:run
 * Package Windows .exe/.msi:    ./gradlew :desktop:packageExe :desktop:packageMsi
 *   (jpackage requires building the Windows targets on Windows with JDK 17+)
 * Package current OS:           ./gradlew :desktop:packageDistributionForCurrentOS
 */
plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.compose.multiplatform)
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
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
            targetFormats(TargetFormat.Exe, TargetFormat.Msi, TargetFormat.Deb, TargetFormat.Dmg)
            packageName = "Boot"
            packageVersion = "1.0.0"
            description = "Boot — company management tycoon (Windows sprite port)"
            copyright = "Boot"
            vendor = "Boot"
            windows {
                menuGroup = "Boot"
                // Stable upgrade id so later MSI installs replace this build.
                upgradeUuid = "A7C3E91F-2B4D-4E8A-9F01-6D5C8B7A3E21"
                shortcut = true
                dirChooser = true
            }
        }
    }
}
