/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.services.impl.moduledata;

import com.google.common.collect.ImmutableMap;
import io.github.nucleuspowered.nucleus.quickstart.module.StandardModule;
import io.github.nucleuspowered.nucleus.services.interfaces.IModuleDataProvider;
import org.spongepowered.api.util.Tristate;
import uk.co.drnaylor.quickstart.ModuleHolder;
import uk.co.drnaylor.quickstart.exceptions.NoModuleException;
import uk.co.drnaylor.quickstart.holders.DiscoveryModuleHolder;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import com.google.inject.Singleton;

@Singleton
public class ModuleDataProvider implements IModuleDataProvider {

    private final Supplier<DiscoveryModuleHolder<StandardModule, StandardModule>> moduleHolderSupplier;

    public ModuleDataProvider(Supplier<DiscoveryModuleHolder<StandardModule, StandardModule>> moduleHolderSupplier) {
        this.moduleHolderSupplier = moduleHolderSupplier;
    }

    private final Map<String, Class<?>> moduleConfigs = new HashMap<>();
    private final Map<Class<?>, Supplier<?>> providers = new HashMap<>();

    @Override public boolean isLoaded(String id) {
        try {
            return this.moduleHolderSupplier.get().isModuleLoaded(id);
        } catch (NoModuleException e) {
            throw new IllegalArgumentException("The module " + id + " does not exist.");
        }
    }

    @Override public <T> void registerModuleConfig(String moduleId,
            Class<T> typeOfConfig,
            Supplier<T> configGetter) {
        if (this.providers.containsKey(typeOfConfig) || this.moduleConfigs.containsKey(moduleId)) {
            throw new IllegalStateException("Cannot register type or module more than once!");
        }

        this.moduleConfigs.put(moduleId, typeOfConfig);
        this.providers.put(typeOfConfig, configGetter);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T getModuleConfig(Class<T> configType) throws IllegalArgumentException {
        if (this.providers.containsKey(configType)) {
            return (T) this.providers.get(configType).get();
        }

        throw new IllegalArgumentException(configType.getSimpleName() + " does not exist");
    }

    @Override public Map<String, Class<?>> getModuleToConfigType() {
        return ImmutableMap.copyOf(this.moduleConfigs);
    }

    @Override public <T> T getDefaultModuleConfig(Class<T> configType) throws IllegalArgumentException {
        if (this.providers.containsKey(configType)) {
            try {
                return configType.newInstance();
            } catch (InstantiationException | IllegalAccessException e) {
                throw new IllegalArgumentException("Could not instantiate", e);
            }
        }

        throw new IllegalArgumentException(configType.getSimpleName() + " does not exist");
    }

    @Override public Collection<String> getModules(Tristate isEnabled) {
        ModuleHolder.ModuleStatusTristate tristate;
        switch (isEnabled) {
            case TRUE:
                tristate = ModuleHolder.ModuleStatusTristate.ENABLE;
                break;
            case FALSE:
                tristate = ModuleHolder.ModuleStatusTristate.DISABLE;
                break;
            default:
                tristate = ModuleHolder.ModuleStatusTristate.ALL;
                break;
        }
        return this.moduleHolderSupplier.get().getModules(tristate);
    }
}
