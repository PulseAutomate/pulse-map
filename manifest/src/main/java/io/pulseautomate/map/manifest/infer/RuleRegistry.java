package io.pulseautomate.map.manifest.infer;

import static io.pulseautomate.map.manifest.infer.AttributeRules.*;
import static io.pulseautomate.map.manifest.util.Constants.*;
import static io.pulseautomate.map.manifest.util.Names.Attr.*;
import static io.pulseautomate.map.manifest.util.Names.ColorMode.COLOR_TEMP;
import static io.pulseautomate.map.manifest.util.Names.ColorMode.HS;
import static io.pulseautomate.map.manifest.util.Names.Domain.*;
import static io.pulseautomate.map.manifest.util.Names.HaAttr.BRIGHTNESS;
import static io.pulseautomate.map.manifest.util.Names.HaAttr.CURRENT_POSITION;
import static io.pulseautomate.map.manifest.util.Names.HaAttr.CURRENT_TILT_POSITION;
import static io.pulseautomate.map.manifest.util.Names.HaAttr.DIRECTION_LIST;
import static io.pulseautomate.map.manifest.util.Names.HaAttr.EFFECT_LIST;
import static io.pulseautomate.map.manifest.util.Names.HaAttr.HVAC_MODES;
import static io.pulseautomate.map.manifest.util.Names.HaAttr.MAX;
import static io.pulseautomate.map.manifest.util.Names.HaAttr.MAX_MIREDS;
import static io.pulseautomate.map.manifest.util.Names.HaAttr.MAX_TEMP;
import static io.pulseautomate.map.manifest.util.Names.HaAttr.MIN;
import static io.pulseautomate.map.manifest.util.Names.HaAttr.MIN_MIREDS;
import static io.pulseautomate.map.manifest.util.Names.HaAttr.MIN_TEMP;
import static io.pulseautomate.map.manifest.util.Names.HaAttr.PERCENTAGE;
import static io.pulseautomate.map.manifest.util.Names.HaAttr.POSITION;
import static io.pulseautomate.map.manifest.util.Names.HaAttr.PRESET_MODES;
import static io.pulseautomate.map.manifest.util.Names.HaAttr.SOUND_MODE_LIST;
import static io.pulseautomate.map.manifest.util.Names.HaAttr.SOURCE_LIST;
import static io.pulseautomate.map.manifest.util.Names.HaAttr.SPEED_LIST;
import static io.pulseautomate.map.manifest.util.Names.HaAttr.STEP;
import static io.pulseautomate.map.manifest.util.Names.HaAttr.SUPPORTED_COLOR_MODES;
import static io.pulseautomate.map.manifest.util.Names.HaAttr.TARGET_TEMP_STEP;
import static io.pulseautomate.map.manifest.util.Names.HaAttr.TEMPERATURE_UNIT;
import static io.pulseautomate.map.manifest.util.Names.HaAttr.TILT_POSITION;
import static io.pulseautomate.map.manifest.util.Names.HaAttr.UNIT_OF_MEASUREMENT;
import static io.pulseautomate.map.manifest.util.Names.HaAttr.VOLUME_LEVEL;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class RuleRegistry {
  private final Map<String, DomainRuleSet> byDomain;

  private RuleRegistry(Map<String, DomainRuleSet> byDomain) {
    this.byDomain = byDomain;
  }

  public DomainRuleSet forDomain(String domain) {
    return byDomain.get(domain);
  }

  public static RuleRegistry defaultRegistry() {
    var climate =
        create(
            CLIMATE,
            enumFrom(HVAC_MODE, HVAC_MODES, HVAC_MODE, false),
            enumFrom(PRESET_MODE, PRESET_MODES, null, true),
            numberDescriptor(CURRENT_TEMP_C, UNIT_CELSIUS_WITH_SYMBOL),
            numberWithCapsTempC(
                TARGET_TEMP_C,
                UNIT_CELSIUS_WITH_SYMBOL,
                MIN_TEMP,
                MAX_TEMP,
                TARGET_TEMP_STEP,
                TEMPERATURE_UNIT));

    var light =
        create(
            LIGHT,
            presentIfAny(percentPct(BRIGHTNESS_PCT), BRIGHTNESS, SUPPORTED_COLOR_MODES),
            enumFrom(EFFECT, EFFECT_LIST, null, true),
            presentIf(
                colorTempKelvinFromMireds(COLOR_TEMP_K, MIN_MIREDS, MAX_MIREDS),
                colorModeIncludes(COLOR_TEMP)
                    .or(state -> state.attributes().containsKey(MIN_MIREDS))),
            presentIf(hueDegrees(HUE_DEG), colorModeIncludes(HS)),
            presentIf(saturationPct(SATURATION_PCT), colorModeIncludes(HS)));

    var fan =
        create(
            FAN,
            presentIfAny(percentPct(SPEED_PCT), PERCENTAGE, SPEED_LIST),
            enumFrom(PRESET_MODE, PRESET_MODES, null, true),
            enumFrom(DIRECTION, DIRECTION_LIST, DIRECTION, true),
            booleanFlag(OSCILLATING, OSCILLATING));

    var cover =
        create(
            COVER,
            presentIfAny(percentPct(POSITION_PCT), CURRENT_POSITION, POSITION),
            presentIfAny(percentPct(TILT_POSITION_PCT), CURRENT_TILT_POSITION, TILT_POSITION));

    var media =
        create(
            MEDIA_PLAYER,
            presentIfAny(percentPct(VOLUME_PCT), VOLUME_LEVEL),
            enumFrom(SOURCE, SOURCE_LIST, SOURCE, true),
            enumFrom(SOUND_MODE, SOUND_MODE_LIST, SOUND_MODE, true));

    var number =
        create(
            NUMBER,
            presentIfAny(
                numberWithCapsFromKeys(VALUE, UNIT_OF_MEASUREMENT, MIN, MAX, STEP),
                MIN,
                MAX,
                STEP,
                UNIT_OF_MEASUREMENT));

    return new RuleRegistry(createRuleMap(climate, light, fan, cover, media, number));
  }

  private static DomainRuleSet create(String domain, AttributeRule... attributeRule) {
    return new DomainRuleSet(domain, List.of(attributeRule));
  }

  private static Map<String, DomainRuleSet> createRuleMap(DomainRuleSet... rules) {
    var map = new LinkedHashMap<String, DomainRuleSet>();
    for (var rule : rules) {
      map.put(rule.domain(), rule);
    }
    return map;
  }
}
