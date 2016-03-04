/*
 * This file is part of Essence, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.essencepowered.essence.api.service;

import io.github.essencepowered.essence.api.data.EssenceWorld;
import io.github.essencepowered.essence.api.exceptions.NoSuchWorldException;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import org.spongepowered.api.world.World;

import java.io.IOException;
import java.util.UUID;

/**
 * A service that retrieves {@link EssenceWorld}s.
 */
public interface EssenceWorldLoaderService {

    /**
     * Gets the world associated with the provided UUID.
     * @param uuid The {@link UUID} of the world to load.
     * @return The {@link EssenceWorld} that contains the Essenence data for the world.
     *
     * @throws NoSuchWorldException If the world does not exist.
     * @throws IOException If the data file could not be read
     * @throws ObjectMappingException If the data file is malformed.
     */
    EssenceWorld getWorld(UUID uuid) throws NoSuchWorldException, IOException, ObjectMappingException;

    /**
     * Gets the world associated with the provided UUID.
     * @param world The {@link World} to load.
     * @return The {@link EssenceWorld} that contains the Essenence data for the world.
     *
     * @throws IOException If the data file could not be read
     * @throws ObjectMappingException If the data file is malformed.
     */
    EssenceWorld getWorld(World world) throws IOException, ObjectMappingException;

    /**
     * Saves all world data.
     */
    void saveAll();
}
