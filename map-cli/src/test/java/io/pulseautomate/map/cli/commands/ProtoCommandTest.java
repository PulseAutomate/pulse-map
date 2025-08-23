package io.pulseautomate.map.cli.commands;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

class ProtoCommandTest {

  @TempDir Path tmp;

  private Path inputDir;
  private Path outDir;

  @BeforeEach
  void setUp() throws Exception {
    inputDir = tmp.resolve("input");
    outDir = tmp.resolve("output");
    Files.createDirectories(inputDir);
    Files.createDirectories(outDir);

    // Copy test fixture files into the temporary input directory
    try (InputStream manifestIn =
            getClass().getClassLoader().getResourceAsStream("fixtures/manifest.pb");
        InputStream lockIn =
            getClass().getClassLoader().getResourceAsStream("fixtures/map.lock.pb")) {
      assertThat(manifestIn).isNotNull();
      assertThat(lockIn).isNotNull();
      Files.copy(manifestIn, inputDir.resolve("manifest.pb"));
      Files.copy(lockIn, inputDir.resolve("map.lock.pb"));
    }
  }

  @Test
  void runsSuccessfully_and_generates_proto_files() throws Exception {
    // 1. Arrange: Create an instance of the command
    var command = new ProtoCommand();
    var cmd = new CommandLine(command);

    // 2. Act: Execute the command with arguments
    int exitCode = cmd.execute("--dir", inputDir.toString(), "--out", outDir.toString());

    // 3. Assert: Check the results
    assertThat(exitCode).isEqualTo(0);

    var generatedDir = outDir.resolve("pulse/v1");
    assertThat(generatedDir).isDirectory();
    assertThat(generatedDir.resolve("attributes.proto")).exists();
    assertThat(generatedDir.resolve("entities.proto")).exists();
    assertThat(generatedDir.resolve("climate.proto")).exists(); // Based on demo data

    // Verify content of one file to ensure it's not empty
    String climateProtoContent = Files.readString(generatedDir.resolve("climate.proto"));
    assertThat(climateProtoContent).contains("enum ClimateHvacMode");
  }

  @Test
  void fails_when_input_files_are_missing() {
    var command = new ProtoCommand();
    var cmd = new CommandLine(command);

    // Point to an empty directory
    var emptyDir = tmp.resolve("empty");

    int exitCode = cmd.execute("--dir", emptyDir.toString(), "--out", outDir.toString());

    // Should fail with a non-zero exit code
    assertThat(exitCode).isNotEqualTo(0);
  }
}
