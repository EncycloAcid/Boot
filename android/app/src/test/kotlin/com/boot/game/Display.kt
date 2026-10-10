package com.boot.game

import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement
import org.robolectric.RuntimeEnvironment
import java.io.File

/**
 * How the screenshot tests draw the app. By default, as a phone at standard text size in light
 * mode. `./gradlew :app:testDebugUnitTest -PfontScale=1.3 -Pnight` draws every screen with big
 * text and/or in dark mode instead, into its own folder, to catch layouts that only break there.
 * Runs ahead of the Compose rule, before the test activity starts.
 */
class DisplayRule : TestRule {
    override fun apply(base: Statement, description: Description) = object : Statement() {
        override fun evaluate() {
            if (night) RuntimeEnvironment.setQualifiers("+night")
            if (fontScale != 1f) RuntimeEnvironment.setFontScale(fontScale)
            base.evaluate()
        }
    }

    companion object {
        val fontScale = System.getProperty("boot.fontScale")?.toFloatOrNull() ?: 1f
        val night = System.getProperty("boot.night") == "true"

        /** Where screenshots go: build/screenshots, or e.g. build/screenshots-large-night. */
        val screenshots: File
            get() = File(
                "build/" + listOfNotNull("screenshots", "large".takeIf { fontScale != 1f }, "night".takeIf { night }).joinToString("-"),
            ).apply { mkdirs() }
    }
}
