package io.pulseautomate.map.manifest.util;

public final class Names {
  private Names() {}

  public static final class Domain {
    public static final String CLIMATE = "climate";
    public static final String LIGHT = "light";
    public static final String FAN = "fan";
    public static final String COVER = "cover";
    public static final String MEDIA_PLAYER = "media_player";
  }

  public static final class Attr {
    public static final String HVAC_MODE = "hvac_mode";
    public static final String PRESET_MODE = "preset_mode";
    public static final String CURRENT_TEMP_C = "current_temp_c";
    public static final String TARGET_TEMP_C = "target_temp_c";

    public static final String BRIGHTNESS_PCT = "brightness_pct";
    public static final String EFFECT = "effect";
    public static final String COLOR_TEMP_K = "color_temp_k";
    public static final String HUE_DEG = "hue_deg";
    public static final String SATURATION_PCT = "saturation_pct";

    public static final String SPEED_PCT = "speed_pct";
    public static final String DIRECTION = "direction";
    public static final String OSCILLATING = "oscillating";

    public static final String POSITION_PCT = "position_pct";
    public static final String TILT_POSITION_PCT = "tilt_position_pct";

    public static final String VOLUME_PCT = "volume_pct";
    public static final String SOURCE = "source";
    public static final String SOUND_MODE = "sound_mode";
  }

  public static final class HaAttr {
    public static final String HVAC_MODES = "hvac_modes";
    public static final String HVAC_MODE = "hvac_mode";
    public static final String PRESET_MODES = "preset_modes";
    public static final String MIN_TEMP = "min_temp";
    public static final String MAX_TEMP = "max_temp";
    public static final String TARGET_TEMP_STEP = "target_temp_step";
    public static final String TEMPERATURE_UNIT = "temperature_unit";

    public static final String BRIGHTNESS = "brightness";
    public static final String SUPPORTED_COLOR_MODES = "supported_color_modes";
    public static final String EFFECT_LIST = "effect_list";
    public static final String MIN_MIREDS = "min_mireds";
    public static final String MAX_MIREDS = "max_mireds";

    public static final String PERCENTAGE = "percentage";
    public static final String SPEED_LIST = "speed_list";
    public static final String DIRECTION = "direction";
    public static final String DIRECTION_LIST = "direction_list";
    public static final String OSCILLATING = "oscillating";

    public static final String CURRENT_POSITION = "current_position";
    public static final String POSITION = "position";
    public static final String CURRENT_TILT_POSITION = "current_tilt_position";
    public static final String TILT_POSITION = "tilt_position";

    public static final String VOLUME_LEVEL = "volume_level";
    public static final String SOURCE = "source";
    public static final String SOURCE_LIST = "source_list";
    public static final String SOUND_MODE = "sound_mode";
    public static final String SOUND_MODE_LIST = "sound_mode_list";
  }

  public static final class ColorMode {
    public static final String HS = "hs";
    public static final String COLOR_TEMP = "color_temp";
  }
}
