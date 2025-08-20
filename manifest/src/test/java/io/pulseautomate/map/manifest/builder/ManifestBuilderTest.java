package io.pulseautomate.map.manifest.builder;

import static io.pulseautomate.map.manifest.util.Constants.UNIT_CELSIUS_WITH_SYMBOL;
import static io.pulseautomate.map.manifest.util.Constants.UNIT_PERCENT;
import static io.pulseautomate.map.manifest.util.Names.Attr.*;
import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.Assertions.within;

import io.pulseautomate.map.ha.model.HAService;
import io.pulseautomate.map.ha.model.HAServiceField;
import io.pulseautomate.map.ha.model.HASnapshot;
import io.pulseautomate.map.ha.model.HAState;
import io.pulseautomate.map.manifest.gen.model.FieldKind;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ManifestBuilderTest {

  @Test
  void climate_celsius_modes_and_caps() {
    var a = new LinkedHashMap<String, Object>();
    a.put("hvac_modes", List.of("off", "heat", "auto"));
    a.put("preset_modes", List.of("eco", "comfort"));
    a.put("min_temp", 5.0);
    a.put("max_temp", 30.0);
    a.put("target_temp_step", 0.5);
    a.put("temperature_unit", "°C");

    var state = new HAState("climate.living_room_trv", "heat", a, null, null);
    var m = new ManifestBuilder().build("2025.6", new HASnapshot(List.of(state), List.of()));

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
    var a = new LinkedHashMap<String, Object>();
    a.put("min_temp", 41.0); // ≈ 5C
    a.put("max_temp", 86.0); // ≈ 30C
    a.put("target_temp_step", 1.0); // ≈ 0.555C
    a.put("temperature_unit", "F");

    var state = new HAState("climate.trv", "heat", a, null, null);
    var m = new ManifestBuilder().build("2025.6", new HASnapshot(List.of(state), List.of()));

    var caps = m.getEntities(0).getAttributesMap().get(TARGET_TEMP_C).getCaps();
    assertThat(caps.getMin()).isCloseTo(5.0, within(0.01));
    assertThat(caps.getMax()).isCloseTo(30.0, within(0.01));
    assertThat(caps.getStep()).isCloseTo(0.555, within(0.01));
  }

  @Test
  void light_brightness_effect_color_and_hs() {
    var a = new LinkedHashMap<String, Object>();
    a.put("brightness", 180);
    a.put("effect_list", List.of("rainbow", "night"));
    a.put("min_mireds", 153);
    a.put("max_mireds", 500);
    a.put("supported_color_modes", List.of("hs", "color_temp"));

    var state = new HAState("light.lamp", "on", a, null, null);
    var m = new ManifestBuilder().build("2025.6", new HASnapshot(List.of(state), List.of()));

    var attrs = m.getEntities(0).getAttributesMap();
    assertThat(attrs).containsKeys(BRIGHTNESS_PCT, EFFECT, COLOR_TEMP_K, HUE_DEG, SATURATION_PCT);

    var b = attrs.get(BRIGHTNESS_PCT);
    assertThat(b.getKind()).isEqualTo(FieldKind.NUMBER);
    assertThat(b.getUnit()).isEqualTo(UNIT_PERCENT);
    assertThat(b.getCaps().getMin()).isEqualTo(0.0);
    assertThat(b.getCaps().getMax()).isEqualTo(100.0);
    assertThat(b.getCaps().getStep()).isEqualTo(1.0);

    var k = attrs.get(COLOR_TEMP_K).getCaps();
    assertThat(k.getMin()).isCloseTo(2000.0, within(0.1));
    assertThat(k.getMax()).isCloseTo(6535.95, within(0.5));
    assertThat(k.getStep()).isEqualTo(50.0);
  }

  @Test
  void services_are_carried_through_with_required_flags() {
    var fields = Map.of("temperature", new HAServiceField(true, null, null, "Set temp"));
    var svc = new HAService("climate", "set_temperature", fields);

    var m = new ManifestBuilder().build("2025.6", new HASnapshot(List.of(), List.of(svc)));
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
