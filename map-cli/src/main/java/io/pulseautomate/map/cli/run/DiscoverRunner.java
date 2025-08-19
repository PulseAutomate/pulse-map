package io.pulseautomate.map.cli.run;

import io.pulseautomate.map.ha.client.HAHttpClient;
import io.pulseautomate.map.ha.config.HAConfig;
import io.pulseautomate.map.ha.model.HAService;
import io.pulseautomate.map.ha.model.HAServiceField;
import io.pulseautomate.map.ha.model.HASnapshot;
import io.pulseautomate.map.ha.model.HAState;
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

  public static DiscoverRunner forDemo(String haVersionOpt) {
    var provider = new DemoProvider(haVersionOpt);
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

  private static final class DemoProvider implements SnapshotProvider {
    private final String versionOpt;

    DemoProvider(String versionOpt) {
      this.versionOpt = versionOpt;
    }

    @Override
    public HASnapshot fetch() {
      List<HAState> states = new ArrayList<>();

      // climate
      var aClimate = new LinkedHashMap<String, Object>();
      aClimate.put("device_class", "heater");
      aClimate.put("hvac_modes", List.of("off", "heat", "auto"));
      aClimate.put("preset_modes", List.of("eco", "comfort"));
      aClimate.put("min_temp", 5.0);
      aClimate.put("max_temp", 30.0);
      aClimate.put("target_temp_step", 0.5);
      aClimate.put("temperature_unit", "°C");
      states.add(new HAState("climate.living_room_trv", "heat", aClimate, null, null));

      // light
      var aLight = new LinkedHashMap<String, Object>();
      aLight.put("supported_color_modes", List.of("hs", "color_temp"));
      aLight.put("brightness", 180);
      aLight.put("min_mireds", 153);
      aLight.put("max_mireds", 500);
      aLight.put("effect_list", List.of("rainbow", "night"));
      states.add(new HAState("light.lamp", "on", aLight, null, null));

      // fan
      var aFan = new LinkedHashMap<String, Object>();
      aFan.put("percentage", 33);
      aFan.put("preset_modes", List.of("auto", "boost"));
      aFan.put("direction", "forward");
      aFan.put("oscillating", true);
      states.add(new HAState("fan.tower", "on", aFan, null, null));

      // cover
      var aCover = new LinkedHashMap<String, Object>();
      aCover.put("current_position", 42);
      aCover.put("current_tilt_position", 10);
      states.add(new HAState("cover.blind", "open", aCover, null, null));

      // media_player
      var aTv = new LinkedHashMap<String, Object>();
      aTv.put("volume_level", 0.35);
      aTv.put("source_list", List.of("TV", "HDMI1"));
      aTv.put("sound_mode_list", List.of("stereo", "movie"));
      states.add(new HAState("media_player.lg_tv", "on", aTv, null, null));

      // Services (minimal example)
      Map<String, HAServiceField> climateSetTemp =
          Map.of("temperature", new HAServiceField(true, null, null, "Set temperature"));
      List<HAService> services =
          List.of(new HAService("climate", "set_temperature", climateSetTemp));

      return new HASnapshot(states, services);
    }

    @Override
    public String haVersion() {
      return versionOpt != null ? versionOpt : "demo";
    }
  }
}
