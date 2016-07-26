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
     * Gets warnings for a specific user
     *
     * @param user The {@link User} to get warnings from.
     * @param includeActive If active warnings should be included.
     * @param includeExpired If expired warnings should be included.
     * @return A list of {@link WarnData}.
     */
    List<WarnData> getWarnings(User user, boolean includeActive, boolean includeExpired);

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
     * Removes a warning from a player.
     *
     * @param user The {@link User} to remove a warning from.
     * @param warning The {@link WarnData} to remove.
     * @param permanent If the warning should be removed permanently.
     * @return <code>true</code> if the warning was removed.
     */
    boolean removeWarning(User user, WarnData warning, boolean permanent);

    /**
     * Clears warnings from a player.
     *
     * @param user The {@link User} to remove all warnings from.
     * @param clearActive If active warnings should be removed.
     * @param clearExpired If expired warnings should be removed.
     * @return <code>true</code> if all warnings were removed.
     */
    boolean clearWarnings(User user, boolean clearActive, boolean clearExpired);

    /**
     * Updates a current users warnings
     *
     * @param user The {@link User} to update.
     * @return <code>true</code> if all warnings were updated.
     */
    boolean updateWarnings(User user);
}
