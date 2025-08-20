package io.pulseautomate.map.manifest.builder;

import static io.pulseautomate.map.manifest.util.Constants.MANIFEST_SCHEMA_V1;

import io.pulseautomate.map.manifest.gen.model.Entity;
import io.pulseautomate.map.manifest.gen.model.Manifest;
import io.pulseautomate.map.manifest.gen.model.Service;
import io.pulseautomate.map.manifest.gen.model.ServiceField;
import io.pulseautomate.map.manifest.infer.RuleRegistry;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ManifestBuilder {
  private final RuleRegistry rules;

  public ManifestBuilder() {
    this(RuleRegistry.defaultRegistry());
  }

  public ManifestBuilder(RuleRegistry rules) {
    this.rules = rules;
  }

  public Manifest build(
      String haVersion, List<Map<String, Object>> states, List<Map<String, Object>> services) {
    var mb = Manifest.newBuilder().setSchema(MANIFEST_SCHEMA_V1).setHaVersion(haVersion);

    states.forEach(
        stateMap -> {
          var state = MapHAState.from(stateMap);
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

    services.forEach(
        serviceDomainMap -> {
          var domain = (String) serviceDomainMap.get("domain");
          @SuppressWarnings("unchecked")
          var serviceDetails = (Map<String, Object>) serviceDomainMap.get("services");

          if (domain == null || serviceDetails == null) return;

          serviceDetails.forEach(
              (serviceName, serviceData) -> {
                var sb = Service.newBuilder().setDomain(domain).setService(serviceName);
                @SuppressWarnings("unchecked")
                var serviceDataMap = (Map<String, Object>) serviceData;
                @SuppressWarnings("unchecked")
                var fieldsMap = (Map<String, Map<String, Object>>) serviceDataMap.get("fields");

                var initialFields = new LinkedHashMap<String, ServiceField>();
                if (fieldsMap != null) {
                  fieldsMap.forEach(
                      (fieldName, fieldData) -> {
                        var required = (boolean) fieldData.getOrDefault("required", false);
                        initialFields.put(
                            fieldName, ServiceField.newBuilder().setRequired(required).build());
                      });
                }

                var typedFields = ServiceTyping.apply(domain, serviceName, initialFields);
                if (typedFields != null) sb.putAllFields(typedFields);

                mb.addServices(sb);
              });
        });

    return mb.build();
  }

  private static String domainOf(String entityId) {
    var i = entityId.indexOf(".");
    return i > 0 ? entityId.substring(0, i) : entityId;
  }
}
