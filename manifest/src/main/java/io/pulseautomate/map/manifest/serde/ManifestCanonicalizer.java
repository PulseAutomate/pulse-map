package io.pulseautomate.map.manifest.serde;

import io.pulseautomate.map.manifest.gen.model.*;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

public final class ManifestCanonicalizer {
  private ManifestCanonicalizer() {}

  public static Manifest canonicalize(Manifest m) {
    var entities =
        m.getEntitiesList().stream()
            .sorted(
                Comparator.comparing(Entity::getStableId, Comparator.nullsLast(String::compareTo)))
            .map(ManifestCanonicalizer::canonEntity)
            .toList();

    var services =
        m.getServicesList().stream()
            .sorted(Comparator.comparing(Service::getDomain).thenComparing(Service::getService))
            .map(ManifestCanonicalizer::canonService)
            .toList();

    return Manifest.newBuilder()
        .setSchema(m.getSchema())
        .setHaVersion(m.getHaVersion())
        .addAllEntities(entities)
        .addAllServices(services)
        .build();
  }

  private static Entity canonEntity(Entity e) {
    Map<String, AttributeDesc> attrs = null;
    if (!e.getAttributesMap().isEmpty())
      attrs =
          e.getAttributesMap().entrySet().stream()
              .sorted(Map.Entry.comparingByKey())
              .collect(
                  Collectors.toMap(
                      Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a, LinkedHashMap::new));

    var newEntity =
        Entity.newBuilder()
            .setStableId(e.getStableId())
            .setEntityId(e.getEntityId())
            .setDomain(e.getDomain())
            .setDeviceClass(e.getDeviceClass())
            .setArea(e.getArea());
    if (attrs != null) newEntity.putAllAttributes(attrs);
    return newEntity.build();
  }

  private static Service canonService(Service s) {
    Map<String, ServiceField> fields = null;
    if (!s.getFieldsMap().isEmpty())
      fields =
          s.getFieldsMap().entrySet().stream()
              .sorted(Map.Entry.comparingByKey())
              .collect(
                  Collectors.toMap(
                      Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a, LinkedHashMap::new));

    var newService = Service.newBuilder().setDomain(s.getDomain()).setService(s.getService());
    if (fields != null) newService.putAllFields(fields);
    return newService.build();
  }
}
