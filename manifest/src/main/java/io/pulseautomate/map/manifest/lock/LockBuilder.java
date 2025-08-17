package io.pulseautomate.map.manifest.lock;

import io.pulseautomate.map.manifest.id.StableId;
import io.pulseautomate.map.manifest.model.Entity;
import io.pulseautomate.map.manifest.model.Manifest;
import io.pulseautomate.map.manifest.model.Service;
import io.pulseautomate.map.manifest.serde.ManifestCanonicalizer;
import io.pulseautomate.map.manifest.serde.ManifestJson;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.*;

public final class LockBuilder {
  private LockBuilder() {}

  public static LockFile build(Manifest manifest, LockFile previous, Instant nowUtc) {
    Objects.requireNonNull(manifest, "manifest");
    Objects.requireNonNull(nowUtc, "nowUtc");

    var manifestHash = sha256Hex(serialise(manifest));

    Map<String, String> entityMap = new LinkedHashMap<>();
    Map<String, String> prevMap =
        previous != null && previous.entity_map() != null ? previous.entity_map() : Map.of();

    for (Entity e : ManifestCanonicalizer.canonicalize(manifest).entities()) {
      var stable = e.stable_id();
      if (stable == null || stable.isBlank()) stable = prevMap.get(e.entity_id());

      if (stable == null || stable.isBlank()) stable = StableId.derive(e.entity_id());

      entityMap.put(e.entity_id(), stable);
    }

    Map<String, String> serviceSig = new LinkedHashMap<>();
    for (Service s : ManifestCanonicalizer.canonicalize(manifest).services()) {
      var key = s.domain() + "." + s.service();
      var shape = signatureShape(s);
      serviceSig.put(key, sha256Hex(shape));
    }

    Map<String, List<String>> attrEnums = new LinkedHashMap<>();
    for (Entity e : ManifestCanonicalizer.canonicalize(manifest).entities()) {
      if (e.attributes() == null) continue;
      for (var entry : e.attributes().entrySet()) {
        var attr = entry.getKey();
        var desc = entry.getValue();

        if (desc.enumValues() == null || desc.enumValues().isEmpty()) continue;
        var key = e.domain() + "." + attr;
        var list = attrEnums.computeIfAbsent(key, k -> new ArrayList<>());
        list.addAll(desc.enumValues());
      }
    }

    for (var it : attrEnums.entrySet()) {
      List<String> uniqSorted = it.getValue().stream().distinct().sorted().toList();
      it.setValue(uniqSorted);
    }

    return new LockFile(
        1,
        manifestHash,
        DateTimeFormatter.ISO_INSTANT.format(nowUtc),
        entityMap,
        serviceSig,
        attrEnums.isEmpty() ? null : attrEnums);
  }

  private static String serialise(Manifest m) {
    try {
      return ManifestJson.toPrettyString(ManifestCanonicalizer.canonicalize(m));
    } catch (Exception e) {
      throw new RuntimeException("Failed to serialise manifest", e);
    }
  }

  private static String signatureShape(Service s) {
    var fields = new ArrayList<String>();

    if (s.fields() != null) {
      s.fields().entrySet().stream()
          .sorted(Map.Entry.comparingByKey())
          .forEach(
              e -> {
                var serviceField = e.getValue();
                var sb = new StringBuilder();

                sb.append(e.getKey()).append(":").append(nullToEmpty(serviceField.type()));

                if (Boolean.TRUE.equals(serviceField.required())) sb.append(":req");
                if (serviceField.unit() != null && !serviceField.unit().isBlank())
                  sb.append(":").append(serviceField.unit());

                fields.add(sb.toString());
              });
    }

    return s.domain() + "." + s.service() + "|" + String.join("|", fields);
  }

  private static String sha256Hex(String s) {
    try {
      var md = MessageDigest.getInstance("SHA-256");
      var d = md.digest(s.getBytes(StandardCharsets.UTF_8));
      return Hex.of(d);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private static final class Hex {
    private static final char[] HEX = "0123456789abcdef".toCharArray();

    static String of(byte[] b) {
      var out = new char[b.length * 2];
      for (int i = 0, j = 0; i < b.length; i++) {
        var v = b[i] & 0xFF;
        out[j++] = HEX[v >> 4];
        out[j++] = HEX[v & 0x0f];
      }
      return new String(out);
    }
  }

  private static String nullToEmpty(String s) {
    return s == null ? "" : s;
  }
}
