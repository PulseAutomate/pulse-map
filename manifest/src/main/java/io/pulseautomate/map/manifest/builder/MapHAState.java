package io.pulseautomate.map.manifest.builder;

import java.util.Collections;
import java.util.Map;

public record MapHAState(String entityId, Map<String, Object> attributes) {
  @SuppressWarnings("unchecked")
  public static MapHAState from(Map<String, Object> stateMap) {
    if (stateMap == null) return new MapHAState("", Collections.emptyMap());
    var entityId = (String) stateMap.getOrDefault("entity_id", "");
    var attributes =
        (Map<String, Object>) stateMap.getOrDefault("attributes", Collections.emptyMap());
    return new MapHAState(entityId, attributes);
  }
}
