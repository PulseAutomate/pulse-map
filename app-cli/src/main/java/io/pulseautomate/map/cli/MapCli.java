package io.pulseautomate.map.cli;

import picocli.CommandLine;

@CommandLine.Command(
    name = "pulse-map",
    mixinStandardHelpOptions = true,
    version = "pulse-map 0.1",
    description = "Pulse Map CLI",
    subcommands = {})
public final class MapCli implements Runnable {
  public static void main(String[] args) {
    var code = new CommandLine(new MapCli()).execute(args);
    System.exit(code);
  }

  @Override
  public void run() {
    new CommandLine(this).usage(System.out);
  }
}
