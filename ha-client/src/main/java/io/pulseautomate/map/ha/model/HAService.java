package io.pulseautomate.map.ha.model;

import java.util.Map;

public record HAService(
        String domain,
        String service,
        Map<String, HAServiceField> fields
) {
}
