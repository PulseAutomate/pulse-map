package io.pulseautomate.map.manifest.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record Entity(
    String stable_id,
    String entity_id,
    String domain,
    String device_class,
    String area,
    Map<String, AttributeDesc> attributes) {}
