package io.pulseautomate.map.manifest.model;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ServiceField(
        String type,
        String unit,
        Boolean required
) {
}
