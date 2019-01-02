/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.storage.services.transients;

import io.github.nucleuspowered.storage.dataobjects.modules.ITransientDataModule;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public abstract class AbstractTransientKeyedService<K, TM extends ITransientDataModule> implements ITransientService.Keyed<K, TM> {

    private final Map<K, Value> cache = new HashMap<>();

    @Override public void removeAll() {
        this.cache.clear();
    }

    @Override public <T extends TM> Optional<T> get(K key, Class<T> module) {
        Value v = this.cache.get(key);
        if (v != null) {
            return v.get(module);
        }

        return Optional.empty();
    }

    @Override public <T extends TM> void set(K key, Class<T> module, T data) {
        this.cache.computeIfAbsent(key, k -> new Value()).set(module, data);
    }

    @Override public <T extends TM> void remove(K key, Class<T> module) {
        Value v = this.cache.get(key);
        if (v != null) {
            v.remove(module);
        }
    }

    @Override public void removeAll(K key) {
        this.cache.remove(key);
    }

    private class Value extends AbstractTransientSingleService<TM> {

        private final Map<Class<? extends TM>, TM> cache = new HashMap<>();

        @Override public <T extends TM> T tryCreate(Class<T> module) throws Exception {
            return AbstractTransientKeyedService.this.tryCreate(module);
        }

    }
}
