package io.pulseautomate.map.manifest.serde;

import static org.assertj.core.api.Assertions.assertThat;

import io.pulseautomate.map.manifest.gen.model.*;
import io.pulseautomate.map.manifest.util.Hashing;
import java.util.List;
import org.junit.jupiter.api.Test;

class ManifestPbTest {
  @Test
  void canonicalization_produces_deterministic_binary_output() {
    var presetAttr =
        AttributeDesc.newBuilder()
            .setKind(FieldKind.ENUM)
            .setOptional(true)
            .addAllEnumValues(List.of("eco", "comfort"))
            .build();
    var hvacAttr =
        AttributeDesc.newBuilder()
            .setKind(FieldKind.ENUM)
            .addAllEnumValues(List.of("off", "heat", "auto"))
            .build();

    var entity =
        Entity.newBuilder()
            .setStableId("stable:aa12…")
            .setEntityId("climate.living_room_trv")
            .setDomain("climate")
            .putAttributes("preset_mode", presetAttr)
            .putAttributes("hvac_mode", hvacAttr)
            .build();

    var manifest =
        Manifest.newBuilder().setSchema(1).setHaVersion("2025.6").addEntities(entity).build();

    var canon = ManifestCanonicalizer.canonicalize(manifest);
    var bytes = canon.toByteArray();
    var hash = Hashing.sha256Hex(bytes);

    assertThat(hash).isEqualTo("67a9f71ffcd9e57fd929b499af59ea0af0704ea664e7bdc5935ab65beba1c809");
  }
}
