package io.pulseautomate.map.ha.config;

import java.net.URI;
import java.time.Duration;

public record HAConfig(URI baseUrl, String token, Duration requestTimeout, int maxRetries) {
  public static HAConfig of(URI baseUrl, String token) {
    return new HAConfig(baseUrl, token, Duration.ofSeconds(15), 2);
  }
}
