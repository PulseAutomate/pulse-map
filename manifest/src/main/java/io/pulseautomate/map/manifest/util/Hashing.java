package io.pulseautomate.map.manifest.util;

import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

public final class Hashing {
  private Hashing() {}

  public static String sha256Hex(String s) {
    var md = HashAlgo.SHA256.newDigest();
    return HexFormat.of().formatHex(md.digest(s.getBytes(StandardCharsets.UTF_8)));
  }

  public static String sha256Hex(byte[] bytes) {
    var md = HashAlgo.SHA256.newDigest();
    return HexFormat.of().formatHex(md.digest(bytes));
  }
}
