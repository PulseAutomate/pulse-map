package io.pulseautomate.map.proto;

import io.pulseautomate.map.manifest.gen.model.Entity;
import io.pulseautomate.map.manifest.gen.model.LockFile;
import io.pulseautomate.map.manifest.gen.model.Manifest;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public final class ProtoGenerator {
  private final Manifest manifest;
  private final LockFile lockFile;

  public ProtoGenerator(Manifest manifest, LockFile lockFile) {
    this.manifest = manifest;
    this.lockFile = lockFile;
  }

  public void generate(Path outDir) throws IOException {
    Files.createDirectories(outDir);
    var packageDir = outDir.resolve("pulse/v1");
    Files.createDirectories(packageDir);

    Files.writeString(packageDir.resolve("attributes.proto"), generateAttributes());
    Files.writeString(packageDir.resolve("entities.proto"), generateEntities());
    Files.writeString(packageDir.resolve("services.proto"), generateServices());

    var enums = generateDomainEnums();
    for (var entry : enums.entrySet()) {
      Files.writeString(packageDir.resolve(entry.getKey()), entry.getValue());
    }
  }

  private String generateAttributes() throws IOException {
    return readTemplate("attributes.proto.template");
  }

  private String generateEntities() throws IOException {
    var template = readTemplate("entities.proto.template");
    var imports =
        manifest.getEntitiesList().stream()
            .map(Entity::getDomain)
            .distinct()
            .sorted()
            .map(domain -> "import \"pulse/v1/" + domain + ".proto\";")
            .collect(Collectors.joining("\n"));

    return template.replace("{{IMPORTS}}", imports);
  }

  private String generateServices() throws IOException {
    return readTemplate("services.proto.template");
  }

  private Map<String, String> generateDomainEnums() throws IOException {
    var domainEnums = new LinkedHashMap<String, StringBuilder>();
    var attrEnums = lockFile.getAttrEnumsMap();

    attrEnums.forEach(
        (key, enumCache) -> {
          var parts = key.split("\\.");
          if (parts.length < 2) return;
          var domain = parts[0];
          var attr = parts[1];

          var enumName = toPascalCase(domain) + toPascalCase(attr);
          var sb = new StringBuilder();

          sb.append("enum ").append(enumName).append(" {\n");
          sb.append("  ").append(enumName.toUpperCase()).append("_UNSPECIFIED = 0;\n");
          var sortedValues = enumCache.getValuesList().stream().sorted().toList();
          for (int i = 0; i < sortedValues.size(); i++) {
            var value = sortedValues.get(i);
            sb.append("  ")
                .append(value.toUpperCase(Locale.ROOT))
                .append(" = ")
                .append(i + 1)
                .append(";\n");
          }
          sb.append("}\n\n");

          domainEnums.computeIfAbsent(domain, k -> new StringBuilder()).append(sb);
        });

    var domainFiles = new LinkedHashMap<String, String>();
    var template = readTemplate("domain.proto.template");
    for (var entry : domainEnums.entrySet()) {
      var domain = entry.getKey();
      var enums = entry.getValue().toString();
      var content = template.replace("{{ENUMS}}", enums);
      domainFiles.put(domain + ".proto", content);
    }
    return domainFiles;
  }

  private static String toPascalCase(String s) {
    var parts = s.split("_");
    var sb = new StringBuilder();

    for (var part : parts) {
      sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
    }

    return sb.toString();
  }

  private String readTemplate(String name) throws IOException {
    var path = "templates/" + name;
    try (InputStream in = getClass().getClassLoader().getResourceAsStream(path)) {
      if (in == null) throw new IOException("Template not found: " + path);
      return new String(in.readAllBytes(), StandardCharsets.UTF_8);
    }
  }
}
