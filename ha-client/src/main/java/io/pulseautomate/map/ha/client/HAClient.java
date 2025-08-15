package io.pulseautomate.map.ha.client;

import io.pulseautomate.map.ha.model.HAService;
import io.pulseautomate.map.ha.model.HASnapshot;
import io.pulseautomate.map.ha.model.HAState;

import java.io.Closeable;
import java.util.List;

public interface HAClient extends Closeable {
    List<HAState> fetchStates() throws HAHttpException;
    List<HAService> fetchServices() throws HAHttpException;

    default HASnapshot fetchSnapshot() throws HAHttpException {
        return new HASnapshot(fetchStates(), fetchServices());
    }

    @Override
    default void close() {}
}
