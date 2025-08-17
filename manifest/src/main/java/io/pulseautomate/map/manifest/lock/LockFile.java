package io.pulseautomate.map.manifest.lock;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "schema",
        "manifest_hash",
        "generated_at",
        "entity_map",
        "service_sig",
        "attr_enums"})
public record LockFile(
        int schema,
        String manifest_hash,
        String generated_at,
        Map<String, String> entity_map,
        Map<String,String> service_sig,
        Map<String, List<String>> attr_enums
) {
}
