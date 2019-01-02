/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.storage.services.transients;

import io.github.nucleuspowered.storage.dataobjects.modules.ITransientDataModule;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public abstract class AbstractTransientSingleService<TM extends ITransientDataModule> implements ITransientService.Single<TM> {

    private final Map<Class<? extends TM>, TM> cache = new HashMap<>();

    @Override public void removeAll() {
        this.cache.clear();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends TM> Optional<T> get(Class<T> module) {
        return Optional.of((T) this.cache.get(module));
    }

    @Override
    public <T extends TM> void set(Class<T> module, T data) {
        this.cache.put(module, data);
    }

    @Override
    public <T extends TM> void remove(Class<T> module) {
        this.cache.remove(module);
    }

}
