package io.pulseautomate.map.cli.commands;

import io.pulseautomate.map.manifest.lock.LockPb;
import io.pulseautomate.map.manifest.serde.ManifestPb;
import io.pulseautomate.map.proto.ProtoGenerator;
import picocli.CommandLine;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;

@CommandLine.Command(
        name = "proto",
        mixinStandardHelpOptions = true,
        description = "Generate Protobuf definitions from manifest and lock file"
)
public final class ProtoCommand implements Callable<Integer> {
    @CommandLine.Option(
            names = {"--dir"},
            description = "Directory containing manifest.pb and map.lock.pb",
            paramLabel = "<dir>",
            defaultValue = "."
    )
    private Path dir;

    @CommandLine.Option(
            names = {"--out"},
            description = "Output directory for the generated .proto files",
            paramLabel = "<dir>",
            required = true
    )
    private Path outDir;

    @Override
    public Integer call() throws Exception {
        var manifestPath = dir.resolve("manifest.pb");
        var lockFilePath = dir.resolve("map.lock.pb");

        if (!Files.exists(manifestPath) || !Files.exists(lockFilePath)) {
            System.err.println("Error: manifest.pd and map.lock.pb must exist in the specified directory.");
            return 1;
        }

        var manifest = ManifestPb.read(manifestPath);
        var lockFile = LockPb.read(lockFilePath);
        var generator = new ProtoGenerator(manifest, lockFile);

        generator.generate(outDir);
        System.out.println("Successfully generated Protobuf files in " + outDir);
        return 0;
    }
}
