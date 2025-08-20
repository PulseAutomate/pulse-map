package io.pulseautomate.map.cli.commands;

import io.pulseautomate.map.manifest.lock.LockBuilder;
import io.pulseautomate.map.manifest.lock.LockPb;
import io.pulseautomate.map.manifest.serde.ManifestCanonicalizer;
import io.pulseautomate.map.manifest.serde.ManifestPb;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.concurrent.Callable;
import picocli.CommandLine;

@CommandLine.Command(
    name = "validate",
    mixinStandardHelpOptions = true,
    description = "Validate manifest.pb against map.lock.pb (hash and coverage).")
public final class ValidateCommand implements Callable<Integer> {
  @CommandLine.Option(
      names = {"--dir"},
      description = "Directory containing the manifest.pb and map.lock.pb files.",
      paramLabel = "<dir>",
      defaultValue = ".")
  Path dir;

  @CommandLine.Option(
      names = {"--strict"},
      description = "Exit with non-zero status if any validation issue is found")
  boolean strict;

  @Override
  public Integer call() throws Exception {
    var manifestPath = dir.resolve("manifest.pb");
    var lockPath = dir.resolve("map.lock.pb");

    if (!Files.exists(manifestPath)) {
      System.err.println("manifest.pb not found: " + manifestPath.toAbsolutePath());
      return 2;
    }

    if (!Files.exists(lockPath)) {
      System.err.println("map.lock.pb not found: " + lockPath.toAbsolutePath());
      return 2;
    }

    var manifest = ManifestPb.read(manifestPath);
    var canon = ManifestCanonicalizer.canonicalize(manifest);
    var lock = LockPb.read(lockPath);

    var recomputedHash = LockBuilder.build(canon, null, Instant.EPOCH).getManifestHash();
    var hashOk = recomputedHash.equals(lock.getManifestHash());

    var missingStable = 0;
    for (var e : canon.getEntitiesList()) {
      var stable = lock.getEntityMapMap().get(e.getEntityId());
      if (stable == null || stable.isBlank()) {
        System.err.println("[MISSING] stable_id for " + e.getEntityId());
        missingStable++;
      }
    }

    var missingServiceSig = 0;
    for (var s : canon.getServicesList()) {
      var key = s.getDomain() + "." + s.getService();
      var sig = lock.getServiceSigMap().get(key);
      if (sig == null || sig.isBlank()) {
        System.err.println("[MISSING] service signature for " + key);
        missingServiceSig++;
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
