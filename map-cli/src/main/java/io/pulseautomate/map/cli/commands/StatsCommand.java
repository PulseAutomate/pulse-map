package io.pulseautomate.map.cli.commands;

import io.pulseautomate.map.manifest.serde.ManifestPb;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.concurrent.Callable;
import picocli.CommandLine;

@CommandLine.Command(
    name = "stats",
    mixinStandardHelpOptions = true,
    description = "Print quick stats for a manifest.pb (per-domain counts and attribute coverage).")
public final class StatsCommand implements Callable<Integer> {
  @CommandLine.Option(
      names = {"--manifest"},
      description = "Path to manifest.pb file.",
      paramLabel = "<file>",
      defaultValue = "manifest.pb")
  Path manifestPath;

  @CommandLine.Option(
      names = {"--top"},
      description = "Show top N domains",
      paramLabel = "<n>",
      defaultValue = "10")
  int topN;

  @Override
  public Integer call() throws Exception {
    var manifest = ManifestPb.read(manifestPath);
    var domainCounts = new HashMap<String, Integer>();
    var attrCounts = new HashMap<String, Integer>();

    for (var e : manifest.getEntitiesList()) {
      domainCounts.merge(e.getDomain(), 1, Integer::sum);
      for (var a : e.getAttributesMap().keySet()) {
        attrCounts.merge(e.getDomain() + "." + a, 1, Integer::sum);
      }
    }

    var totalEntities = manifest.getEntitiesCount();
    var totalServices = manifest.getServicesCount();

    System.out.println("Entities: " + totalEntities + " Services: " + totalServices);

    System.out.println("Top domains:");
    domainCounts.entrySet().stream()
        .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
        .limit(topN)
        .forEach(e -> System.out.printf(" %-16s %5d%n", e.getKey(), e.getValue()));

    System.out.println("Top attributes (domain.attr):");
    attrCounts.entrySet().stream()
        .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
        .limit(topN)
        .forEach(e -> System.out.printf(" %-24s %5d%n", e.getKey(), e.getValue()));

    return 0;
  }
}
