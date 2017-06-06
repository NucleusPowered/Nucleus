/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.api.service;

import io.github.nucleuspowered.nucleus.api.nucleusdata.Kit;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * A service for getting and setting kits.
 */
public interface NucleusKitService {

    /**
     * Gets the names of all the kits currently in NucleusPlugin.
     * @return A {@link Set} of {@link String}s.
     */
    Set<String> getKitNames();

    /**
     * Gets all the kits currently in NucleusPlugin.
     * @return A {@link Map} of names to {@link Kit}s.
     */
    Map<String, Kit> getKits();

    /**
     * Gets the requested kit if it exists.
     *
     * @param name The name of the kit.
     * @return An {@link Optional} that might contain the kit.
     */
    Optional<Kit> getKit(String name);

    /**
     * Removes the requested kit.
     *
     * @param kitName The name of the kit to remove.
     * @return <code>true</code> if a kit was removed.
     */
    boolean removeKit(String kitName);

    /**
     * Saves a kit with the requested name.
     *
     * @param kitName The name of the kit to save.
     * @param kit The kit to save.
     */
    void saveKit(String kitName, Kit kit);

    /**
     * Gets a new kit object for use with the Kit service.
     *
     * <p>
     *     Do not make your own kit type, it will not get saved! Use this instead.
     * </p>
     *
     * @return The {@link Kit}
     */
    Kit createKit();

}
