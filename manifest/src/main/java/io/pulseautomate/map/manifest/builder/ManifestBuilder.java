package io.pulseautomate.map.manifest.builder;

import static io.pulseautomate.map.manifest.util.Constants.MANIFEST_SCHEMA_V1;

import io.pulseautomate.map.ha.model.HAService;
import io.pulseautomate.map.ha.model.HASnapshot;
import io.pulseautomate.map.ha.model.HAState;
import io.pulseautomate.map.manifest.infer.RuleRegistry;
import io.pulseautomate.map.manifest.model.*;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class ManifestBuilder {
  private final RuleRegistry rules;

  public ManifestBuilder() {
    this(RuleRegistry.defaultRegistry());
  }

  public ManifestBuilder(RuleRegistry rules) {
    this.rules = rules;
  }

  public Manifest build(String haVersion, HASnapshot snapshot) {
    Objects.requireNonNull(haVersion);
    Objects.requireNonNull(snapshot);

    var entities = snapshot.states().stream().map(this::entityFromState).toList();

    var services = snapshot.services().stream().map(this::serviceFromHA).toList();

    return new Manifest(MANIFEST_SCHEMA_V1, haVersion, entities, services);
  }

  private Entity entityFromState(HAState state) {
    var domain = domainOf(state.entityId());
    var set = rules.forDomain(domain);
    Map<String, AttributeDesc> attrs = set != null ? set.infer(state) : null;

    return new Entity(
        null, state.entityId(), domain, str(state.attributes().get("device_class")), null, attrs);
  }

  private Service serviceFromHA(HAService hs) {
    Map<String, ServiceField> fields;

    if (hs.fields() != null && !hs.fields().isEmpty()) {
      fields = new LinkedHashMap<>();
      hs.fields()
          .forEach((name, f) -> fields.put(name, new ServiceField(null, null, f.required())));
    } else {
      fields = null;
    }

    return new Service(hs.domain(), hs.service(), fields);
  }

  private static String domainOf(String entityId) {
    var i = entityId.indexOf(".");
    return i > 0 ? entityId.substring(0, i) : entityId;
  }

  private static String str(Object o) {
    return o == null ? null : o.toString();
  }
}
