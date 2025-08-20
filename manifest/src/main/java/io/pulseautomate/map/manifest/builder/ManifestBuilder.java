package io.pulseautomate.map.manifest.builder;

import static io.pulseautomate.map.manifest.util.Constants.MANIFEST_SCHEMA_V1;

import io.pulseautomate.map.ha.model.HASnapshot;
import io.pulseautomate.map.manifest.gen.model.Entity;
import io.pulseautomate.map.manifest.gen.model.Manifest;
import io.pulseautomate.map.manifest.gen.model.Service;
import io.pulseautomate.map.manifest.gen.model.ServiceField;
import io.pulseautomate.map.manifest.infer.RuleRegistry;
import java.util.LinkedHashMap;

public final class ManifestBuilder {
  private final RuleRegistry rules;

  public ManifestBuilder() {
    this(RuleRegistry.defaultRegistry());
  }

  public ManifestBuilder(RuleRegistry rules) {
    this.rules = rules;
  }

  public Manifest build(String haVersion, HASnapshot snapshot) {
    var mb = Manifest.newBuilder().setSchema(MANIFEST_SCHEMA_V1).setHaVersion(haVersion);

    snapshot
        .states()
        .forEach(
            state -> {
              var eb =
                  Entity.newBuilder()
                      .setEntityId(state.entityId())
                      .setDomain(domainOf(state.entityId()));
              var dc = state.attributes().get("device_class");
              if (dc != null) eb.setDeviceClass(dc.toString());

              var set = rules.forDomain(eb.getDomain());
              if (set != null) {
                var attrs = set.infer(state);
                if (attrs != null) eb.putAllAttributes(attrs);
              }
              mb.addEntities(eb);
            });

    snapshot
        .services()
        .forEach(
            hs -> {
              var sb = Service.newBuilder().setDomain(hs.domain()).setService(hs.service());

              var initialFields = new LinkedHashMap<String, ServiceField>();
              if (hs.fields() != null && !hs.fields().isEmpty()) {
                hs.fields()
                    .forEach(
                        (name, f) -> {
                          var fb = ServiceField.newBuilder().setRequired(f.required());
                          initialFields.put(name, fb.build());
                        });
              }

              var typedFields = ServiceTyping.apply(hs, initialFields);

              if (typedFields != null) sb.putAllFields(typedFields);

              mb.addServices(sb);
            });

    return mb.build();
  }

  private static String domainOf(String entityId) {
    var i = entityId.indexOf(".");
    return i > 0 ? entityId.substring(0, i) : entityId;
  }
}
