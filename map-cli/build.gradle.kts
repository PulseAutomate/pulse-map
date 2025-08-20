plugins {
    application
    alias(libs.plugins.versions)

    id("com.github.johnrengelman.shadow") version "8.1.1"
    id("org.graalvm.buildtools.native") version "0.10.2"
}

repositories {
    mavenCentral()
}

application {
    // your CLI entrypoint
    mainClass.set("io.pulseautomate.map.cli.MapCli")
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

tasks.shadowJar {
    archiveBaseName = "pulse-map"
    archiveClassifier = ""
}

tasks.register<Copy>("bundleDist") {
    dependsOn(":map-cli:installDist")
    from(layout.buildDirectory.dir("install/map-cli"))
    into(layout.buildDirectory.dir("bundle/map-cli-${project.version}"))
}

val toolchains = extensions.getByType(JavaToolchainService::class.java)

// keep 'run' clean (no agent)
tasks.named<JavaExec>("run").configure {
    jvmArgumentProviders.removeIf { p ->
        val n = p.javaClass.name
        n.contains("Graal", ignoreCase = true) || n.contains("NativeImageAgent", ignoreCase = true)
    }
}

graalvmNative {
    toolchainDetection = true

    binaries {
        // the binary that :map-cli:nativeCompile produces
        named("main") {
            imageName = "pulse-map"

            // FORCE an executable (never a shared library)
            sharedLibrary.set(false)

            // toolchain
            val graal = toolchains.launcherFor {
                languageVersion = JavaLanguageVersion.of(21)
                vendor = JvmVendorSpec.GRAAL_VM
            }
            val anyJdk21 = toolchains.launcherFor {
                languageVersion = JavaLanguageVersion.of(21)
            }
            javaLauncher = graal.orElse(anyJdk21)

            // use agent outputs if present (reflection/resources)
            configurationFileDirectories.from(
                file("$buildDir/native/agent-output/main"),
                file("$buildDir/native/agent-output/test")
            )

            // CLI-friendly defaults
            buildArgs.set(
                listOf(
                    "--no-fallback",
                    "--install-exit-handlers"
                )
            )

            // Defensive: strip any accidental shared-library flags coming from elsewhere
            // (older samples, parent convention, etc.)
            buildArgs.set(buildArgs.get().filterNot {
                it.equals("--shared", true) ||
                        it.startsWith("-H:Kind=SHARED_LIBRARY", true) ||
                        it.startsWith("-H:Kind=", true) // nukes old experimental flag if it sneaks in
            })
        }
    }
}

// Agent recording helper (unchanged)
val agentOutMain = layout.buildDirectory.dir("native/agent-output/main")
tasks.register<JavaExec>("recordAgentDemo") {
    group = "native"
    description = "Run CLI with Graal agent (demo) to record reflection config"

    mainClass = "io.pulseautomate.map.cli.MapCli"
    classpath = sourceSets["main"].runtimeClasspath

    javaLauncher = toolchains.launcherFor {
        languageVersion = JavaLanguageVersion.of(21)
        vendor = JvmVendorSpec.GRAAL_VM
    }

    jvmArgs("-agentlib:native-image-agent=config-output-dir=${agentOutMain.get().asFile.absolutePath}")
    args("discover", "--out", "../temp/pulse-map", "--demo")
}

tasks.register<JavaExec>("recordAgentValidate") {
    group = "native"
    description = "Run CLI validate with Graal agent to record Jackson reflection config"

    mainClass = "io.pulseautomate.map.cli.MapCli"
    classpath = sourceSets["main"].runtimeClasspath

    // Use GraalVM JDK to load the agent
    javaLauncher = toolchains.launcherFor {
        languageVersion = JavaLanguageVersion.of(21)
        vendor = JvmVendorSpec.GRAAL_VM
    }

    // Point agent to the same dir you already include in graalvmNative.configurationFileDirectories
    jvmArgs("-agentlib:native-image-agent=config-output-dir=${agentOutMain.get().asFile.absolutePath}")

    // Validate the demo output recorded by recordAgentDemo
    args("validate", "--dir", "../temp/pulse-map")
}

