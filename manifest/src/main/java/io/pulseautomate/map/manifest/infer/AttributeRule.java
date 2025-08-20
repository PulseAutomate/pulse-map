package io.pulseautomate.map.manifest.infer;

import io.pulseautomate.map.manifest.builder.MapHAState;
import io.pulseautomate.map.manifest.gen.model.AttributeDesc;
import java.util.Map;
import java.util.Optional;

@FunctionalInterface
public interface AttributeRule {
  Optional<Map.Entry<String, AttributeDesc>> infer(MapHAState state);
}
