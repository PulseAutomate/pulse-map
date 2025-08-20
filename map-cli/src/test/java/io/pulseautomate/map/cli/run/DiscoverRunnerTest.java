package io.pulseautomate.map.cli.run;

import static io.pulseautomate.map.manifest.util.Names.Attr.HVAC_MODE;
import static org.assertj.core.api.Assertions.assertThat;

import io.pulseautomate.map.manifest.gen.model.LockFile;
import io.pulseautomate.map.manifest.gen.model.Manifest;
import io.pulseautomate.map.manifest.lock.LockPb;
import io.pulseautomate.map.manifest.serde.ManifestPb;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class DiscoverRunnerTest {

  @TempDir Path tmp;

  @Test
  void writes_manifest_and_lock_and_reuses_lock_on_second_run() throws Exception {
    // 1. Mock the input data from ha-client (as a Map)
    var attributes = new LinkedHashMap<String, Object>();
    attributes.put("hvac_modes", List.of("off", "heat", "auto"));
    attributes.put("min_temp", 5.0);
    attributes.put("max_temp", 30.0);
    attributes.put("target_temp_step", 0.5);
    attributes.put("temperature_unit", "°C");

    var state =
        Map.<String, Object>of("entity_id", "climate.living_room_trv", "attributes", attributes);

    var service =
        Map.<String, Object>of(
            "domain",
            "climate",
            "services",
            Map.of(
                "set_temperature",
                Map.of("fields", Map.of("temperature", Map.of("required", true)))));

    var provider =
        new DiscoverRunner.SnapshotProvider() {
          @Override
          public List<Map<String, Object>> fetchStates() {
            return List.of(state);
          }

          @Override
          public List<Map<String, Object>> fetchServices() {
            return List.of(service);
          }

          @Override
          public String haVersion() {
            return "2025.6";
          }
        };

    var runner =
        new DiscoverRunner(provider, new io.pulseautomate.map.manifest.builder.ManifestBuilder());

    // 2. First run: Discover and write files
    var r1 = runner.run(tmp, false); // false for binary .pb output
    assertThat(Files.exists(r1.manifestPath())).isTrue();
    assertThat(Files.exists(r1.lockPath())).isTrue();

    // 3. Verify the contents of the first run's files
    Manifest m1 = ManifestPb.read(r1.manifestPath());
    assertThat(m1.getEntitiesCount()).isEqualTo(1);
    assertThat(m1.getEntities(0).getAttributesMap()).containsKey(HVAC_MODE);

    LockFile lf1 = LockPb.read(r1.lockPath());
    String stable1 = lf1.getEntityMapMap().get("climate.living_room_trv");
    assertThat(stable1).startsWith("stable:");

    // 4. Second run: Should reuse the stable_id from the first run's lock file
    var r2 = runner.run(tmp, false);
    LockFile lf2 = LockPb.read(r2.lockPath());
    String stable2 = lf2.getEntityMapMap().get("climate.living_room_trv");

    // 5. Assert that the stable ID was correctly preserved
    assertThat(stable2).isEqualTo(stable1);
  }
}
