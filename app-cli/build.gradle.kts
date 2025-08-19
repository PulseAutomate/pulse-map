plugins {
    application
    alias(libs.plugins.versions)
    alias(libs.plugins.graalvm.native)
}

dependencies {
    implementation(project(":ha-client"))
    implementation(project(":manifest"))

    implementation(libs.picocli)
    implementation(libs.bundles.jackson)

    annotationProcessor(libs.picocli.codegen)

    testImplementation(libs.junit.jupiter)
    testImplementation(libs.assertj.core)
}

application {
    mainClass.set("io.pulseautomate.map.cli.MapCli")
}

graalvmNative {
    binaries {
        named("main") {
            imageName.set("pulse-map")

            configurationFileDirectories.from(
                file("${layout.buildDirectory}/native/agent-output/main"),
                file("${layout.buildDirectory}/native/agent-output/test")
            )

            buildArgs.addAll(listOf(
                "--no-fallback",
                "--install-exit-handlers"
            ))
        }
    }
}