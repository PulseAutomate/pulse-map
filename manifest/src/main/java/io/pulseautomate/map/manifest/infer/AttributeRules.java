package io.pulseautomate.map.manifest.infer;

import static io.pulseautomate.map.manifest.util.Constants.*;
import static io.pulseautomate.map.manifest.util.Names.HaAttr.SUPPORTED_COLOR_MODES;

import io.pulseautomate.map.manifest.builder.MapHAState;
import io.pulseautomate.map.manifest.gen.model.AttributeDesc;
import io.pulseautomate.map.manifest.gen.model.CapabilityRange;
import io.pulseautomate.map.manifest.gen.model.FieldKind;
import io.pulseautomate.map.manifest.util.Temperature;
import java.util.*;
import java.util.function.Predicate;

public final class AttributeRules {
  private AttributeRules() {}

  public static AttributeRule enumFrom(
      String canonicalName, String listKey, String fallbackOneOfKey, boolean optional) {
    return state -> {
      var a = state.attributes();
      var list = asStringList(a.get(listKey));

      var desc = AttributeDesc.newBuilder().setKind(FieldKind.ENUM);
      if (optional) desc.setOptional(Boolean.TRUE);

      if (!list.isEmpty()) {
        desc.addAllEnumValues(list);
        return Optional.of(Map.entry(canonicalName, desc.build()));
      }

      if (fallbackOneOfKey != null && a.get(fallbackOneOfKey) instanceof String one) {
        desc.addEnumValues(one);
        return Optional.of(Map.entry(canonicalName, desc.build()));
      }

      return Optional.empty();
    };
  }

  public static AttributeRule numberWithCapsTempC(
      String canonicalName,
      String unit,
      String minKey,
      String maxKey,
      String stepKey,
      String unitAttrKey) {
    return state -> {
      var a = state.attributes();
      var min = num(a.get(minKey));
      var max = num(a.get(maxKey));
      var step = num(a.get(stepKey));
      var cap = CapabilityRange.newBuilder();

      if (step == null) step = 0.5;

      var u = str(a.get(unitAttrKey));
      if (u != null
          && (u.equalsIgnoreCase(UNIT_FAHRENHEIT_WITH_SYMBOL)
              || u.equalsIgnoreCase(UNIT_FAHRENHEIT)
              || u.equalsIgnoreCase(FAHRENHEIT))) {
        if (min != null) cap.setMin(Temperature.fToC(min));
        if (max != null) cap.setMax(Temperature.fToC(max));
        cap.setStep(step * (5.0 / 9.0));
      } else {
        if (min != null) cap.setMin(min);
        if (max != null) cap.setMax(max);
        cap.setStep(step);
      }

      return Optional.of(
          Map.entry(
              canonicalName,
              AttributeDesc.newBuilder()
                  .setKind(FieldKind.NUMBER)
                  .setUnit(unit)
                  .setCaps(cap)
                  .build()));
    };
  }

  public static AttributeRule numberWithFixedCaps(
      String canonicalName, String unit, double min, double max, double step) {
    return state ->
        Optional.of(
            Map.entry(
                canonicalName,
                AttributeDesc.newBuilder()
                    .setKind(FieldKind.NUMBER)
                    .setUnit(unit)
                    .setCaps(CapabilityRange.newBuilder().setMin(min).setMax(max).setStep(step))
                    .build()));
  }

  public static AttributeRule numberWithCapsFromKeys(
      String canonicalName, String unitKey, String minKey, String maxKey, String stepKey) {
    return state -> {
      var a = state.attributes();
      var min = num(a.get(minKey));
      var max = num(a.get(maxKey));
      var step = num(a.get(stepKey));
      var unit = str(a.get(unitKey));

      var cap = CapabilityRange.newBuilder();
      cap.setStep(step == null ? 1.0 : step);

      if (min == null || max == null && unit == null) return Optional.empty();
      cap.setMin(min);
      if (max != null) cap.setMax(max);

      var desc = AttributeDesc.newBuilder().setKind(FieldKind.NUMBER).setCaps(cap);

      if (unit != null) desc.setUnit(unit);

      return Optional.of(Map.entry(canonicalName, desc.build()));
    };
  }

  public static AttributeRule numberDescriptor(String canonicalName, String unit) {
    return state ->
        Optional.of(
            Map.entry(
                canonicalName,
                AttributeDesc.newBuilder().setKind(FieldKind.NUMBER).setUnit(unit).build()));
  }

  public static AttributeRule presentIfAny(AttributeRule base, String... haAttrKeys) {
    Set<String> keys = Set.of(haAttrKeys);
    return state -> {
      var a = state.attributes();
      for (var k : keys) if (a.containsKey(k)) return base.infer(state);
      return Optional.empty();
    };
  }

  public static AttributeRule presentIfAll(AttributeRule base, String... haAttrKeys) {
    Set<String> keys = Set.of(haAttrKeys);
    return state -> {
      var a = state.attributes();
      for (var k : keys) if (!a.containsKey(k)) return Optional.empty();
      return base.infer(state);
    };
  }

  public static AttributeRule presentIf(AttributeRule base, Predicate<MapHAState> pred) {
    return state -> pred.test(state) ? base.infer(state) : Optional.empty();
  }

  public static AttributeRule booleanFlag(String canonicalName, String presenceKey) {
    return presentIfAny(
        state ->
            Optional.of(
                Map.entry(
                    canonicalName,
                    AttributeDesc.newBuilder()
                        .setKind(FieldKind.BOOLEAN)
                        .setOptional(Boolean.TRUE)
                        .build())),
        presenceKey);
  }

  public static AttributeRule percentPct(String canonicalName) {
    return numberWithFixedCaps(canonicalName, UNIT_PERCENT, 0, 100, 1);
  }

  public static AttributeRule colorTempKelvinFromMireds(
      String canonicalName, String minMiredKey, String maxMiredKey) {
    return presentIfAll(
        state -> {
          var a = state.attributes();
          var minMired = num(a.get(minMiredKey));
          var maxMired = num(a.get(maxMiredKey));

          if (minMired == null || maxMired == null) return Optional.empty();

          var minK = 1_000_000.0 / maxMired;
          var maxK = 1_000_000.0 / minMired;

          return Optional.of(
              Map.entry(
                  canonicalName,
                  AttributeDesc.newBuilder()
                      .setKind(FieldKind.NUMBER)
                      .setUnit(UNIT_KELVIN)
                      .setCaps(
                          CapabilityRange.newBuilder()
                              .setMin(minK)
                              .setMax(maxK)
                              .setStep(COLOR_TEMP_STEP_K))
                      .build()));
        },
        minMiredKey,
        maxMiredKey);
  }

  public static AttributeRule hueDegrees(String canonicalName) {
    return numberWithFixedCaps(canonicalName, DEGREE_SYMBOL, 0, 360, 1);
  }

  public static AttributeRule saturationPct(String canonicalName) {
    return percentPct(canonicalName);
  }

  public static Predicate<MapHAState> colorModeIncludes(String mode) {
    return state -> {
      var o = state.attributes().get(SUPPORTED_COLOR_MODES);
      if (!(o instanceof List<?> list)) return false;
      for (var it : list) if (mode.equalsIgnoreCase(String.valueOf(it))) return true;
      return false;
    };
  }

  private static List<String> asStringList(Object o) {
    if (o instanceof List<?> list) {
      var out = new ArrayList<String>();
      for (var it : list) if (it != null) out.add(it.toString());
      return out;
    }

    return List.of();
  }

  private static Double num(Object o) {
    if (o instanceof Number n) return n.doubleValue();
    try {
      return o != null ? Double.parseDouble(o.toString()) : null;
    } catch (Exception e) {
      return null;
    }
  }

  private static String str(Object o) {
    return o == null ? null : o.toString();
  }
}
