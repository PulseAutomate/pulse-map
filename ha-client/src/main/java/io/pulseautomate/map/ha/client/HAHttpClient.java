package io.pulseautomate.map.ha.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.pulseautomate.map.ha.config.HAConfig;
import io.pulseautomate.map.ha.model.HAService;
import io.pulseautomate.map.ha.model.HAServiceField;
import io.pulseautomate.map.ha.model.HAState;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public final class HAHttpClient implements HAClient {
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private final HAConfig cfg;
    private final HttpClient http;

    public HAHttpClient(HAConfig cfg) {
        this.cfg = cfg;
        this.http = HttpClient.newBuilder()
                .connectTimeout(cfg.requestTimeout())
                .version(HttpClient.Version.HTTP_1_1)
                .build();
    }

    @Override
    public List<HAState> fetchStates() throws HAHttpException {
        return getJson("/api/states", new TypeReference<>() {});
    }

    @Override
    public List<HAService> fetchServices() throws HAHttpException {
        var raw = getJson("/api/services", new TypeReference<List<Map<String, Object>>>() {});
        List<HAService> out = new ArrayList<>();

        for (var item : raw) {
            var domain = Objects.toString(item.get("domain"), null);
            @SuppressWarnings("unchecked")
            var services = (Map<String, Object>) item.get("services");

            if (domain == null || services == null) continue;

            for (Map.Entry<String, Object> e : services.entrySet()) {
                var service = e.getKey();
                @SuppressWarnings("unchecked")
                Map<String, Object> svc = (Map<String, Object>) e.getValue();
                @SuppressWarnings("unchecked")
                Map<String, Map<String, Object>> fieldsRaw = (Map<String, Map<String, Object>>) svc.get("fields");

                Map<String, HAServiceField> fields = new LinkedHashMap<>();
                if (fieldsRaw != null) {
                    for (var fe : fieldsRaw.entrySet()) {
                        var node = fe.getValue();
                        HAServiceField f = MAPPER.convertValue(node, HAServiceField.class);
                        fields.put(fe.getKey(), f);
                    }
                }

                out.add(new HAService(domain, service, fields));
            }
        }
        return out;
    }

    private <T> T getJson(String path, TypeReference<T> type) throws HAHttpException {
        final var uri = resolve(path);
        var attempts = 0;
        HAHttpException last = null;

        while (attempts <= cfg.maxRetries()) {
            attempts++;
            try {
                var req = HttpRequest.newBuilder(uri)
                        .timeout(cfg.requestTimeout())
                        .header("Authorization", "Bearer " + cfg.token())
                        .header("Accept", "application/json")
                        .GET()
                        .build();

                var res = http.send(req, HttpResponse.BodyHandlers.ofString());
                var code = res.statusCode();
                var body = res.body();

                if (code >= 200 && code < 300)
                    return MAPPER.readValue(body, type);

                if (isTransient(code))
                    last = new HAHttpException("Transient HTTP " + code, code, uri.toString(), body);
                else
                    throw new HAHttpException("HTTP " + code + " from " + uri, code, uri.toString(), body);
            } catch (IOException | InterruptedException ioe) {
                last = new HAHttpException("I/O error calling " + uri, 0, uri.toString(), null, ioe);
            }

            if (attempts <= cfg.maxRetries())
                sleepBackoff(attempts);
        }

        throw last != null
                ? last
                : new HAHttpException("Unknown error", -1, uri.toString(), null);
    }

    private URI resolve(String path) {
        var base = cfg.baseUrl().toString();
        if (base.endsWith("/") && path.startsWith("/"))
            path = path.substring(1);
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
        } catch (InterruptedException ignored) {}
    }
}
