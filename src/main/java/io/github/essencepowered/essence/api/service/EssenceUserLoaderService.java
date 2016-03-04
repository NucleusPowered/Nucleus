/*
 * This file is part of Essence, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.essencepowered.essence.api.service;

import io.github.essencepowered.essence.api.data.EssenceUser;
import io.github.essencepowered.essence.api.exceptions.NoSuchPlayerException;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import org.spongepowered.api.entity.living.player.User;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

/**
 * A service that retrieves {@link EssenceUser}s.
 */
public interface EssenceUserLoaderService {

    /**
     * Gets a list of {@link EssenceUser}s that represents the online.
     *
     * @return A {@link List} of {@link EssenceUser}s.
     */
    List<EssenceUser> getOnlineUsers();

    /**
     * Gets the user data file from their UUID.
     *
     * <p>
     *     Consumers of this API should note that this method handles some caching of {@link User} objects, and in most
     *     cases, will load {@link User} objects from memory, and will handle holding the object in memory after loading
     *     from the persistent data source. Consumers are therefore requested to NOT hold onto user objects in their
     *     code, preferring the UUID of the user so that an up to date object is returned should there be a requirement
     *     to reload the user from disc.
     * </p>
     *
     * @param playerUUID The {@link UUID} of the player in question.
     * @return The {@link EssenceUser}
     *
     * @throws NoSuchPlayerException Thrown when the player has never player on the server.
     * @throws IOException Thrown when the file could not be read.
     * @throws ObjectMappingException Thrown when the object mapper fails.
     */
    EssenceUser getUser(UUID playerUUID) throws NoSuchPlayerException, IOException, ObjectMappingException;

    /**
     * Gets the user data file from the {@link User}.
     *
     * <p>
     *     Consumers of this API should note that this method handles some caching of {@link User} objects, and in most
     *     cases, will load {@link User} objects from memory, and will handle holding the object in memory after loading
     *     from the persistent data source. Consumers are therefore requested to NOT hold onto user objects in their
     *     code, preferring the UUID of the user so that an up to date object is returned should there be a requirement
     *     to reload the user from disc.
     * </p>
     *
     * @param user The {@link User} of the player in question.
     * @return The {@link EssenceUser}
     *
     * @throws IOException Thrown when the file could not be read.
     * @throws ObjectMappingException Thrown when the object mapper fails.
     *
     */
    EssenceUser getUser(User user) throws IOException, ObjectMappingException;
}
