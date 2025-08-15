package io.pulseautomate.map.ha.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record HAServiceField(
        @JsonProperty("required") boolean required,
        @JsonProperty("example") Object example,
        @JsonProperty("selector") Map<String, Object> selector,
        @JsonProperty("description") String description
) {
}
