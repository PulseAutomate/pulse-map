package io.pulseautomate.map.manifest.serde;

import io.pulseautomate.map.manifest.model.*;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

public final class ManifestCanonicalizer {
  private ManifestCanonicalizer() {}

  public static Manifest canonicalize(Manifest m) {
    var entities =
        m.entities().stream()
            .sorted(
                Comparator.comparing(Entity::stable_id, Comparator.nullsLast(String::compareTo)))
            .map(ManifestCanonicalizer::canonEntity)
            .toList();

    var services =
        m.services().stream()
            .sorted(Comparator.comparing(Service::domain).thenComparing(Service::service))
            .map(ManifestCanonicalizer::canonService)
            .toList();

    return new Manifest(m.schema(), m.ha_version(), entities, services);
  }

  private static Entity canonEntity(Entity e) {
    Map<String, AttributeDesc> attrs = null;
    if (e.attributes() != null)
      attrs =
          e.attributes().entrySet().stream()
              .sorted(Map.Entry.comparingByKey())
              .collect(
                  Collectors.toMap(
                      Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a, LinkedHashMap::new));

    return new Entity(e.stable_id(), e.entity_id(), e.domain(), e.device_class(), e.area(), attrs);
  }

  private static Service canonService(Service s) {
    Map<String, ServiceField> fields = null;
    if (s.fields() != null)
      fields =
          s.fields().entrySet().stream()
              .sorted(Map.Entry.comparingByKey())
              .collect(
                  Collectors.toMap(
                      Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a, LinkedHashMap::new));

    return new Service(s.domain(), s.service(), fields);
  }
}
