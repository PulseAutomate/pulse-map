package io.pulseautomate.map.cli.commands;

import io.pulseautomate.map.manifest.lock.LockBuilder;
import io.pulseautomate.map.manifest.lock.LockJson;
import io.pulseautomate.map.manifest.serde.ManifestCanonicalizer;
import io.pulseautomate.map.manifest.serde.ManifestJson;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.concurrent.Callable;
import picocli.CommandLine;

@CommandLine.Command(
    name = "validate",
    mixinStandardHelpOptions = true,
    description = "Validate manifest.json against map.lock.json (hash and coverage).")
public final class ValidateCommand implements Callable<Integer> {
  @CommandLine.Option(
      names = {"--dir"},
      description = "Directory containing the manifest.json and map.lock.json files.",
      paramLabel = "<dir>",
      defaultValue = ".")
  Path dir;

  @CommandLine.Option(
      names = {"--strict"},
      description = "Exit with non-zero status if any validation issue is found")
  boolean strict;

  @Override
  public Integer call() throws Exception {
    var manifestPath = dir.resolve("manifest.json");
    var lockPath = dir.resolve("map.lock.json");

    if (!Files.exists(manifestPath)) {
      System.err.println("manifest.json not found: " + manifestPath.toAbsolutePath());
      return 2;
    }
    if (!Files.exists(lockPath)) {
      System.err.println("map.lock.json not found: " + lockPath.toAbsolutePath());
      return 2;
    }

    var manifest = ManifestJson.read(manifestPath);
    var canon = ManifestCanonicalizer.canonicalize(manifest);
    var lock = LockJson.read(lockPath);

    var recomputed = LockBuilder.build(canon, null, Instant.EPOCH).manifest_hash();
    var hashOk = recomputed.equals(lock.manifest_hash());

    var missingStable = 0;
    if (canon.entities() != null) {
      for (var e : canon.entities()) {
        var stable = lock.entity_map() != null ? lock.entity_map().get(e.entity_id()) : null;
        if (stable == null || stable.isBlank()) {
          System.err.println("[MISSING] stable_id for " + e.entity_id());
          missingStable++;
        }
      }
    }

    var missingServiceSig = 0;
    if (canon.services() != null) {
      for (var s : canon.services()) {
        var key = s.domain() + "." + s.service();
        var sig = lock.service_sig() != null ? lock.service_sig().get(key) : null;
        if (sig == null || sig.isBlank()) {
          System.err.println("[MISSING] service signature for " + key);
          missingServiceSig++;
        }
      }
    }

    System.out.println("Hash match: " + (hashOk ? "OK" : "FAIL"));
    System.out.println("Entities without stable_id: " + missingStable);
    System.out.println("Services without signature: " + missingServiceSig);

    var ok = hashOk && missingStable == 0 && missingServiceSig == 0;
    if (!ok && strict) return 1;
    return 0;
  }
}
