package io.pulseautomate.map.cli.commands;

import io.pulseautomate.map.cli.run.DiscoverRunner;
import java.net.URI;
import java.nio.file.Path;
import java.util.concurrent.Callable;
import picocli.CommandLine;

@CommandLine.Command(
    name = "discover",
    mixinStandardHelpOptions = true,
    description = "Discover from Home Assistant (or demo) and write manifest.pb and map.lock.pb")
public final class DiscoverCommand implements Callable<Integer> {
  @CommandLine.Option(
      names = {"--out"},
      paramLabel = "<dir>",
      description = "Output directory for manifest.pb and map.lock.pb",
      required = true)
  Path outDir;

  @CommandLine.Option(
      names = {"--ha-url"},
      paramLabel = "<url>",
      description = "Home Assistant base URL (fallback: $HA_URL)")
  URI haUrl;

  @CommandLine.Option(
      names = {"--ha-token"},
      paramLabel = "<token>",
      description = "Home Assistant long-lived access token (fallback: $HA_TOKEN)")
  String haToken;

  @CommandLine.Option(
      names = {"--ha-version"},
      paramLabel = "<ver>",
      description = "Home Assistant version label to embed in manifest (fallback: $HA_VERSION)")
  String haVersion;

  @CommandLine.Option(
      names = {"--demo"},
      description = "Use built-in demo snapshot (ignores HA URL/token)")
  boolean demo;

  @CommandLine.Option(
      names = {"--verbose", "-v"},
      description = "Enable verbose output (default: false)")
  boolean verbose;

  @CommandLine.Option(
      names = {"--json"},
      description = "Output files in human-readable JSON format (default: false)")
  boolean jsonOutput;

  static DiscoverRunner.Factory factory = DiscoverRunner::forHomeAssistant;

  @Override
  public Integer call() throws Exception {
    final var url = haUrl != null ? haUrl : envUri("HA_URL");
    final var token = firstNonBlank(haToken, getEnv("HA_TOKEN"));
    final var version = firstNonBlank(haVersion, getEnv("HA_VERSION"));

    final var useHA = (url != null && !isBlank(token));
    final var runner =
        demo
            ? DiscoverRunner.forDemo(version)
            : (useHA
                ? factory.create(url, token, version)
                : DiscoverRunner.forSnapshotOnly(version));

    if (verbose) {
      System.out.println(
          "[pulse-map] mode="
              + (demo ? "demo" : (useHA ? "ha" : "snapshot"))
              + " out="
              + outDir.toAbsolutePath()
              + " haUrl="
              + (url == null ? "(none)" : url)
              + " haVersion="
              + (version == null ? "(none)" : version)
              + " format="
              + (jsonOutput ? "json" : "protobuf"));
    }

    var res = runner.run(outDir, jsonOutput);

    System.out.printf("Wrote:%n %s%n %s%n", res.manifestPath(), res.lockPath());
    System.out.printf("Entities: %d, Services: %d%n", res.entities(), res.services());

    return 0;
  }

  private static String getEnv(String k) {
    var v = System.getenv(k);
    return isBlank(v) ? null : v;
  }

  private static URI envUri(String k) {
    var v = System.getenv(k);
    return isBlank(v) ? null : URI.create(v);
  }

  private static String firstNonBlank(String a, String b) {
    return isBlank(a) ? b : a;
  }

  private static boolean isBlank(String s) {
    return s == null || s.isBlank();
  }
}
