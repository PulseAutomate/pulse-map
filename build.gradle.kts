plugins {
    id("java-library")
    alias(libs.plugins.spotless)
    alias(libs.plugins.versions) apply false
    alias(libs.plugins.changelog)
}

allprojects {
    group = "io.pulseautomate"
    version = project.version

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

changelog {
    version = project.version.toString()
    path.set("${project.rootDir}/CHANGELOG.md")
    header = provider { "[${project.version}]" }
    keepUnreleasedSection = true
    unreleasedTerm = "[Unreleased]"
}