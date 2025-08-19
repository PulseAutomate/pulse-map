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
import io.pulseautomate.map.manifest.model.*;
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

    assertThat(m.entities()).hasSize(1);
    var e = m.entities().getFirst();
    assertThat(e.domain()).isEqualTo("climate");

    var attrs = e.attributes();
    assertThat(attrs).containsKeys(HVAC_MODE, PRESET_MODE, CURRENT_TEMP_C, TARGET_TEMP_C);

    var hvac = attrs.get(HVAC_MODE);
    assertThat(hvac.kind()).isEqualTo(FieldKind.ENUM);
    assertThat(hvac.enumValues()).containsExactly("off", "heat", "auto");

    var tgt = attrs.get(TARGET_TEMP_C);
    assertThat(tgt.unit()).isEqualTo(UNIT_CELSIUS_WITH_SYMBOL);
    assertThat(tgt.caps().min()).isEqualTo(5.0);
    assertThat(tgt.caps().max()).isEqualTo(30.0);
    assertThat(tgt.caps().step()).isEqualTo(0.5);
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

    var caps = m.entities().getFirst().attributes().get(TARGET_TEMP_C).caps();
    assertThat(caps.min()).isCloseTo(5.0, within(0.01));
    assertThat(caps.max()).isCloseTo(30.0, within(0.01));
    assertThat(caps.step()).isCloseTo(0.555, within(0.01));
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

    var attrs = m.entities().getFirst().attributes();
    assertThat(attrs).containsKeys(BRIGHTNESS_PCT, EFFECT, COLOR_TEMP_K, HUE_DEG, SATURATION_PCT);

    var b = attrs.get(BRIGHTNESS_PCT);
    assertThat(b.kind()).isEqualTo(FieldKind.NUMBER);
    assertThat(b.unit()).isEqualTo(UNIT_PERCENT);
    assertThat(b.caps().min()).isEqualTo(0.0);
    assertThat(b.caps().max()).isEqualTo(100.0);
    assertThat(b.caps().step()).isEqualTo(1.0);

    var k = attrs.get(COLOR_TEMP_K).caps();
    assertThat(k.min()).isCloseTo(2000.0, within(0.1));
    assertThat(k.max()).isCloseTo(6535.95, within(0.5));
    assertThat(k.step()).isEqualTo(50.0);
  }

  @Test
  void fan_speed_direction_preset_and_oscillation() {
    var a = new LinkedHashMap<String, Object>();
    a.put("percentage", 33);
    a.put("preset_modes", List.of("auto", "boost"));
    a.put("direction", "forward"); // fallbackOneOfKey path
    a.put("oscillating", true);

    var state = new HAState("fan.tower", "on", a, null, null);
    var m = new ManifestBuilder().build("2025.6", new HASnapshot(List.of(state), List.of()));

    var attrs = m.entities().getFirst().attributes();
    assertThat(attrs).containsKeys(SPEED_PCT, PRESET_MODE, DIRECTION, OSCILLATING);
    assertThat(attrs.get(SPEED_PCT).caps().max()).isEqualTo(100.0);
    assertThat(attrs.get(DIRECTION).kind()).isEqualTo(FieldKind.ENUM);
    assertThat(attrs.get(PRESET_MODE).enumValues()).containsExactly("auto", "boost");
    assertThat(attrs.get(OSCILLATING).kind()).isEqualTo(FieldKind.BOOLEAN);
  }

  @Test
  void cover_position_and_tilt_percentage() {
    var a = new LinkedHashMap<String, Object>();
    a.put("current_position", 42);
    a.put("current_tilt_position", 10);

    var state = new HAState("cover.blind", "open", a, null, null);
    var m = new ManifestBuilder().build("2025.6", new HASnapshot(List.of(state), List.of()));

    var attrs = m.entities().getFirst().attributes();
    assertThat(attrs).containsKeys(POSITION_PCT, TILT_POSITION_PCT);
    assertThat(attrs.get(POSITION_PCT).caps().min()).isEqualTo(0.0);
    assertThat(attrs.get(TILT_POSITION_PCT).caps().max()).isEqualTo(100.0);
  }

  @Test
  void media_player_volume_source_sound_mode() {
    var a = new LinkedHashMap<String, Object>();
    a.put("volume_level", 0.35);
    a.put("source_list", List.of("TV", "HDMI1"));
    a.put("sound_mode_list", List.of("stereo", "movie"));

    var state = new HAState("media_player.lg_tv", "on", a, null, null);
    var m = new ManifestBuilder().build("2025.6", new HASnapshot(List.of(state), List.of()));

    var attrs = m.entities().getFirst().attributes();
    assertThat(attrs).containsKeys(VOLUME_PCT, SOURCE, SOUND_MODE);
    assertThat(attrs.get(VOLUME_PCT).unit()).isEqualTo(UNIT_PERCENT);
    assertThat(attrs.get(SOURCE).enumValues()).containsExactly("TV", "HDMI1");
  }

  @Test
  void services_are_carried_through_with_required_flags() {
    var fields = Map.of("temperature", new HAServiceField(true, null, null, "Set temp"));
    var svc = new HAService("climate", "set_temperature", fields);

    var m = new ManifestBuilder().build("2025.6", new HASnapshot(List.of(), List.of(svc)));
    assertThat(m.services()).hasSize(1);
    var s = m.services().getFirst();
    assertThat(s.domain()).isEqualTo("climate");
    assertThat(s.service()).isEqualTo("set_temperature");
    assertThat(s.fields()).containsKey("temperature");
    assertThat(s.fields().get("temperature").required()).isTrue();
    assertThat(s.fields().get("temperature").type()).isEqualTo("number");
    assertThat(s.fields().get("temperature").unit()).isEqualTo(UNIT_CELSIUS_WITH_SYMBOL);
  }
}
