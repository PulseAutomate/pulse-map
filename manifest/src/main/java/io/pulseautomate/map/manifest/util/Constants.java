package io.pulseautomate.map.manifest.util;

public final class Constants {
  private Constants() {}

  // === Stable IDs ===
  public static final String STABLE_PREFIX = "stable:";
  public static final int STABLE_ID_HEX_LENGTH = 12;

  // === Service signature formatting ===
  /** Used between name and field list */
  public static final char SIG_MAIN_SEP = '|';

  /** Used between fields */
  public static final char SIG_FIELD_SEP = ',';

  /** Used between parts of a field */
  public static final char SIG_PART_SEP = ':';

  public static final String SIG_REQ_FLAG = ":req";

  // === Schema versions ===
  public static final int MANIFEST_SCHEMA_V1 = 1;
  public static final int LOCK_SCHEMA_V1 = 1;

  public static final String DEGREE_SYMBOL = "°";

  public static final String CELSIUS = "celsius";
  public static final String UNIT_CELSIUS = "C";
  public static final String UNIT_CELSIUS_WITH_SYMBOL = DEGREE_SYMBOL + UNIT_CELSIUS;
  public static final String FAHRENHEIT = "fahrenheit";
  public static final String UNIT_FAHRENHEIT = "F";
  public static final String UNIT_FAHRENHEIT_WITH_SYMBOL = DEGREE_SYMBOL + UNIT_FAHRENHEIT;

  public static final String KELVIN = "kelvin";
  public static final String UNIT_KELVIN = "K";
  public static final String UNIT_KELVIN_WITH_SYMBOL = DEGREE_SYMBOL + UNIT_KELVIN;
  public static final double COLOR_TEMP_STEP_K = 50.0;

  public static final String UNIT_PERCENT = "%";
}
