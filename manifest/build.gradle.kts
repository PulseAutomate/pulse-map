plugins {
    id("com.google.protobuf") version "0.9.4"
}

dependencies {
    implementation(project(":ha-client"))

    implementation("com.google.protobuf:protobuf-java:4.28.2")

    testImplementation(libs.junit.jupiter)
    testImplementation(libs.assertj.core)
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:4.28.2"
    }
}