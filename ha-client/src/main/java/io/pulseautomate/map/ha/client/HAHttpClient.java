package io.pulseautomate.map.ha.client;

import io.pulseautomate.map.ha.config.HAConfig;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import org.json.JSONArray;

public final class HAHttpClient implements HAClient {

  private final HAConfig cfg;
  private final HttpClient http;

  public HAHttpClient(HAConfig cfg) {
    this.cfg = cfg;
    this.http =
        HttpClient.newBuilder()
            .connectTimeout(cfg.requestTimeout())
            .version(HttpClient.Version.HTTP_1_1)
            .build();
  }

  @Override
  public List<Map<String, Object>> fetchStates() throws HAHttpException {
    var jsonBody = getJsonString("/api/states");
    var jsonArray = new JSONArray(jsonBody);
    return jsonArray.toList().stream().map(item -> (Map<String, Object>) item).toList();
  }

  @Override
  public List<Map<String, Object>> fetchServices() throws HAHttpException {
    var jsonBody = getJsonString("/api/services");
    var jsonArray = new JSONArray(jsonBody);
    return jsonArray.toList().stream().map(item -> (Map<String, Object>) item).toList();
  }

  private String getJsonString(String path) throws HAHttpException {
    final var uri = resolve(path);
    var attempts = 0;
    HAHttpException last = null;

    while (attempts <= cfg.maxRetries()) {
      attempts++;
      try {
        var req =
            HttpRequest.newBuilder(uri)
                .timeout(cfg.requestTimeout())
                .header("Authorization", "Bearer " + cfg.token())
                .header("Accept", "application/json")
                .GET()
                .build();

        var res = http.send(req, HttpResponse.BodyHandlers.ofString());
        var code = res.statusCode();
        var body = res.body();

        if (code >= 200 && code < 300) return body;

        if (isTransient(code))
          last = new HAHttpException("Transient HTTP " + code, code, uri.toString(), body);
        else throw new HAHttpException("HTTP " + code + " from " + uri, code, uri.toString(), body);
      } catch (IOException | InterruptedException ioe) {
        last = new HAHttpException("I/O error calling " + uri, 0, uri.toString(), null, ioe);
      }

      if (attempts <= cfg.maxRetries()) sleepBackoff(attempts);
    }

    throw last != null ? last : new HAHttpException("Unknown error", -1, uri.toString(), null);
  }

  private URI resolve(String path) {
    var base = cfg.baseUrl().toString();
    if (base.endsWith("/") && path.startsWith("/")) path = path.substring(1);
    return URI.create(base + path);
  }

  private static boolean isTransient(int status) {
    return status == 408 || status == 429 || status >= 500;
  }

  private static void sleepBackoff(int attempt) {
    var base = Math.min(1000L & (1L << Math.min(5, attempt - 1)), 8000L);
    var jitter = ThreadLocalRandom.current().nextLong(0, 250);
    try {
      Thread.sleep(base + jitter);
    } catch (InterruptedException ignored) {
    }
  }
}
