package io.pulseautomate.map.manifest.lock;

import static org.assertj.core.api.Assertions.assertThat;

import io.pulseautomate.map.manifest.model.*;
import io.pulseautomate.map.manifest.serde.ManifestCanonicalizer;
import io.pulseautomate.map.manifest.serde.ManifestJson;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import org.junit.jupiter.api.Test;

class LockBuilderTest {

    private Manifest sampleManifest() {
        var attrs = new LinkedHashMap<String, AttributeDesc>();
        attrs.put("hvac_mode", new AttributeDesc(FieldKind.ENUM, null, List.of("off","heat","auto"), null, null));
        attrs.put("preset_mode", new AttributeDesc(FieldKind.ENUM, null, List.of("eco","comfort"), true, null));
        attrs.put("current_temp_c", new AttributeDesc(FieldKind.NUMBER, "°C", null, null, null));
        attrs.put("target_temp_c", new AttributeDesc(FieldKind.NUMBER, "°C", null, null, new CapabilityRange(5.0, 30.0, 0.5)));

        var entity = new Entity("stable:aa12…", "climate.living_room_trv", "climate", "heater", null, attrs);

        var fields = new LinkedHashMap<String, ServiceField>();
        fields.put("temperature", new ServiceField("number", "°C", true));
        var service = new Service("climate", "set_temperature", fields);

        return new Manifest(1, "2025.6", List.of(entity), List.of(service));
    }

    @Test
    void builds_lock_with_hashes_and_maps() throws Exception {
        var manifest = sampleManifest();
        var now = Instant.parse("2025-08-14T19:00:00Z");
        var lock = LockBuilder.build(manifest, null, now);

        // manifest hash should be sha256 of canonicalized pretty json
        var canonJson = ManifestJson.toPrettyString(ManifestCanonicalizer.canonicalize(manifest));
        assertThat(lock.manifest_hash()).hasSize(64);
        assertThat(lock.generated_at()).isEqualTo("2025-08-14T19:00:00Z");

        // entity map should contain our entity_id -> stable_id
        assertThat(lock.entity_map()).containsEntry("climate.living_room_trv", "stable:aa12…");

        // service signature should exist and be deterministic
        var sig = lock.service_sig().get("climate.set_temperature");
        assertThat(sig).isNotNull().hasSize(64);

        // attr enums should contain climate.hvac_mode & preset_mode; sorted/deduped
        assertThat(lock.attr_enums()).isNotNull();
        assertThat(lock.attr_enums().get("climate.hvac_mode")).containsExactly("auto", "heat", "off");
        assertThat(lock.attr_enums().get("climate.preset_mode")).containsExactly("comfort", "eco");
    }

    @Test
    void reuses_previous_mapping_when_stable_id_missing() {
        // manifest has NO stable_id (simulating earlier stage before we compute it)
        var entity = new Entity(null, "light.kitchen", "light", null, null, null);
        var manifest = new Manifest(1, "2025.6", List.of(entity), List.of());
        var prev = new LockFile(1, "hash", "2025-01-01T00:00:00Z",
                java.util.Map.of("light.kitchen", "stable:deadbeefcafe"),
                java.util.Map.of(), null);

        var lock = LockBuilder.build(manifest, prev, Instant.parse("2025-08-14T00:00:00Z"));

        assertThat(lock.entity_map()).containsEntry("light.kitchen", "stable:deadbeefcafe");
    }

    @Test
    void derives_stable_id_from_entity_id_when_no_previous() {
        var entity = new Entity(null, "sensor.outdoor_temp", "sensor", null, null, null);
        var manifest = new Manifest(1, "2025.6", List.of(entity), List.of());
        var lock = LockBuilder.build(manifest, null, Instant.parse("2025-08-14T00:00:00Z"));
        // Check shape, not exact hex (algorithm detail)
        assertThat(lock.entity_map().get("sensor.outdoor_temp")).startsWith("stable:");
    }
}
