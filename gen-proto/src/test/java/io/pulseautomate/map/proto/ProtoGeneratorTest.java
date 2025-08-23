package io.pulseautomate.map.proto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import io.pulseautomate.map.manifest.gen.model.LockFile;
import io.pulseautomate.map.manifest.gen.model.Manifest;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ProtoGeneratorTest {
  @TempDir Path tmp;

  private Manifest manifest;
  private LockFile lockFile;

  @BeforeEach
  void setup() throws Exception {
    try (var manifestInput =
            getClass().getClassLoader().getResourceAsStream("fixtures/manifest.pb");
        var lockFileInput =
            getClass().getClassLoader().getResourceAsStream("fixtures/map.lock.pb")) {
      assertThat(manifestInput).isNotNull();
      assertThat(lockFileInput).isNotNull();

      manifest = Manifest.parseFrom(manifestInput);
      lockFile = LockFile.parseFrom(lockFileInput);
    }
  }

  @Test
  void generates_protobuf_files_match_golden_snapshot() throws Exception {
    var generator = new ProtoGenerator(manifest, lockFile);
    generator.generate(tmp);

    var pulseDir = tmp.resolve("pulse/v1");
    assertThat(pulseDir).isDirectory();

    var goldenDir = Path.of("src", "test", "resources", "fixtures", "golden");
    assertThat(goldenDir).isDirectory();

    try (Stream<Path> generatedFiles = Files.list(pulseDir)) {
      generatedFiles.forEach(
          generatedFile -> {
            try {
              Path goldenFile = goldenDir.resolve(generatedFile.getFileName());
              assertThat(goldenFile).exists();
              assertThat(Files.readString(generatedFile)).isEqualTo(Files.readString(goldenFile));
            } catch (IOException e) {
              throw new RuntimeException(e);
            }
          });
    }
  }
}
