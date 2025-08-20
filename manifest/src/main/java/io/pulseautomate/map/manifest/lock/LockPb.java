package io.pulseautomate.map.manifest.lock;

import io.pulseautomate.map.manifest.gen.model.LockFile;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class LockPb {
  private LockPb() {}

  public static void write(Path path, LockFile lockFile) throws IOException {
    Files.createDirectories(path.getParent());
    try (var os = Files.newOutputStream(path)) {
      lockFile.writeTo(os);
    }
  }

  public static LockFile read(Path path) throws IOException {
    try (var is = Files.newInputStream(path)) {
      return LockFile.parseFrom(is);
    }
  }
}
