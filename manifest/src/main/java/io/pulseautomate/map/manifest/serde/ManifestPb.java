package io.pulseautomate.map.manifest.serde;

import io.pulseautomate.map.manifest.gen.model.Manifest;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ManifestPb {
  private ManifestPb() {}

  public static void write(Path path, Manifest manifest) throws IOException {
    Files.createDirectories(path.getParent());
    try (OutputStream os = Files.newOutputStream(path)) {
      manifest.writeTo(os);
    }
  }

  public static Manifest read(Path path) throws IOException {
    try (InputStream is = Files.newInputStream(path)) {
      return Manifest.parseFrom(is);
    }
  }
}
