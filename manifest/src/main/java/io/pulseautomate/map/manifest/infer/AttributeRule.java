package io.pulseautomate.map.manifest.infer;

import io.pulseautomate.map.ha.model.HAState;
import io.pulseautomate.map.manifest.model.AttributeDesc;
import java.util.Map;
import java.util.Optional;

@FunctionalInterface
public interface AttributeRule {
  Optional<Map.Entry<String, AttributeDesc>> infer(HAState state);
}
