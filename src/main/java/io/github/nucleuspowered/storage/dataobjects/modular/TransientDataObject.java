/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.storage.dataobjects.modular;

import io.github.nucleuspowered.storage.dataobjects.modules.ITransientDataModule;

import java.util.HashMap;
import java.util.Map;

/**
 * A transient data object is an in-memory store of data. Nothing here will be saved to any storage medium and
 * is not guaranteed to last (this is down to the application).
 */
public abstract class TransientDataObject<TM extends ITransientDataModule> {

    private final Map<Class<? extends TM>, TM> transientCache = new HashMap<>();

    @SuppressWarnings("unchecked")
    private <T extends TM> void setTransient(T dataModule) {
        this.transientCache.put((Class<T>) dataModule.getClass(), dataModule);
    }

    @SuppressWarnings("unchecked")
    public final <T extends TM> T getTransient(Class<T> module) {
        if (this.transientCache.containsKey(module)) {
            return (T) this.transientCache.get(module);
        }

        try {

            T dm = tryGetTransient(module);
            setTransient(dm);
            return dm;
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    protected abstract <T extends TM> T tryGetTransient(Class<T> module) throws Exception;
}
