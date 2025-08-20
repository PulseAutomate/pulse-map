package io.pulseautomate.map.ha.client;

import java.io.Closeable;
import java.util.List;
import java.util.Map;

public interface HAClient extends Closeable {
  List<Map<String, Object>> fetchStates() throws HAHttpException;

  List<Map<String, Object>> fetchServices() throws HAHttpException;

  @Override
  default void close() {}
}
