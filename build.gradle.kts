import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.testing.Test
import org.gradle.testing.jacoco.tasks.JacocoReport
import org.gradle.testing.jacoco.tasks.JacocoCoverageVerification
import java.math.BigDecimal

plugins {
    id("java-library")
    alias(libs.plugins.spotless)
    alias(libs.plugins.versions) apply false
    alias(libs.plugins.changelog)

    // Coverage & SBOM
    jacoco
    alias(libs.plugins.cyclonedx) apply false
}

allprojects {
    group = "io.pulseautomate"
    version = project.version
    repositories { mavenCentral() }
}

subprojects {
    apply(plugin = "java-library")
    apply(plugin = "jacoco")
    apply(plugin = "org.cyclonedx.bom")

    java { toolchain { languageVersion.set(JavaLanguageVersion.of(21)) } }
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

/* =========================
   Coverage: merged root gate
   ========================= */

// Allow override via gradle.properties (e.g., coverage.minimum=0.70)
val coverageMinimum: BigDecimal =
    (findProperty("coverage.minimum") as String?)?.toBigDecimal() ?: BigDecimal("0.80")

// Modules to EXCLUDE from coverage totals (generated/APIs/test helpers)
val coverageExcludedModules = setOf(
    "gen-proto",
    "gen-jvm",
    "gen-attrs",
    "grpc-api",
    "test-fixtures",
    "integration-tests"
)

// Projects that count toward coverage
val coverageIncludedProjects = subprojects.filter { it.name !in coverageExcludedModules }

// Exclusions for generated or trivial classes (pattern-based)
val coverageClassExcludes = listOf(
    // generated code
    "**/gen/**",
    "**/generated/**",
    "**/build/generated/**",
    "**/*Protos*",
    "**/*Grpc*",
    "**/*GrpcKt*",
    "**/*Proto*",
    // launchers / CLI wrappers
    "**/Main*"
)

// Collect only the test tasks from included modules (avoid wiring excluded ones)
val includedTestTasks = coverageIncludedProjects.mapNotNull { it.tasks.findByName("test") }

// Root merged report (no separate JacocoMerge task needed)
tasks.register<JacocoReport>("jacocoRootReport") {
    // Ensure Spotless checks complete first
    dependsOn("spotlessCheck")

    // Ensure tests in INCLUDED modules ran so exec files exist
    dependsOn(includedTestTasks)

    // Be explicit about ordering vs potential 'spotlessApply'
    mustRunAfter("spotlessApply", "spotlessJava", "spotlessKotlinGradle")

    // Gather all JaCoCo exec files from INCLUDED modules only
    executionData(fileTree(rootDir) {
        include(coverageIncludedProjects.map { "${it.projectDir}/build/jacoco/test.exec" })
    })

    // Collect sources/classes from INCLUDED modules
    val mains = coverageIncludedProjects.mapNotNull {
        it.extensions.findByType(SourceSetContainer::class.java)?.findByName("main")
    }

    sourceDirectories.setFrom(mains.flatMap { it.allSource.srcDirs })

    val classDirs = files(mains.map { it.output }).asFileTree.matching {
        exclude(coverageClassExcludes)
    }
    classDirectories.setFrom(classDirs)

    reports {
        xml.required.set(true)
        html.required.set(true)
    }
}

tasks.register<JacocoCoverageVerification>("jacocoRootCoverageVerification") {
    dependsOn("spotlessCheck")
    dependsOn(includedTestTasks)
    dependsOn("jacocoRootReport")

    mustRunAfter("spotlessApply", "spotlessJava", "spotlessKotlinGradle")

    // Use execs only from INCLUDED modules
    executionData(fileTree(rootDir) {
        include(coverageIncludedProjects.map { "${it.projectDir}/build/jacoco/test.exec" })
    })

    val mains = coverageIncludedProjects.mapNotNull {
        it.extensions.findByType(SourceSetContainer::class.java)?.findByName("main")
    }
    classDirectories.setFrom(files(mains.map { it.output }).asFileTree.matching {
        exclude(coverageClassExcludes)
    })
    sourceDirectories.setFrom(mains.flatMap { it.allSource.srcDirs })

    violationRules {
        rule {
            element = "BUNDLE"
            limit {
                counter = "LINE"
                value = "COVEREDRATIO"
                minimum = coverageMinimum
            }
        }
    }
}

// Enforce the gate on the standard lifecycle
tasks.named("check") { dependsOn("jacocoRootCoverageVerification") }

/* ==================================
   SBOM: CycloneDX aggregator @ root
   ================================== */

// Each subproject gets 'cyclonedxBom' from the plugin.
// This root task runs them all so CI can simply call 'cyclonedxBomAll'.
tasks.register("cyclonedxBomAll") {
    coverageIncludedProjects.forEach { p ->
        p.plugins.withId("org.cyclonedx.bom") {
            dependsOn(p.tasks.named("cyclonedxBom"))
        }
    }
}
