package io.pulseautomate.map.manifest.builder;

import static io.pulseautomate.map.manifest.util.Constants.UNIT_CELSIUS_WITH_SYMBOL;
import static io.pulseautomate.map.manifest.util.Names.Attr.*;
import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.Assertions.within;

import io.pulseautomate.map.manifest.gen.model.FieldKind;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ManifestBuilderTest {

  @Test
  void climate_celsius_modes_and_caps() {
    var attributes = new LinkedHashMap<String, Object>();
    attributes.put("hvac_modes", List.of("off", "heat", "auto"));
    attributes.put("preset_modes", List.of("eco", "comfort"));
    attributes.put("min_temp", 5.0);
    attributes.put("max_temp", 30.0);
    attributes.put("target_temp_step", 0.5);
    attributes.put("temperature_unit", "°C");

    var state =
        Map.<String, Object>of(
            "entity_id", "climate.living_room_trv",
            "state", "heat",
            "attributes", attributes);

    var m = new ManifestBuilder().build("2025.6", List.of(state), List.of());

    assertThat(m.getEntitiesList()).hasSize(1);
    var e = m.getEntities(0);
    assertThat(e.getDomain()).isEqualTo("climate");

    var attrs = e.getAttributesMap();
    assertThat(attrs).containsKeys(HVAC_MODE, PRESET_MODE, CURRENT_TEMP_C, TARGET_TEMP_C);

    var hvac = attrs.get(HVAC_MODE);
    assertThat(hvac.getKind()).isEqualTo(FieldKind.ENUM);
    assertThat(hvac.getEnumValuesList()).containsExactly("off", "heat", "auto");

    var tgt = attrs.get(TARGET_TEMP_C);
    assertThat(tgt.getUnit()).isEqualTo(UNIT_CELSIUS_WITH_SYMBOL);
    assertThat(tgt.getCaps().getMin()).isEqualTo(5.0);
    assertThat(tgt.getCaps().getMax()).isEqualTo(30.0);
    assertThat(tgt.getCaps().getStep()).isEqualTo(0.5);
  }

  @Test
  void climate_fahrenheit_caps_normalized_to_celsius() {
    var attributes = new LinkedHashMap<String, Object>();
    attributes.put("min_temp", 41.0); // ≈ 5C
    attributes.put("max_temp", 86.0); // ≈ 30C
    attributes.put("target_temp_step", 1.0); // ≈ 0.555C
    attributes.put("temperature_unit", "F");

    var state =
        Map.<String, Object>of(
            "entity_id", "climate.trv",
            "state", "heat",
            "attributes", attributes);

    var m = new ManifestBuilder().build("2025.6", List.of(state), List.of());

    var caps = m.getEntities(0).getAttributesMap().get(TARGET_TEMP_C).getCaps();
    assertThat(caps.getMin()).isCloseTo(5.0, within(0.01));
    assertThat(caps.getMax()).isCloseTo(30.0, within(0.01));
    assertThat(caps.getStep()).isCloseTo(0.555, within(0.01));
  }

  @Test
  void services_are_carried_through_with_required_flags() {
    var fields =
        Map.<String, Object>of("temperature", Map.of("required", true, "description", "Set temp"));
    var serviceDetails = Map.<String, Object>of("set_temperature", Map.of("fields", fields));
    var serviceDomain = Map.<String, Object>of("domain", "climate", "services", serviceDetails);

    var m = new ManifestBuilder().build("2025.6", List.of(), List.of(serviceDomain));

    assertThat(m.getServicesList()).hasSize(1);
    var s = m.getServices(0);
    assertThat(s.getDomain()).isEqualTo("climate");
    assertThat(s.getService()).isEqualTo("set_temperature");
    assertThat(s.getFieldsMap()).containsKey("temperature");
    assertThat(s.getFieldsMap().get("temperature").getRequired()).isTrue();
    assertThat(s.getFieldsMap().get("temperature").getType()).isEqualTo("number");
    assertThat(s.getFieldsMap().get("temperature").getUnit()).isEqualTo(UNIT_CELSIUS_WITH_SYMBOL);
  }
}
