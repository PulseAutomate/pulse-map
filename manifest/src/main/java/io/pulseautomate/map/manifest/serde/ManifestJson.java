package io.pulseautomate.map.manifest.serde;

import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.pulseautomate.map.manifest.model.Manifest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ManifestJson {
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true)
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private ManifestJson() {}

    public static String toPrettyString(Manifest m) throws IOException {
        return MAPPER.writer(new DefaultPrettyPrinter()).writeValueAsString(m);
    }

    public static void writePretty(Path path, Manifest m) throws IOException {
        Files.createDirectories(path.getParent());
        MAPPER.writer(new DefaultPrettyPrinter()).writeValue(path.toFile(), m);
    }

    public static Manifest read(Path path) throws IOException {
        return MAPPER.readValue(path.toFile(), Manifest.class);
    }
}
