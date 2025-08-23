plugins {
    alias(libs.plugins.protobuf)
}

dependencies {
    implementation(project(":manifest"))

    implementation(libs.protobuf.java)

    testImplementation(libs.junit.jupiter)
    testImplementation(libs.assertj.core)
}

protobuf {
    protoc {
        artifact = libs.protoc.get().toString()
    }
}