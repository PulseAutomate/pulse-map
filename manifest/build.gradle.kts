dependencies {
    implementation(project(":ha-client"))

    implementation(libs.bundles.jackson)
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.assertj.core)
}