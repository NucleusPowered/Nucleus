/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.api.service;

import io.github.nucleuspowered.nucleus.api.data.WarnData;
import org.spongepowered.api.entity.living.player.User;

import java.util.List;

/**
 * A service that determines whether a player has warnings.
 */
public interface NucleusWarnService {

    /**
     * Gets all warnings for a specific user
     *
     * @param user The {@link User} to check.
     * @return A list of {@link WarnData}.
     */
    List<WarnData> getWarnings(User user);

    /**
     * Adds a warning to a player for a specified duration.
     *
     * @param user The {@link User} to warn.
     * @param warning The {@link WarnData} to add.
     * @return <code>true</code> if the warning was added.
     */
    boolean addWarning(User user, WarnData warning);

    /**
     * Removes a warning from a player.
     *
     * @param user The {@link User} to remove a warning from.
     * @param warning The {@link WarnData} to remove.
     * @return <code>true</code> if the warning was removed.
     */
    boolean removeWarning(User user, WarnData warning);

    /**
     * Clears all warnings from a player.
     *
     * @param user The {@link User} to remove all warnings from.
     * @return <code>true</code> if all warnings were removed.
     */
    boolean clearWarnings(User user);

    /**
     * Updates a current users warnings
     *
     * @param user The {@link User} to update.
     * @return <code>true</code> if all warnings were updated.
     */
    boolean updateWarnings(User user);
}
