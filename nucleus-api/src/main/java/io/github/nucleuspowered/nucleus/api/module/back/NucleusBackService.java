/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.api.module.back;

import io.github.nucleuspowered.nucleus.api.util.WorldTransform;
import org.spongepowered.api.world.server.ServerLocation;
import org.spongepowered.math.vector.Vector3d;

import java.util.Optional;
import java.util.UUID;

/**
 * A service that handles the subject's last location before a warp, that is, the location they will warp to if they
 * run /back.
 *
 * <p>
 *     A subject's last location may not be set. It is not saved across server restarts, and may be discarded at any
 *     time the user is not online.
 * </p>
 */
public interface NucleusBackService {

    /**
     * Gets the location of the subject before they executed the last warp that was marked as Returnable.
     *
     * @param user The {@link UUID} of the user
     * @return If it exists, an {@link Optional} containing the {@link ServerLocation}
     */
    Optional<WorldTransform> getLastLocation(UUID user);

    /**
     * Sets the location that the subject will be warped to if they execute /back
     * @param uuid The {@link UUID}
     * @param location The {@link ServerLocation} to set as the /back target.
     * @param rotation The rotation to set as the /back target.
     */
    void setLastLocation(UUID uuid, ServerLocation location, Vector3d rotation);

    /**
     * Removes the last location from the subject, so that /back will not work for them.
     *
     * @param user The {@link UUID}
     */
    void removeLastLocation(UUID user);

    /**
     * Gets a value indicating whether the user will have their last location logged.
     *
     * @param user The {@link UUID} of the user
     *
     * @return <code>true</code> if it is being logged.
     */
    boolean isLoggingLastLocation(UUID user);

    /**
     * Sets whether the user will have their last location logged.
     *
     * @param user The {@link UUID}
     * @param log Whether to log the user's last location.
     */
    void setLoggingLastLocation(UUID user, boolean log);
}
