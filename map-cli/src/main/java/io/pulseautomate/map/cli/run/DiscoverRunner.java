package io.pulseautomate.map.cli.run;

import com.google.protobuf.util.JsonFormat;
import io.pulseautomate.map.ha.client.HAHttpClient;
import io.pulseautomate.map.ha.config.HAConfig;
import io.pulseautomate.map.manifest.builder.ManifestBuilder;
import io.pulseautomate.map.manifest.lock.LockBuilder;
import io.pulseautomate.map.manifest.lock.LockPb;
import io.pulseautomate.map.manifest.serde.ManifestCanonicalizer;
import io.pulseautomate.map.manifest.serde.ManifestPb;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;

public final class DiscoverRunner {

  public interface Factory {
    DiscoverRunner create(URI haUrl, String token, String haVersionOpt);
  }

  public static DiscoverRunner forHomeAssistant(URI haUrl, String token, String haVersionOpt) {
    Objects.requireNonNull(haUrl);
    Objects.requireNonNull(token);

    SnapshotProvider provider = new HAProvider(haUrl, token, haVersionOpt);
    return new DiscoverRunner(provider, new ManifestBuilder());
  }

  public static DiscoverRunner forSnapshotOnly(String haVersionOpt) {
    SnapshotProvider provider =
        new SnapshotProvider() {
          @Override
          public List<Map<String, Object>> fetchStates() throws Exception {
            return Collections.emptyList();
          }

          @Override
          public List<Map<String, Object>> fetchServices() throws Exception {
            return Collections.emptyList();
          }

          @Override
          public String haVersion() throws Exception {
            return (haVersionOpt != null && !haVersionOpt.isBlank()) ? haVersionOpt : "unknown";
          }
        };

    return new DiscoverRunner(provider, new ManifestBuilder());
  }

  public static DiscoverRunner forDemo(String haVersionOpt) {
    var provider = new DemoProvider(haVersionOpt);
    return new DiscoverRunner(provider, new ManifestBuilder());
  }

  public record Result(Path manifestPath, Path lockPath, int entities, int services) {}

  public interface SnapshotProvider {
    List<Map<String, Object>> fetchStates() throws Exception;

    List<Map<String, Object>> fetchServices() throws Exception;

    String haVersion() throws Exception;
  }

  private final SnapshotProvider provider;
  private final ManifestBuilder builder;

  public DiscoverRunner(SnapshotProvider provider, ManifestBuilder builder) {
    this.provider = provider;
    this.builder = builder;
  }

  public Result run(Path outDir, boolean jsonOutput) throws Exception {
    Files.createDirectories(outDir);
    cleanupOldFiles(outDir);

    var states = provider.fetchStates();
    var services = provider.fetchServices();
    var haVersion = provider.haVersion();

    var manifest = builder.build(haVersion, states, services);
    var canon = ManifestCanonicalizer.canonicalize(manifest);
    var lock = LockBuilder.build(canon, null, Instant.now());

    var manifestExt = jsonOutput ? ".json" : ".pb";
    var lockExt = jsonOutput ? ".json" : ".pb";

    var manifestPath = outDir.resolve("manifest" + manifestExt);
    var lockPath = outDir.resolve("map.lock" + lockExt);

    if (jsonOutput) {
      var printer =
          JsonFormat.printer()
              .preservingProtoFieldNames()
              .sortingMapKeys()
              .alwaysPrintFieldsWithNoPresence();
      Files.writeString(manifestPath, printer.print(canon));
      Files.writeString(lockPath, printer.print(lock));
    } else {
      ManifestPb.write(manifestPath, canon);
      LockPb.write(lockPath, lock);
    }

    return new Result(manifestPath, lockPath, canon.getEntitiesCount(), canon.getServicesCount());
  }

  private void cleanupOldFiles(Path outDir) throws IOException {
    Files.deleteIfExists(outDir.resolve("manifest.pb"));
    Files.deleteIfExists(outDir.resolve("manifest.json"));
    Files.deleteIfExists(outDir.resolve("map.lock.pb"));
    Files.deleteIfExists(outDir.resolve("map.lock.json"));
  }

  private static final class HAProvider implements SnapshotProvider {
    private final URI url;
    private final String token;
    private final String haVersionOpt;

    public HAProvider(URI url, String token, String haVersionOpt) {
      this.url = url;
      this.token = token;
      this.haVersionOpt = haVersionOpt;
    }

    @Override
    public List<Map<String, Object>> fetchStates() throws Exception {
      try (var client = new HAHttpClient(HAConfig.of(url, token))) {
        return client.fetchStates();
      }
    }

    @Override
    public List<Map<String, Object>> fetchServices() throws Exception {
      try (var client = new HAHttpClient(HAConfig.of(url, token))) {
        return client.fetchServices();
      }
    }

    @Override
    public String haVersion() throws Exception {
      return (haVersionOpt != null && !haVersionOpt.isBlank()) ? haVersionOpt : "unknown";
    }
  }

  private static final class DemoProvider implements SnapshotProvider {
    private final String versionOpt;

    DemoProvider(String versionOpt) {
      this.versionOpt = versionOpt;
    }

    @Override
    public List<Map<String, Object>> fetchStates() {
      return List.of(
          // Example climate entity as a map
          Map.of(
              "entity_id",
              "climate.living_room_trv",
              "attributes",
              Map.of(
                  "hvac_modes",
                  List.of("off", "heat", "auto"),
                  "min_temp",
                  5.0,
                  "max_temp",
                  30.0,
                  "temperature_unit",
                  "°C")));
    }

    @Override
    public List<Map<String, Object>> fetchServices() {
      return List.of(
          // Example service as a map
          Map.of(
              "domain",
              "climate",
              "services",
              Map.of(
                  "set_temperature",
                  Map.of("fields", Map.of("temperature", Map.of("required", true))))));
    }

    @Override
    public String haVersion() {
      return versionOpt != null ? versionOpt : "demo";
    }
  }
}
