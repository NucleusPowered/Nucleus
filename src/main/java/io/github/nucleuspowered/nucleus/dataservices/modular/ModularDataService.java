/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.dataservices.modular;

import co.aikar.timings.Timing;
import co.aikar.timings.Timings;
import com.google.common.collect.ImmutableMap;
import io.github.nucleuspowered.nucleus.Nucleus;
import io.github.nucleuspowered.nucleus.dataservices.AbstractService;
import io.github.nucleuspowered.nucleus.dataservices.dataproviders.DataProvider;
import ninja.leaping.configurate.ConfigurationNode;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public abstract class ModularDataService<S extends ModularDataService<S>> extends AbstractService<ConfigurationNode> {

    protected static final Object[] VERSION_PATH = { "version" };
    private final Map<Class<?>, DataModule<S>> cached = new HashMap<>();
    private final Map<Class<?>, TransientModule<S>> transientCache = new HashMap<>();
    private final Timing saveTimings = Timings.of(Nucleus.getNucleus(), "Data Modules - Saving");
    private final Timing loadTimings = Timings.of(Nucleus.getNucleus(), "Data Modules - Loading");
    private final Timing loadTransientTimings = Timings.of(Nucleus.getNucleus(), "Transient Modules - Loading");

    private final Object lockingObject = new Object();

    ModularDataService(DataProvider<ConfigurationNode> dataProvider) {
        super(dataProvider);
    }

    @SuppressWarnings("unchecked")
    public final <T extends TransientModule<S>> T getTransient(Class<T> module) {
        if (this.transientCache.containsKey(module)) {
            return (T) this.transientCache.get(module);
        }

        try {
            this.loadTransientTimings.startTimingIfSync();

            T dm;
            Optional<T> m = tryGetTransient(module);
            if (m.isPresent()) {
                dm = m.get();
            } else {
                Nucleus.getNucleus().getLogger()
                        .warn("Attempting to construct " + module.getSimpleName() + " by reflection. Please add this to the factory.");
                dm = module.newInstance();
            }

            setTransient(dm);
            return dm;
        } catch (IllegalAccessException | InstantiationException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        } finally {
            try {
                this.loadTransientTimings.stopTimingIfSync();
            } catch (final IllegalStateException exception) {
                if (!Thread.currentThread().getName().contains("Shutdown")) {
                    Nucleus.getNucleus().getLogger()
                            .warn("Could not stop timings - if you are shutting down the server you can ignore this message.");
                }
            }
        }
    }

    abstract <T extends TransientModule<S>> Optional<T> tryGetTransient(Class<T> module);

    @SuppressWarnings({"unchecked", "JavaReflectionMemberAccess"})
    public final <T extends DataModule<S>> T get(Class<T> module) {
        synchronized (this.lockingObject) {
            if (this.cached.containsKey(module)) {
                return (T) this.cached.get(module);
            }

            try {
                this.loadTimings.startTimingIfSync();
                Optional<T> m = tryGet(module);
                T dm;
                if (m.isPresent()) {
                    dm = m.get();
                } else {
                    Nucleus.getNucleus().getLogger()
                            .warn("Attempting to construct " + module.getSimpleName() + " by reflection. Please add this to the factory.");

                    if (DataModule.ReferenceService.class.isAssignableFrom(module)) {
                        Constructor s = module.getDeclaredConstructor(this.getClass());
                        s.setAccessible(true);
                        dm = (T) s.newInstance(this);
                    } else {
                        dm = module.newInstance();
                    }
                }

                dm.loadFrom(this.data);
                set(dm);
                return dm;
            } catch (IllegalAccessException | NoSuchMethodException | InvocationTargetException | InstantiationException e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            } finally {
                this.loadTimings.stopTimingIfSync();
            }
        }
    }

    abstract <T extends DataModule<S>> Optional<T> tryGet(Class<T> module);

    public <T extends DataModule<S>> void set(T dataModule) {
        synchronized (this.lockingObject) {
            this.cached.put(dataModule.getClass(), dataModule);
        }
    }

    private <T extends TransientModule<S>> void setTransient(T dataModule) {
        this.transientCache.put(dataModule.getClass(), dataModule);
    }

    @Override public void loadInternal() throws Exception {
        super.loadInternal();
        this.cached.clear(); // Only clear if no exception was caught.
        int version = this.data.getNode(VERSION_PATH).getInt(-1);
        migrate();
        int newVersion = this.data.getNode(VERSION_PATH).getInt(-1);
        if (version != newVersion) {
            saveInternal();
        }
    }

    @Override public void saveInternal() throws Exception {
        try {
            this.saveTimings.startTimingIfSync();

            // If there is nothing in the cache, don't save (because we don't need to).
            if (this.data != null && (!this.cached.isEmpty() || !(this.data.isVirtual() || this.data.getValue() == null))) {
                ImmutableMap.copyOf(this.cached).values().forEach(x -> x.saveTo(this.data));
                super.saveInternal();
            }
        } finally {
            this.saveTimings.stopTimingIfSync();
        }
    }

    public void migrate() {
        // noop
    }

}
