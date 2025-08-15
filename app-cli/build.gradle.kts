plugins {
    application
    alias(libs.plugins.versions)
    alias(libs.plugins.graalvm.native)
}

dependencies {
    implementation(project(":manifest"))
    implementation(libs.picocli)
    annotationProcessor(libs.picocli.codegen)
    testImplementation(libs.junit.jupiter)
}

application {
    mainClass.set("io.pulseautomate.map.cli.Main")
}

graalvmNative {
    binaries {
        named("main") {
            imageName.set("pulse-map")
            buildArgs.add("-H:+ReportExceptionStackTraces")
        }
    }
}