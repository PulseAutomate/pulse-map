package io.pulseautomate.map.cli.run;

import io.pulseautomate.map.ha.client.HAHttpClient;
import io.pulseautomate.map.ha.config.HAConfig;
import io.pulseautomate.map.ha.model.HASnapshot;
import io.pulseautomate.map.manifest.builder.ManifestBuilder;
import io.pulseautomate.map.manifest.lock.LockBuilder;
import io.pulseautomate.map.manifest.lock.LockFile;
import io.pulseautomate.map.manifest.lock.LockJson;
import io.pulseautomate.map.manifest.serde.ManifestCanonicalizer;
import io.pulseautomate.map.manifest.serde.ManifestJson;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

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
          public HASnapshot fetch() throws Exception {
            return new HASnapshot(List.of(), List.of());
          }

          @Override
          public String haVersion() throws Exception {
            return (haVersionOpt != null && !haVersionOpt.isBlank()) ? haVersionOpt : "unknown";
          }
        };

    return new DiscoverRunner(provider, new ManifestBuilder());
  }

  public record Result(Path manifestPath, Path lockPath, int entities, int services) {}

  public interface SnapshotProvider {
    HASnapshot fetch() throws Exception;

    String haVersion() throws Exception;
  }

  private final SnapshotProvider provider;
  private final ManifestBuilder builder;

  public DiscoverRunner(SnapshotProvider provider, ManifestBuilder builder) {
    this.provider = provider;
    this.builder = builder;
  }

  public Result run(Path outDir) throws Exception {
    Files.createDirectories(outDir);

    var snap = provider.fetch();
    var version = provider.haVersion();

    var manifest = builder.build(version, snap);
    var canon = ManifestCanonicalizer.canonicalize(manifest);

    var manifestPath = outDir.resolve("manifest.json");
    ManifestJson.writePretty(manifestPath, canon);

    var lockPath = outDir.resolve("map.lock.json");
    LockFile prev = null;
    if (Files.exists(lockPath))
      try {
        prev = LockJson.read(lockPath);
      } catch (IOException ignore) {
      }

    var lock = LockBuilder.build(canon, prev, Instant.now());
    LockJson.writePretty(lockPath, lock);

    var entities = canon.entities() == null ? 0 : canon.entities().size();
    var services = canon.services() == null ? 0 : canon.services().size();
    return new Result(manifestPath, lockPath, entities, services);
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
    public HASnapshot fetch() throws Exception {
      var client = new HAHttpClient(HAConfig.of(url, token));
      var states = client.fetchStates();
      var services = client.fetchServices();
      return new HASnapshot(states, services);
    }

    @Override
    public String haVersion() throws Exception {
      return (haVersionOpt != null && !haVersionOpt.isBlank()) ? haVersionOpt : "unknown";
    }
  }
}
