plugins {
    id("com.android.application") version "8.4.2" apply false
    id("org.jetbrains.kotlin.android") version "1.9.24" apply false
    id("com.google.dagger.hilt.android") version "2.51.1" apply false
    id("com.diffplug.spotless") version "6.25.0"
}

spotless {
    kotlin {
        target("**/*.kt")
        ktlint("1.2.1").editorConfigOverride(
            mapOf(
                "max_line_length" to "140",
                "ktlint_standard_discouraged_comment" to "disabled",
                "ktlint_standard_no-wildcard-imports" to "disabled"
            )
        )
    }
    kotlinGradle {
        target("**/*.kts")
        ktlint("1.2.1")
    }
}
