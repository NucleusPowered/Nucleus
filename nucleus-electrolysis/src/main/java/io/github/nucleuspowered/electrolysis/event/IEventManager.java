package io.github.nucleuspowered.electrolysis.event;

public interface IEventManager {

    void registerListener(IListener<?> event);

    void unregisterListener(IListener<?> event);

}
