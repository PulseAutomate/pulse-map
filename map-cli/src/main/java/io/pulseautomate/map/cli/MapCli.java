package io.pulseautomate.map.cli;

import io.pulseautomate.map.cli.commands.DiscoverCommand;
import io.pulseautomate.map.cli.commands.ProtoCommand;
import io.pulseautomate.map.cli.commands.StatsCommand;
import io.pulseautomate.map.cli.commands.ValidateCommand;
import java.util.concurrent.Callable;
import picocli.CommandLine;

@CommandLine.Command(
    name = "pulse-map",
    mixinStandardHelpOptions = true,
    version = "pulse-map 0.1",
    description = "Pulse Map CLI",
    subcommands = {
      DiscoverCommand.class,
      ValidateCommand.class,
      StatsCommand.class,
      ProtoCommand.class
    },
    synopsisSubcommandLabel = "COMMAND")
public final class MapCli implements Callable<Integer> {
  public static void main(String[] args) {
    var code = new CommandLine(new MapCli()).execute(args);
    System.exit(code);
  }

  @Override
  public Integer call() throws Exception {
    new CommandLine(this).usage(System.out);
    return CommandLine.ExitCode.OK;
  }
}
