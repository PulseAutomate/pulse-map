package io.pulseautomate.map.manifest.id;

import static io.pulseautomate.map.manifest.util.Constants.STABLE_ID_HEX_LENGTH;
import static io.pulseautomate.map.manifest.util.Constants.STABLE_PREFIX;

import io.pulseautomate.map.manifest.util.Hashing;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Objects;

public final class StableId {
  private StableId() {}

  public static String derive(String seed) {
    Objects.requireNonNull(seed, "seed");
    var hex = Hashing.sha256Hex(seed);
    return STABLE_PREFIX + hex.substring(0, STABLE_ID_HEX_LENGTH);
  }

  private static byte[] sha256(String s) {
    try {
      var md = MessageDigest.getInstance("SHA-256");
      return md.digest(s.getBytes(StandardCharsets.UTF_8));
    } catch (Exception e) {
      throw new RuntimeException("SHA-256 not available", e);
    }
  }
}
