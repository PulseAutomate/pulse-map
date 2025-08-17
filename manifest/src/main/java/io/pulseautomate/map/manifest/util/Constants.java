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
}
