plugins {
    application
    alias(libs.plugins.versions)
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
