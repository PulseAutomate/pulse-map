package io.pulseautomate.map.manifest.util;

public final class Temperature {
  private Temperature() {}

  public static double fToC(double f) {
    return (f - 32) * (5.0 / 9.0);
  }

  public static double cToF(double c) {
    return (c * (9.0 / 5.0)) + 32;
  }
}
