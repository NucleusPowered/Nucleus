/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.core.module;

import io.github.nucleuspowered.nucleus.core.scaffold.command.ICommandExecutor;
import io.github.nucleuspowered.nucleus.core.scaffold.listener.ListenerBase;
import io.github.nucleuspowered.nucleus.core.scaffold.task.SyncTaskBase;
import io.github.nucleuspowered.nucleus.core.scaffold.task.TaskBase;
import io.github.nucleuspowered.nucleus.core.services.INucleusServiceCollection;
import io.github.nucleuspowered.nucleus.core.services.impl.playerinformation.NucleusProvider;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.DefaultQualifier;
import org.spongepowered.api.data.persistence.DataBuilder;
import org.spongepowered.api.data.persistence.DataContentUpdater;
import org.spongepowered.configurate.serialize.TypeSerializerCollection;
import org.spongepowered.configurate.transformation.ConfigurationTransformation;

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

@DefaultQualifier(NonNull.class)
public interface IModule {

    void init(INucleusServiceCollection serviceCollection);

    default void postLoad(final INucleusServiceCollection serviceCollection) {
    }

    Collection<Class<? extends ICommandExecutor>> getCommands();

    Optional<Class<?>> getPermissions();

    Collection<Class<? extends ListenerBase>> getListeners();

    default Collection<Class<? extends TaskBase>> getAsyncTasks() {
        return Collections.emptyList();
    }

    default Collection<Class<? extends SyncTaskBase>> getSyncTasks() {
        return Collections.emptyList();
    }

    default Optional<NucleusProvider> getInfoProvider() {
        return Optional.empty();
    }

    interface Configurable<T> extends IModule {

        Class<T> getConfigClass();

        default Collection<ConfigurationTransformation> getTransformations() {
            return Collections.emptyList();
        }

        @Nullable
        default TypeSerializerCollection moduleTypeSerializers() {
            return null;
        }

        default T createInstance() {
            try {
                return this.getConfigClass().getDeclaredConstructor().newInstance();
            } catch (final InstantiationException | NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException(e.getMessage(), e);
            }
        }

    }

}
