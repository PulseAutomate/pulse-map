package io.pulseautomate.map.manifest.infer;

import static io.pulseautomate.map.manifest.util.Names.Attr.COLOR_TEMP_K;
import static io.pulseautomate.map.manifest.util.Names.Attr.HUE_DEG;
import static io.pulseautomate.map.manifest.util.Names.Attr.SATURATION_PCT;
import static io.pulseautomate.map.manifest.util.Names.HaAttr.MAX_MIREDS;
import static io.pulseautomate.map.manifest.util.Names.HaAttr.MIN_MIREDS;
import static io.pulseautomate.map.manifest.util.Names.HaAttr.SUPPORTED_COLOR_MODES;
import static org.assertj.core.api.Assertions.*;

import io.pulseautomate.map.ha.model.HAState;
import io.pulseautomate.map.manifest.model.AttributeDesc;
import java.util.LinkedHashMap;
import java.util.List;
import org.junit.jupiter.api.Test;

class AttributeRulesTest {

  @Test
  void colorTempKelvinFromMireds_produces_kelvin_caps() {
    var attrs = new LinkedHashMap<String, Object>();
    attrs.put(MIN_MIREDS, 153); // common Philips Hue min (≈ 6536 K)
    attrs.put(MAX_MIREDS, 500); // common max (2000 K)
    attrs.put(SUPPORTED_COLOR_MODES, List.of("color_temp"));

    var state = new HAState("light.any", "on", attrs, null, null);

    var rule = AttributeRules.colorTempKelvinFromMireds(COLOR_TEMP_K, MIN_MIREDS, MAX_MIREDS);
    var entry = rule.infer(state).orElseThrow();
    AttributeDesc desc = entry.getValue();

    assertThat(entry.getKey()).isEqualTo(COLOR_TEMP_K);
    assertThat(desc.unit()).isEqualTo("K");
    // raw conversion: minK = 1e6 / maxMireds = 2000, maxK = 1e6 / minMireds ≈ 6535.95
    assertThat(desc.caps().min()).isCloseTo(2000.0, within(0.1));
    assertThat(desc.caps().max()).isCloseTo(6535.95, within(0.5));
    assertThat(desc.caps().step()).isEqualTo(50.0);
  }

  @Test
  void hs_color_rules_present_when_supported_color_modes_contains_hs() {
    var attrs = new LinkedHashMap<String, Object>();
    attrs.put(SUPPORTED_COLOR_MODES, List.of("hs"));
    var state = new HAState("light.rgb", "on", attrs, null, null);

    var hueRule =
        AttributeRules.presentIf(
            AttributeRules.hueDegrees(HUE_DEG), AttributeRules.colorModeIncludes("hs"));
    var satRule =
        AttributeRules.presentIf(
            AttributeRules.saturationPct(SATURATION_PCT), AttributeRules.colorModeIncludes("hs"));

    assertThat(hueRule.infer(state)).isPresent();
    assertThat(satRule.infer(state)).isPresent();
  }
}
