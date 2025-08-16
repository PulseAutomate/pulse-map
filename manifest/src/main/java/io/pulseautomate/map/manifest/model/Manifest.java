package io.pulseautomate.map.manifest.model;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.util.List;

@JsonPropertyOrder({"schema", "ha_version", "entities", "services"})
public record Manifest(
    int schema, String ha_version, List<Entity> entities, List<Service> services) {}
