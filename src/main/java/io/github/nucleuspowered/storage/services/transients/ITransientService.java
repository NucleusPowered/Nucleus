/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.storage.services.transients;

import io.github.nucleuspowered.storage.dataobjects.modules.ITransientDataModule;

import java.util.Optional;

public interface ITransientService<TM extends ITransientDataModule> {

    <T extends TM> T tryCreate(Class<T> module) throws Exception;

    void removeAll();

    interface Single<TM extends ITransientDataModule> extends ITransientService<TM> {

        <T extends TM> Optional<T> get(Class<T> module);

        default <T extends TM> T getOrCreate(Class<T> module) throws Exception {
            Optional<T> t = get(module);
            if (t.isPresent()) {
                return t.get();
            }

            T m = tryCreate(module);
            set(module, m);
            return m;
        }

        <T extends TM> void set(Class<T> module, T data);

        <T extends TM> void remove(Class<T> module);

    }

    interface Keyed<K, TM extends ITransientDataModule> extends ITransientService<TM> {

        <T extends TM> Optional<T> get(K key, Class<T> module);

        default <T extends TM> T getOrCreate(K key, Class<T> module) throws Exception {
            Optional<T> t = get(key, module);
            if (t.isPresent()) {
                return t.get();
            }

            T m = tryCreate(module);
            set(key, module, m);
            return m;
        }

        <T extends TM> void set(K key, Class<T> module, T data);

        <T extends TM> void remove(K key, Class<T> module);

        void removeAll(K key);
    }

}
