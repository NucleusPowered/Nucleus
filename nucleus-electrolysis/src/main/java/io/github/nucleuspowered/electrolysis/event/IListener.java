package io.github.nucleuspowered.electrolysis.event;

public interface IListener<T> {

    void handle(T event);

}
