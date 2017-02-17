/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.api.service;

import com.flowpowered.math.vector.Vector3d;
import io.github.nucleuspowered.nucleus.api.Stable;
import io.github.nucleuspowered.nucleus.api.exceptions.NoSuchPlayerException;
import io.github.nucleuspowered.nucleus.api.exceptions.NucleusException;
import io.github.nucleuspowered.nucleus.api.nucleusdata.Home;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.service.user.UserStorageService;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Provides information about user homes.
 */
@Stable
public interface NucleusHomeService {

    /**
     * The default home name.
     */
    String DEFAULT_HOME_NAME = "home";

    /**
     * The pattern that all home names must follow.
     */
    Pattern HOME_NAME_PATTERN = Pattern.compile("^[a-zA-Z][a-zA-Z0-9]{1,15}$");

    /**
     * Gets the {@link Home}s for the specified user, identified by their UUID.
     *
     * @param user The {@link UUID}
     * @return The homes.
     */
    List<Home> getHomes(UUID user);

    /**
     * Gets the {@link Home}s for the specified user, identified by their UUID.
     *
     * @param user The {@link UUID}
     * @return The homes.
     */
    default List<Home> getHomes(User user) {
        return getHomes(user.getUniqueId());
    }

    /**
     * Gets a specified home of the user, if it exists.
     *
     * @param user The {@link UUID} of the user to get the home for.
     * @param name The name of the home.
     * @return The {@link Home}, if it exists.
     */
    Optional<Home> getHome(UUID user, String name);

    default Optional<Home> getHome(User user, String name) {
        return getHome(user.getUniqueId(), name);
    }

    /**
     * Creates a home. This is subject to Nucleus' standard checks.
     *
     * <p>
     *     Home names must follow the regex defined by {@link #HOME_NAME_PATTERN}.
     * </p>
     *
     * @param cause The {@link Cause} of the change. The {@link PluginContainer} must be the root cause.
     * @param user The {@link UUID} of the user to create the home for.
     * @param name The name of the home to create.
     * @param location The location of the home.
     * @param rotation The rotation of the player when they return to this home.
     * @throws NucleusException if the home could not be created, due to home limits, or a plugin cancelled the event.
     */
    void createHome(Cause cause, User user, String name, Location<World> location, Vector3d rotation) throws NucleusException;

    default void createHome(Cause cause, UUID user, String name, Location<World> location, Vector3d rotation) throws NucleusException, NoSuchPlayerException {
        createHome(cause, Sponge.getServiceManager().provideUnchecked(UserStorageService.class).get(user).orElseThrow(NoSuchPlayerException::new), name, location, rotation);
    }

    /**
     * Modifies a home's location.
     *
     * @param cause The {@link Cause} of the change. The {@link PluginContainer} must be the root cause.
     * @param user The {@link UUID} of the user to modify the home for.
     * @param name The name of the home to modify.
     * @param location The location of the home.
     * @param rotation The rotation of the player when they return to this home.
     * @throws NucleusException if the home could not be found, or a plugin cancelled the event.
     */
    default void modifyHome(Cause cause, UUID user, String name, Location<World> location, Vector3d rotation) throws NucleusException {
        modifyHome(cause,
            getHome(user, name).orElseThrow(() -> new NucleusException(Text.of("Home does not exist"), NucleusException.ExceptionType.DOES_NOT_EXIST)),
            location,
            rotation);
    }

    /**
     * Modifies a home's location.
     *
     * @param cause The {@link Cause} of the change. The {@link PluginContainer} must be the root cause.
     * @param home The {@link Home} to modify.
     * @param location The location of the home.
     * @param rotation The rotation of the player when they return to this home.
     * @throws NucleusException if the home could not be found, or a plugin cancelled the event.
     */
    void modifyHome(Cause cause, Home home, Location<World> location, Vector3d rotation) throws NucleusException;

    default void modifyHome(Cause cause, User user, String name, Location<World> location, Vector3d rotation) throws NucleusException {
        modifyHome(cause, user.getUniqueId(), name, location, rotation);
    }

    /**
     * Modifies a home's location, if it exists, otherwise creates a home. This is subject to Nucleus' standard checks.
     *
     * @param cause The {@link Cause} of the change. The {@link PluginContainer} must be the root cause.
     * @param user The {@link UUID} of the user to modify the home for.
     * @param name The name of the home to modify or create.
     * @param location The location of the home.
     * @param rotation The rotation of the player when they return to this home.
     * @throws NucleusException if the home could not be created, due to home limits, or a plugin cancelled the event.
     * @throws NoSuchPlayerException if the supplied UUID does not map to a known user
     */
    default void modifyOrCreateHome(Cause cause, UUID user, String name, Location<World> location, Vector3d rotation) throws NucleusException, NoSuchPlayerException {
        if (getHome(user, name).isPresent()) {
            modifyHome(cause, user, name, location, rotation);
        } else {
            createHome(cause, user, name, location, rotation);
        }
    }

    default void modifyOrCreateHome(Cause cause, User user, String name, Location<World> location, Vector3d rotation) throws NucleusException {
        modifyHome(cause, user.getUniqueId(), name, location, rotation);
    }

    /**
     * Removes a home.
     *
     * @param cause The {@link Cause} of the change. The {@link PluginContainer} must be the root cause.
     * @param user The {@link UUID} of the user to remove the home of.
     * @param name The name of the home to remove.
     * @throws NucleusException if the home could not be found, or a plugin cancelled the event.
     */
    default void removeHome(Cause cause, UUID user, String name) throws NucleusException {
        removeHome(cause, getHome(user, name).orElseThrow(() -> new NucleusException(Text.of("Home does not exist"), NucleusException.ExceptionType.DOES_NOT_EXIST)));
    }

    /**
     * Removes a home.
     *
     * @param cause The {@link Cause} of the change. The {@link PluginContainer} must be the root cause.
     * @param home The {@link Home} to remove.
     * @throws NucleusException if the home could not be found, or a plugin cancelled the event.
     */
    void removeHome(Cause cause, Home home) throws NucleusException;

    /**
     * Returns the maximum number of homes the player can have.
     *
     * @param uuid The {@link UUID} of the player.
     * @return The number of homes, or {@link Integer#MAX_VALUE} if unlimited.
     *
     * @throws IllegalArgumentException if the user cannot be found
     */
    int getMaximumHomes(UUID uuid) throws IllegalArgumentException;

    /**
     * Returns the maximum number of homes the player can have.
     *
     * @param user The {@link User}.
     * @return The number of homes, or {@link Integer#MAX_VALUE} if unlimited.
     */
    int getMaximumHomes(User user);
}
