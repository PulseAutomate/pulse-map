package io.pulseautomate.map.ha.model;

import java.util.List;

public record HASnapshot(
        List<HAState> states,
        List<HAService> services
) {
}
