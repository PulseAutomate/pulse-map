package io.pulseautomate.map.manifest.model;

import java.util.Map;

public record Service(String domain, String service, Map<String, ServiceField> fields) {}
