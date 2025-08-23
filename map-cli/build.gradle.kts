plugins {
    application
    alias(libs.plugins.versions)
    alias(libs.plugins.shadow)
    alias(libs.plugins.graalvm.native)
}

repositories {
    mavenCentral()
}

application {
    // Defines the main entry point for the CLI application.
    mainClass.set("io.pulseautomate.map.cli.MapCli")
}

dependencies {
    // Core module dependencies
    implementation(project(":ha-client"))
    implementation(project(":manifest"))
    implementation(project(":gen-proto"))

    // CLI argument parsing
    implementation(libs.picocli)
    annotationProcessor(libs.picocli.codegen)

    // Protobuf utilities for the --json flag
    implementation(libs.protobuf.java.util)

    // Testing dependencies
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.assertj.core)
}

// Configures the Shadow plugin to create a single, executable "fat JAR"
// that includes all necessary dependencies.
tasks.shadowJar {
    archiveBaseName = "pulse-map"
    archiveClassifier = ""
}

// Configures the GraalVM native image build.
graalvmNative {
    // Automatically detect the GraalVM toolchain.
    toolchainDetection = true

    binaries {
        // Defines the 'main' native binary artifact.
        named("main") {
            imageName = "pulse-map"
            mainClass.set(application.mainClass)

            // Force an executable, never a shared library.
            sharedLibrary.set(false)

            // CLI-friendly defaults for a better user experience.
            buildArgs.set(
                listOf(
                    "--no-fallback",
                    "--install-exit-handlers",
                    "-H:+ReportExceptionStackTraces"
                )
            )

            resources.autodetect()
            resources.includedPatterns.add("io/pulseautomate/map/gen/proto/templates/.*\\.template")
        }
    }
}