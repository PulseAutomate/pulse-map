package io.pulseautomate.map.manifest.lock;

import static io.pulseautomate.map.manifest.util.Constants.*;

import io.pulseautomate.map.manifest.gen.model.*;
import io.pulseautomate.map.manifest.id.StableId;
import io.pulseautomate.map.manifest.serde.ManifestCanonicalizer;
import io.pulseautomate.map.manifest.util.Hashing;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.*;

public final class LockBuilder {
  private LockBuilder() {}

  public static LockFile build(Manifest manifest, LockFile previous, Instant nowUtc) {
    Objects.requireNonNull(manifest, "manifest");
    Objects.requireNonNull(nowUtc, "nowUtc");

    Manifest canon = ManifestCanonicalizer.canonicalize(manifest);
    var manifestHash = Hashing.sha256Hex(canon.toByteArray());

    var lockFile =
        LockFile.newBuilder()
            .setSchema(LOCK_SCHEMA_V1)
            .setManifestHash(manifestHash)
            .setGeneratedAt(DateTimeFormatter.ISO_INSTANT.format(nowUtc));

    Map<String, String> prevEntityMap = (previous != null) ? previous.getEntityMapMap() : Map.of();

    for (var e : canon.getEntitiesList()) {
      var stable = e.getStableId();
      if (stable.isBlank()) stable = prevEntityMap.get(e.getEntityId());
      if (stable == null || stable.isBlank()) stable = StableId.derive(e.getEntityId());
      lockFile.putEntityMap(e.getEntityId(), stable);
    }

    for (var s : canon.getServicesList()) {
      var key = s.getDomain() + "." + s.getService();
      var shape = signatureShape(s);
      lockFile.putServiceSig(key, Hashing.sha256Hex(shape));
    }

    Map<String, List<String>> attrEnums = new LinkedHashMap<>();
    for (var e : canon.getEntitiesList()) {
      for (var entry : e.getAttributesMap().entrySet()) {
        var attr = entry.getKey();
        var desc = entry.getValue();

        if (desc.getEnumValuesCount() == 0) continue;
        var key = e.getDomain() + "." + attr;
        var list = attrEnums.computeIfAbsent(key, k -> new ArrayList<>());
        list.addAll(desc.getEnumValuesList());
      }
    }

    for (var it : attrEnums.entrySet()) {
      List<String> uniqSorted = it.getValue().stream().distinct().sorted().toList();
      lockFile.putAttrEnums(it.getKey(), EnumCache.newBuilder().addAllValues(uniqSorted).build());
    }

    return lockFile.build();
  }

  private static String signatureShape(Service s) {
    var fields = new ArrayList<String>();

    if (!s.getFieldsMap().isEmpty()) {
      s.getFieldsMap().entrySet().stream()
          .sorted(Map.Entry.comparingByKey())
          .forEach(
              e -> {
                var serviceField = e.getValue();
                var sb = new StringBuilder();

                sb.append(e.getKey())
                    .append(SIG_PART_SEP)
                    .append(nullToEmpty(serviceField.getType()));

                if (serviceField.getRequired()) sb.append(SIG_REQ_FLAG);
                if (!serviceField.getUnit().isBlank())
                  sb.append(SIG_PART_SEP).append(serviceField.getUnit());

                fields.add(sb.toString());
              });
    }

    return s.getDomain()
        + "."
        + s.getService()
        + SIG_MAIN_SEP
        + String.join(String.valueOf(SIG_FIELD_SEP), fields);
  }

  private static String nullToEmpty(String s) {
    return s == null ? "" : s;
  }
}
