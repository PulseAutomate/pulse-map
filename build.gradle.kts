plugins {
    id("java-library")
    alias(libs.plugins.spotless)
    alias(libs.plugins.versions) apply false
}

allprojects {
    group = "io.pulseautomate"
    version = "0.1.0-SNAPSHOT"

    repositories { mavenCentral() }
}

subprojects {
    apply(plugin = "java-library")

    java {
        toolchain { languageVersion.set(JavaLanguageVersion.of(21)) }
    }

    tasks.withType<Test> { useJUnitPlatform() }
}

/** Formatting (keeps diffs clean) */
spotless {
    java { target("**/*.java"); googleJavaFormat() }
    kotlinGradle { target("**/*.gradle.kts") }
    format("misc") {
        target("**/*.md", "**/.gitignore")
        trimTrailingWhitespace()
        endWithNewline()
    }
}
