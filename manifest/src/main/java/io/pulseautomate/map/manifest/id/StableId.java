package io.pulseautomate.map.manifest.id;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Objects;

public final class StableId {
    private StableId() {}

    public static String derive(String seed) {
        Objects.requireNonNull(seed, "seed");
        var bytes = sha256(seed);
        var hex = HexFormat.of().formatHex(bytes);
        return "stable: " + hex.substring(0, 12);
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
