package io.pulseautomate.map.manifest.infer;

import io.pulseautomate.map.manifest.builder.MapHAState;
import io.pulseautomate.map.manifest.gen.model.AttributeDesc;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class DomainRuleSet {
  private final String domain;
  private final List<AttributeRule> rules;

  public DomainRuleSet(String domain, List<AttributeRule> rules) {
    this.domain = domain;
    this.rules = List.copyOf(rules);
  }

  public String domain() {
    return domain;
  }

  public Map<String, AttributeDesc> infer(MapHAState state) {
    var out = new LinkedHashMap<String, AttributeDesc>();
    for (var rule : rules) {
      rule.infer(state).ifPresent(e -> out.putIfAbsent(e.getKey(), e.getValue()));
    }
    return out.isEmpty() ? null : out;
  }
}
