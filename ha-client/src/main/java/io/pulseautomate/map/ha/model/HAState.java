package io.pulseautomate.map.ha.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record HAState(
    @JsonProperty("entity_id") String entityId,
    @JsonProperty("state") String state,
    @JsonProperty("attributes") Map<String, Object> attributes,
    @JsonProperty("last_changed") String lastChanged,
    @JsonProperty("last_updated") String lastUpdated) {}
