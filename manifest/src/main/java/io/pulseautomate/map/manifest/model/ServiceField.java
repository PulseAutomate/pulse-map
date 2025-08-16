package io.pulseautomate.map.manifest.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"type", "unit", "required"})
public record ServiceField(String type, String unit, Boolean required) {}
