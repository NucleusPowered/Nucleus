/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.util;

import com.google.inject.Injector;
import io.github.nucleuspowered.nucleus.services.IInitService;
import io.github.nucleuspowered.nucleus.services.impl.NucleusServiceCollection;

import javax.inject.Provider;

public class LazyLoad<T> implements Provider<T> {

    private final NucleusServiceCollection nucleusServiceCollection;
    private final Class<T> clazz;
    private final Injector injector;
    private T instance;

    public LazyLoad(NucleusServiceCollection nucleusServiceCollection, Injector injector, Class<T> clazz) {
        this.nucleusServiceCollection = nucleusServiceCollection;
        this.injector = injector;
        this.clazz = clazz;
    }

    @Override public T get() {
        if (this.instance == null) {
            this.instance = this.injector.getInstance(this.clazz);
            if (this.instance instanceof IInitService) {
                ((IInitService) this.instance).init(this.nucleusServiceCollection);
            }
        }
        return this.instance;
    }
}
