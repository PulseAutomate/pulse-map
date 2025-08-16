package io.pulseautomate.map.ha.client;

import static org.assertj.core.api.Assertions.*;

import io.pulseautomate.map.ha.config.HAConfig;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.Test;

class HAHttpClientTest {

  private String readResource(String name) throws Exception {
    var in = getClass().getClassLoader().getResourceAsStream(name);
    assertThat(in).as("resource %s must exist on classpath", name).isNotNull();
    try (in) {
      return new String(in.readAllBytes(), StandardCharsets.UTF_8);
    }
  }

  @Test
  void fetchStates_ok() throws Exception {
    try (MockWebServer server = new MockWebServer()) {
      server.enqueue(
          new MockResponse().setResponseCode(200).setBody(readResource("fixtures/states.json")));
      server.start();
      var cfg =
          new HAConfig(
              URI.create(server.url("/").toString()), "TOKEN", java.time.Duration.ofSeconds(2), 0);
      var client = new HAHttpClient(cfg);
      var states = client.fetchStates();
      assertThat(states).hasSize(1);
      assertThat(states.getFirst().entityId()).isEqualTo("light.living_room");
      assertThat(states.getFirst().attributes()).containsEntry("brightness", 180);
    }
  }

  @Test
  void fetchServices_ok() throws Exception {
    try (MockWebServer server = new MockWebServer()) {
      server.enqueue(
          new MockResponse().setResponseCode(200).setBody(readResource("fixtures/services.json")));
      server.start();
      var cfg =
          new HAConfig(
              URI.create(server.url("/").toString()), "TOKEN", java.time.Duration.ofSeconds(2), 0);
      var client = new HAHttpClient(cfg);
      var services = client.fetchServices();
      assertThat(services).hasSize(1);
      var s = services.getFirst();
      assertThat(s.domain()).isEqualTo("light");
      assertThat(s.service()).isEqualTo("turn_on");
      assertThat(s.fields()).containsKey("brightness_pct");
      assertThat(s.fields().get("brightness_pct").required()).isFalse();
    }
  }

  @Test
  void retriesOn500_thenSucceeds() throws Exception {
    try (MockWebServer server = new MockWebServer()) {
      server.enqueue(new MockResponse().setResponseCode(500).setBody("oops"));
      server.enqueue(
          new MockResponse().setResponseCode(200).setBody(readResource("fixtures/states.json")));
      server.start();
      var cfg =
          new HAConfig(
              URI.create(server.url("/").toString()), "TOKEN", java.time.Duration.ofMillis(800), 1);
      var client = new HAHttpClient(cfg);
      var states = client.fetchStates();
      assertThat(states).hasSize(1);
    }
  }

  @Test
  void authFailure401_noRetry() throws Exception {
    try (MockWebServer server = new MockWebServer()) {
      server.enqueue(new MockResponse().setResponseCode(401).setBody("unauthorized"));
      server.start();
      var cfg =
          new HAConfig(
              URI.create(server.url("/").toString()), "BAD", java.time.Duration.ofSeconds(1), 2);
      var client = new HAHttpClient(cfg);
      assertThatThrownBy(client::fetchStates)
          .isInstanceOf(HAHttpException.class)
          .hasMessageContaining("HTTP 401");
    }
  }
}
