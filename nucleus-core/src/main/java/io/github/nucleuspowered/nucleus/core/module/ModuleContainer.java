/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.core.module;

import com.google.inject.Injector;
import org.jetbrains.annotations.NotNull;

public class ModuleContainer implements Comparable<ModuleContainer> {

    private final String id;
    private final String name;
    private final boolean isRequired;
    private final Class<? extends IModule> moduleClass;

    public static ModuleContainer createContainer(
            final String id,
            final String name,
            final Class<? extends IModule> moduleClass
    ) {
        return new ModuleContainer(id, name, false, moduleClass);
    }

    public static <T> ModuleContainer.Configurable<T> createContainer(
            final String id,
            final String name,
            final Class<? extends IModule.Configurable<T>> moduleClass,
            final Class<T> configurableClass
    ) {
        return new ModuleContainer.Configurable<>(id, name, false, moduleClass, configurableClass);
    }

    public ModuleContainer(
            final String id,
            final String name,
            final boolean isRequired,
            final Class<? extends IModule> moduleClass) {
        this.id = id;
        this.name = name;
        this.isRequired = isRequired;
        this.moduleClass = moduleClass;
    }

    public String getId() {
        return this.id;
    }

    public String getName() {
        return this.name;
    }

    public boolean isRequired() {
        return this.isRequired;
    }

    public Class<? extends IModule> getModuleClass() {
        return this.moduleClass;
    }

    public IModule construct(final Injector injector) {
        return injector.getInstance(this.moduleClass);
    }

    @Override
    public int compareTo(@NotNull ModuleContainer o) {
        return this.id.compareTo(o.id);
    }

    public static final class Configurable<T> extends ModuleContainer {

        private final Class<T> configurationClass;

        public Configurable(
                final String id,
                final String name,
                final boolean isRequired,
                final Class<? extends IModule> moduleClass,
                final Class<T> configurationClass) {
            super(id, name, isRequired, moduleClass);
            this.configurationClass = configurationClass;
        }

        public Class<T> getConfigurationClass() {
            return this.configurationClass;
        }
    }

}
