package io.pulseautomate.map.manifest.builder;

import static io.pulseautomate.map.manifest.util.Constants.*;
import static io.pulseautomate.map.manifest.util.Names.Domain.*;
import static io.pulseautomate.map.manifest.util.Names.SvcField.*;
import static io.pulseautomate.map.manifest.util.Names.SvcField.KELVIN;

import io.pulseautomate.map.manifest.gen.model.ServiceField;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class ServiceTyping {
  private ServiceTyping() {}

  static Map<String, ServiceField> apply(
      String domain, String serviceName, Map<String, ServiceField> in) {
    if (in == null || in.isEmpty()) return in;
    var out = new LinkedHashMap<String, ServiceField>(in.size());
    in.forEach((name, f) -> out.put(name, typed(domain, serviceName, name, f)));
    return out;
  }

  private static ServiceField typed(String domain, String service, String name, ServiceField f) {
    var type = f.getType();
    var unit = f.getUnit();

    // --- Climate ---
    if (CLIMATE.equals(domain)) {
      if ("set_temperature".equals(service) && TEMPERATURE.equals(name)) {
        type = nullable(type, "number");
        unit = nullable(unit, UNIT_CELSIUS_WITH_SYMBOL);
      } else if ("set_hvac_mode".equals(service) && HVAC_MODE.equals(name)) {
        type = nullable(type, "enum");
      } else if ("set_preset_mode".equals(service) && PRESET_MODE.equals(name)) {
        type = nullable(type, "enum");
      }
    }

    // --- Light ---
    else if (LIGHT.equals(domain) && "turn_on".equals(service)) {
      if (BRIGHTNESS_PCT.equals(name)) {
        type = nullable(type, "percent");
        unit = nullable(unit, UNIT_PERCENT);
      } else if (COLOR_TEMP.equals(name) || COLOR_TEMP_KELVIN.equals(name) || KELVIN.equals(name)) {
        if (COLOR_TEMP_KELVIN.equals(name) || KELVIN.equals(name)) {
          type = nullable(type, "number");
          unit = nullable(unit, UNIT_KELVIN);
        } else {
          type = nullable(type, "mireds");
          unit = nullable(unit, "mired");
        }
      } else if (TRANSITION.equals(name)) {
        type = nullable(type, "duration_s");
        unit = nullable(unit, "s");
      } else if (EFFECT.equals(name)) {
        type = nullable(type, "enum");
      }
    }

    // --- Fan ---
    else if (FAN.equals(domain)) {
      if (("set_percentage".equals(service) || "set_speed".equals(service))
          && PERCENTAGE.equals(name)) {
        type = nullable(type, "percent");
        unit = nullable(unit, UNIT_PERCENT);
      } else if ("set_direction".equals(service) && DIRECTION.equals(name)) {
        type = nullable(type, "enum");
      } else if (("set_preset_mode".equals(service) || "set_preset".equals(service))
          && PRESET_MODE.equals(name)) {
        type = nullable(type, "enum");
      }
    }

    // --- Cover ---
    else if (COVER.equals(domain)) {
      if ("set_cover_position".equals(service) && POSITION.equals(name)) {
        type = nullable(type, "percent");
        unit = nullable(unit, UNIT_PERCENT);
      } else if ("set_cover_tilt_position".equals(service) && TILT_POSITION.equals(name)) {
        type = nullable(type, "percent");
        unit = nullable(unit, UNIT_PERCENT);
      }
    }

    // --- Media Player ---
    else if (MEDIA_PLAYER.equals(domain)) {
      if ("volume_set".equals(service) && VOLUME_LEVEL.equals(name)) {
        type = nullable(type, "percent");
        unit = nullable(unit, UNIT_PERCENT);
      } else if ("select_source".equals(service) && SOURCE.equals(name)) {
        type = nullable(type, "enum");
      } else if ("select_sound_mode".equals(service) && SOUND_MODE.equals(name)) {
        type = nullable(type, "enum");
      }
    }

    // --- NUMBER (generic) ---
    else if (NUMBER.equals(domain) && "ser_value".equals(service) && VALUE.equals(name)) {
      type = nullable(type, "number");
    }

    if (Objects.equals(type, f.getType()) && Objects.equals(unit, f.getUnit())) return f;
    return ServiceField.newBuilder()
        .setType(type)
        .setUnit(unit)
        .setRequired(f.getRequired())
        .build();
  }

  private static String nullable(String current, String candidate) {
    return (current == null || current.isBlank()) ? candidate : current;
  }
}
