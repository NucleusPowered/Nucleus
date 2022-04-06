/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.core;

import io.github.nucleuspowered.nucleus.core.scaffold.command.modifier.CommandModifierFactory;
import io.github.nucleuspowered.storage.persistence.IStorageRepositoryFactory;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.registry.RegistryKey;
import org.spongepowered.api.registry.RegistryRoots;
import org.spongepowered.api.registry.RegistryType;

public final class Registry {

    public static final class Keys {

        public static final ResourceKey COMMAND_MODIFIER_FACTORY_KEY = ResourceKey.of("nucleus", "command_modifier_factory");

        public static final ResourceKey STORAGE_REPOSITORY_KEY = ResourceKey.of("nucleus", "storage_repository_factory");

        public static final ResourceKey FLAT_FILE_STORAGE_KEY = ResourceKey.of("nucleus", "flat_file");
    }

    public static final class Types {

        public static final RegistryType<CommandModifierFactory> COMMAND_MODIFIER_FACTORY =
                RegistryType.of(RegistryRoots.SPONGE, Keys.COMMAND_MODIFIER_FACTORY_KEY);

        public static final RegistryType<IStorageRepositoryFactory> STORAGE_REPOSITORY =
                RegistryType.of(RegistryRoots.SPONGE, Keys.STORAGE_REPOSITORY_KEY);

    }

    public static final class RegistryKeys {
        public static final RegistryKey<IStorageRepositoryFactory> STORAGE_REPOSITORY_FLAT_FILE =
                RegistryKey.of(Types.STORAGE_REPOSITORY, Keys.FLAT_FILE_STORAGE_KEY);

    }



}
