package io.pulseautomate.map.cli.run;

import static io.pulseautomate.map.manifest.util.Names.Attr.HVAC_MODE;
import static org.assertj.core.api.Assertions.assertThat;

import io.pulseautomate.map.ha.model.HAService;
import io.pulseautomate.map.ha.model.HAServiceField;
import io.pulseautomate.map.ha.model.HASnapshot;
import io.pulseautomate.map.ha.model.HAState;
import io.pulseautomate.map.manifest.lock.LockFile;
import io.pulseautomate.map.manifest.lock.LockJson;
import io.pulseautomate.map.manifest.model.Manifest;
import io.pulseautomate.map.manifest.serde.ManifestJson;
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
    var a = new LinkedHashMap<String, Object>();
    a.put("hvac_modes", List.of("off", "heat", "auto"));
    a.put("min_temp", 5.0);
    a.put("max_temp", 30.0);
    a.put("target_temp_step", 0.5);
    a.put("temperature_unit", "°C");
    var state = new HAState("climate.living_room_trv", "heat", a, null, null);

    var fields = Map.of("temperature", new HAServiceField(true, null, null, "Set temp"));
    var svc = new HAService("climate", "set_temperature", fields);

    var provider =
        new DiscoverRunner.SnapshotProvider() {
          @Override
          public HASnapshot fetch() {
            return new HASnapshot(List.of(state), List.of(svc));
          }

          @Override
          public String haVersion() {
            return "2025.6";
          }
        };

    var runner =
        new DiscoverRunner(provider, new io.pulseautomate.map.manifest.builder.ManifestBuilder());

    var r1 = runner.run(tmp);
    assertThat(Files.exists(r1.manifestPath())).isTrue();
    assertThat(Files.exists(r1.lockPath())).isTrue();

    Manifest m1 = ManifestJson.read(r1.manifestPath());
    assertThat(m1.entities()).hasSize(1);
    assertThat(m1.entities().getFirst().attributes()).containsKey(HVAC_MODE);

    LockFile lf1 = LockJson.read(r1.lockPath());
    String stable1 = lf1.entity_map().get("climate.living_room_trv");
    assertThat(stable1).startsWith("stable:");

    var r2 = runner.run(tmp);
    LockFile lf2 = LockJson.read(r2.lockPath());
    String stable2 = lf2.entity_map().get("climate.living_room_trv");
    assertThat(stable2).isEqualTo(stable1);
  }
}
