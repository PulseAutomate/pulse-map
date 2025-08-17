package io.pulseautomate.map.manifest.lock;

import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class LockJson {
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true)
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private LockJson() {}

    public static String toPrettyString(LockFile lf) throws IOException {
        return MAPPER.writer(new DefaultPrettyPrinter()).writeValueAsString(lf);
    }

    public static void writePretty(Path path, LockFile lf) throws IOException {
        Files.createDirectories(path.getParent());
        MAPPER.writer(new DefaultPrettyPrinter()).writeValue(path.toFile(), lf);
    }

    public static LockFile read(Path path) throws IOException {
        return MAPPER.readValue(path.toFile(), LockFile.class);
    }
}
