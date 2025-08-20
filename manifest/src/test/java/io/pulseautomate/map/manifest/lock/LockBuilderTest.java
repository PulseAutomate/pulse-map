package io.pulseautomate.map.manifest.lock;

import static org.assertj.core.api.Assertions.assertThat;

import io.pulseautomate.map.manifest.gen.model.*;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class LockBuilderTest {

  private Manifest sampleManifest() {
    var hvacAttr =
        AttributeDesc.newBuilder()
            .setKind(FieldKind.ENUM)
            .addAllEnumValues(List.of("off", "heat", "auto"))
            .build();
    var presetAttr =
        AttributeDesc.newBuilder()
            .setKind(FieldKind.ENUM)
            .setOptional(true)
            .addAllEnumValues(List.of("eco", "comfort"))
            .build();

    var entity =
        Entity.newBuilder()
            .setStableId("stable:aa12…")
            .setEntityId("climate.living_room_trv")
            .setDomain("climate")
            .setDeviceClass("heater")
            .putAttributes("hvac_mode", hvacAttr)
            .putAttributes("preset_mode", presetAttr)
            .build();

    var service =
        Service.newBuilder()
            .setDomain("climate")
            .setService("set_temperature")
            .putFields(
                "temperature",
                ServiceField.newBuilder().setType("number").setUnit("°C").setRequired(true).build())
            .build();

    return Manifest.newBuilder()
        .setSchema(1)
        .setHaVersion("2025.6")
        .addEntities(entity)
        .addServices(service)
        .build();
  }

  @Test
  void builds_lock_with_hashes_and_maps() {
    var manifest = sampleManifest();
    var now = Instant.parse("2025-08-14T19:00:00Z");
    var lock = LockBuilder.build(manifest, null, now);

    assertThat(lock.getManifestHash()).hasSize(64);
    assertThat(lock.getGeneratedAt()).isEqualTo("2025-08-14T19:00:00Z");
    assertThat(lock.getEntityMapMap()).containsEntry("climate.living_room_trv", "stable:aa12…");
    assertThat(lock.getServiceSigMap().get("climate.set_temperature")).isNotNull().hasSize(64);
    assertThat(lock.getAttrEnumsMap().get("climate.hvac_mode").getValuesList())
        .containsExactly("auto", "heat", "off");
    assertThat(lock.getAttrEnumsMap().get("climate.preset_mode").getValuesList())
        .containsExactly("comfort", "eco");
  }

  @Test
  void reuses_previous_mapping_when_stable_id_missing() {
    var entity = Entity.newBuilder().setEntityId("light.kitchen").setDomain("light").build();
    var manifest =
        Manifest.newBuilder().setSchema(1).setHaVersion("2025.6").addEntities(entity).build();

    var prev =
        LockFile.newBuilder()
            .setSchema(1)
            .setManifestHash("hash")
            .setGeneratedAt("2025-01-01T00:00:00Z")
            .putEntityMap("light.kitchen", "stable:deadbeefcafe")
            .build();

    var lock = LockBuilder.build(manifest, prev, Instant.parse("2025-08-14T00:00:00Z"));

    assertThat(lock.getEntityMapMap()).containsEntry("light.kitchen", "stable:deadbeefcafe");
  }

  @Test
  void derives_stable_id_from_entity_id_when_no_previous() {
    var entity = Entity.newBuilder().setEntityId("sensor.outdoor_temp").setDomain("sensor").build();
    var manifest =
        Manifest.newBuilder().setSchema(1).setHaVersion("2025.6").addEntities(entity).build();

    var lock = LockBuilder.build(manifest, null, Instant.parse("2025-08-14T00:00:00Z"));

    assertThat(lock.getEntityMapMap().get("sensor.outdoor_temp")).startsWith("stable:");
  }
}
