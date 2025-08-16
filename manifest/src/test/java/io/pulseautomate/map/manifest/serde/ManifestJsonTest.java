package io.pulseautomate.map.manifest.serde;

import static org.assertj.core.api.Assertions.assertThat;

import io.pulseautomate.map.manifest.model.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import org.junit.jupiter.api.Test;

class ManifestJsonTest {

  @Test
  void writes_canonical_pretty_json_matching_golden() throws Exception {
    var attrs = new LinkedHashMap<String, AttributeDesc>();
    attrs.put(
        "hvac_mode",
        new AttributeDesc(FieldKind.ENUM, null, List.of("off", "heat", "auto"), null, null));
    attrs.put(
        "preset_mode",
        new AttributeDesc(FieldKind.ENUM, null, List.of("eco", "comfort"), true, null));
    attrs.put("current_temp_c", new AttributeDesc(FieldKind.NUMBER, "°C", null, null, null));
    attrs.put(
        "target_temp_c",
        new AttributeDesc(FieldKind.NUMBER, "°C", null, null, new CapabilityRange(5.0, 30.0, 0.5)));

    var entity =
        new Entity("stable:aa12…", "climate.living_room_trv", "climate", "heater", null, attrs);

    var fields = new LinkedHashMap<String, ServiceField>();
    fields.put("temperature", new ServiceField("number", "°C", true));

    var service = new Service("climate", "set_temperature", fields);

    var manifest = new Manifest(1, "2025.6", List.of(entity), List.of(service));
    var canon = ManifestCanonicalizer.canonicalize(manifest);

    // write to string and compare
    var out = ManifestJson.toPrettyString(canon).trim();
    var golden = readResource("golden/manifest_min.json").trim();

    assertThat(out).isEqualTo(golden);
  }

  private static String readResource(String name) throws Exception {
    var url = ManifestJsonTest.class.getClassLoader().getResource(name);
    if (url == null) throw new IllegalArgumentException("missing resource: " + name);
    return Files.readString(Path.of(url.toURI()), StandardCharsets.UTF_8);
  }
}
