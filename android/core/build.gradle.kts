import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// The whole simulation as plain Kotlin, so it builds and tests on the JVM without an emulator.
plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
}

dependencies {
    api(libs.kotlinx.serialization.json)
    testImplementation(kotlin("test"))
    testImplementation(libs.junit)
}

// ./gradlew :core:balance -Pruns=20 [-Pstart=1985]
tasks.register<JavaExec>("balance") {
    group = "verification"
    description = "Autopilot many games per starting lane and print outcome stats."
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("com.boot.core.BalanceKt")
    args((project.findProperty("runs") ?: "20").toString(), (project.findProperty("start") ?: "").toString())
}
