/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.home.services;

import com.google.inject.Inject;
import io.github.nucleuspowered.nucleus.core.Util;
import io.github.nucleuspowered.nucleus.api.module.home.NucleusHomeService;
import io.github.nucleuspowered.nucleus.api.module.home.data.Home;
import io.github.nucleuspowered.nucleus.api.module.home.exception.HomeException;
import io.github.nucleuspowered.nucleus.api.teleport.data.TeleportResult;
import io.github.nucleuspowered.nucleus.api.teleport.data.TeleportScanners;
import io.github.nucleuspowered.nucleus.core.configurate.datatypes.LocationNode;
import io.github.nucleuspowered.nucleus.modules.home.HomeKeys;
import io.github.nucleuspowered.nucleus.modules.home.HomePermissions;
import io.github.nucleuspowered.nucleus.modules.home.events.AbstractHomeEvent;
import io.github.nucleuspowered.nucleus.modules.home.events.CreateHomeEvent;
import io.github.nucleuspowered.nucleus.modules.home.events.DeleteHomeEvent;
import io.github.nucleuspowered.nucleus.modules.home.events.ModifyHomeEvent;
import io.github.nucleuspowered.nucleus.modules.home.events.UseHomeEvent;
import io.github.nucleuspowered.nucleus.core.scaffold.service.ServiceBase;
import io.github.nucleuspowered.nucleus.core.scaffold.service.annotations.APIService;
import io.github.nucleuspowered.nucleus.core.services.INucleusServiceCollection;
import io.github.nucleuspowered.nucleus.core.services.impl.storage.dataobjects.modular.IUserDataObject;
import io.github.nucleuspowered.nucleus.core.services.interfaces.INucleusLocationService;
import io.github.nucleuspowered.nucleus.core.services.interfaces.IPermissionService;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.event.Cause;
import org.spongepowered.api.event.CauseStackManager;
import org.spongepowered.api.world.server.ServerLocation;
import org.spongepowered.api.world.teleport.TeleportHelperFilter;
import org.spongepowered.math.vector.Vector3d;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

@APIService(NucleusHomeService.class)
public class HomeService implements NucleusHomeService, ServiceBase {

    private final INucleusServiceCollection serviceCollection;

    @Inject
    public HomeService(final INucleusServiceCollection serviceCollection) {
        this.serviceCollection = serviceCollection;
    }

    @Override
    public List<Home> getHomes(final UUID user) {
        final Optional<IUserDataObject> service = this.serviceCollection.storageManager().getUserOnThread(user); //.get().getHome;
        return service.map(modularUserService -> this.getHomes(user, modularUserService)).orElseGet(Collections::emptyList);

    }

    private List<Home> getHomes(final UUID user, final IUserDataObject userDataObject) {
        return this.getHomesFrom(user, userDataObject.get(HomeKeys.HOMES).orElseGet(Collections::emptyMap));
    }

    public Collection<String> getHomeNames(final UUID user) {
        return this.serviceCollection.storageManager()
                .getUserOnThread(user).flatMap(x -> x.get(HomeKeys.HOMES).map(Map::keySet)).orElseGet(Collections::emptySet);
    }

    @Override public Optional<Home> getHome(final UUID user, final String name) {
        final Optional<IUserDataObject> service = this.serviceCollection.storageManager().getUser(user).join();
        return service.flatMap(modularUserService -> this.getHome(name, user, modularUserService.get(HomeKeys.HOMES).orElse(null)));

    }

    @Override public void createHome(final UUID user, final String name, final ServerLocation location, final Vector3d rotation)
            throws HomeException {

        final Cause cause = Sponge.server().causeStackManager().currentCause();
        if (!NucleusHomeService.HOME_NAME_PATTERN.matcher(name).matches()) {
            throw new HomeException(
                    this.serviceCollection.messageProvider().getMessageFor(
                            Sponge.server().causeStackManager().currentCause()
                                    .first(Audience.class).orElseGet(Sponge::systemSubject),
                            "command.sethome.name"),
                    HomeException.Reasons.INVALID_NAME
            );
        }

        final int max = this.getMaximumHomes(user);
        final IUserDataObject udo = this.serviceCollection.storageManager().getOrCreateUserOnThread(user);
        final Map<String, LocationNode> m = udo.get(HomeKeys.HOMES).orElseGet(Collections::emptyMap);
        if (m.size() >= max) {
            throw new HomeException(
                    this.serviceCollection.messageProvider().getMessageFor(cause.first(Audience.class).orElseGet(Sponge::systemSubject),
                            "command.sethome.limit", String.valueOf(max)),
                    HomeException.Reasons.LIMIT_REACHED);
        }

        final CreateHomeEvent event = new CreateHomeEvent(name, user, cause, location);
        this.postEvent(event);

        if (!this.setHome(user, m, name, location, rotation, false)) {
            throw new HomeException(
                    this.serviceCollection.messageProvider().getMessageFor(
                        cause.first(Audience.class).orElseGet(Sponge::systemSubject),
                            "command.sethome.seterror",
                            name
                    ),
                    HomeException.Reasons.UNKNOWN);
        }

    }

    @Override
    public void modifyHome(final UUID user, final String name, final ServerLocation location, final Vector3d rotation)
            throws HomeException {
        final Home home = this.getHome(user, name)
                .orElseThrow(() -> new HomeException(Component.text("That home does not exist"), HomeException.Reasons.DOES_NOT_EXIST));
        this.modifyHomeInternal(home, location, rotation);
    }

    @Override
    public void modifyHome(final Home home, final ServerLocation location, final Vector3d rotation) throws HomeException {
        // Preconditions.checkState(cause.root() instanceof PluginContainer, "The root must be a PluginContainer");
        this.modifyHomeInternal(home, location, rotation);
    }

    public void modifyHomeInternal(final Home home, final ServerLocation location, final Vector3d rotation) throws HomeException {
        final Cause cause = Sponge.server().causeStackManager().currentCause();
        final ModifyHomeEvent event = new ModifyHomeEvent(cause, home, location);
        this.postEvent(event);

        final IUserDataObject udo = this.serviceCollection.storageManager().getOrCreateUserOnThread(home.getOwnersUniqueId());
        final Map<String, LocationNode> m = udo.get(HomeKeys.HOMES).orElseGet(Collections::emptyMap);
        if (!this.setHome(home.getOwnersUniqueId(), m, home.getName(), location, rotation, true)) {
            throw new HomeException(
                    this.serviceCollection.messageProvider().getMessageFor(
                            cause.first(Audience.class).orElseGet(Sponge::systemSubject),
                            "command.sethome.seterror",
                            home.getName()
                    ),
                    HomeException.Reasons.UNKNOWN);
        }

    }

    @Override
    public void removeHome(final UUID uuid, final String homeName) throws HomeException {
        final Home home = this.getHome(uuid, homeName)
                .orElseThrow(() -> new HomeException(Component.text("That home does not exist"), HomeException.Reasons.DOES_NOT_EXIST));

        try (final CauseStackManager.StackFrame frame = Sponge.server().causeStackManager().pushCauseFrame()) {
            final Cause cause = frame.currentCause();
            final DeleteHomeEvent event = new DeleteHomeEvent(cause, home);
            this.postEvent(event);

            final IUserDataObject udo = this.serviceCollection.storageManager().getOrCreateUserOnThread(home.getOwnersUniqueId());
            final Map<String, LocationNode> m = udo.get(HomeKeys.HOMES).orElseGet(Collections::emptyMap);
            if (!this.deleteHome(home.getOwnersUniqueId(), m, home.getName())) {
                throw new HomeException(
                        this.serviceCollection.messageProvider().getMessageFor(
                                cause.first(Audience.class).orElseGet(Sponge::systemSubject),
                                "command.home.delete.fail",
                                home.getName()),
                        HomeException.Reasons.UNKNOWN);
            }
        }
    }

    @Override
    public int getMaximumHomes(final UUID uuid) throws IllegalArgumentException {
        final Optional<User> user = Sponge.server().userManager().load(uuid).join();
        if (!user.isPresent()) {
            throw new IllegalArgumentException("user does not exist.");
        }

        return this.getMaximumHomes(user.get());
    }

    @Override
    public int getMaximumHomes(final User src) {
        final IPermissionService permissionService = this.serviceCollection.permissionService();
        if (permissionService.hasPermission(src, HomePermissions.HOMES_UNLIMITED)) {
            return Integer.MAX_VALUE;
        }

        return Math.max(permissionService.getPositiveIntOptionFromSubject(src, NucleusHomeService.HOME_COUNT_OPTION)
                .orElse(1), 1);
    }

    public TeleportResult warpToHome(final ServerPlayer src, final Home home, final boolean safeTeleport) throws HomeException {
        Sponge.server().worldManager().world(home.getResourceKey())
                .orElseThrow(() ->
                        new HomeException(
                                this.serviceCollection.messageProvider().getMessageFor(src, "command.home.invalid", home.getName()),
                                HomeException.Reasons.INVALID_LOCATION
                        ));

        final ServerLocation targetLocation = home.getLocation().orElseThrow((() ->
                        new HomeException(
                                this.serviceCollection.messageProvider().getMessageFor(src, "command.home.invalid", home.getName()),
                                HomeException.Reasons.INVALID_LOCATION
                        )));
                // ReturnMessageException.fromKey("command.home.invalid", home.getName()));

        try (final CauseStackManager.StackFrame frame = Sponge.server().causeStackManager().pushCauseFrame()) {
            frame.pushCause(src);
            this.postEvent(new UseHomeEvent(frame.currentCause(), src.uniqueId(), home));
        }

        final INucleusLocationService teleportService = this.serviceCollection.teleportService();
        final TeleportHelperFilter filter = teleportService.getAppropriateFilter(src, safeTeleport);

        return teleportService.teleportPlayer(
                        src,
                        targetLocation,
                        home.getRotation(),
                        false,
                        TeleportScanners.NO_SCAN.get(),
                        filter
                );
    }

    private void postEvent(final AbstractHomeEvent event) throws HomeException {
        if (Sponge.eventManager().post(event)) {
            throw new HomeException(event.getCancelMessage().orElseGet(() ->
                    this.serviceCollection.messageProvider().getMessageFor(
                            event.cause().first(Audience.class).orElseGet(Sponge::systemSubject),
                            "nucleus.eventcancelled")),
                    HomeException.Reasons.PLUGIN_CANCELLED
            );
        }
    }

    private List<Home> getHomesFrom(final UUID uuid, final Map<String, LocationNode> msln) {
        final List<Home> i = new ArrayList<>();
        for (final Map.Entry<String, LocationNode> entry : msln.entrySet()) {
            i.add(this.getHomeFrom(entry.getKey(), uuid, entry.getValue()));
        }

        return Collections.unmodifiableList(i);
    }

    private Home getHomeFrom(final String string, final UUID user, final LocationNode node) {
        return new NucleusHome(string, user, node);
    }

    private Optional<Home> getHome(final String home, final UUID uuid, @Nullable final Map<String, LocationNode> homeData) {
        if (homeData == null) {
            return Optional.empty();
        }
        return Util.getValueIgnoreCase(homeData, home).map(x -> this.getHomeFrom(home, uuid, x));
    }

    private boolean setHome(final UUID uuid, Map<String, LocationNode> m, final String home, final ServerLocation location, final Vector3d rotation,
            final boolean overwrite) {
        final Pattern warpName = Pattern.compile("^[a-zA-Z][a-zA-Z0-9]{1,15}$");

        if (m == null) {
            m = new HashMap<>();
        } else {
            m = new HashMap<>(m);
        }

        final Optional<String> os = Util.getKeyIgnoreCase(m, home);
        if (os.isPresent() || !warpName.matcher(home).matches()) {
            if (!overwrite || !this.deleteHome(m, home)) {
                return false;
            }
        }

        m.put(home, new LocationNode(location, rotation));
        this.setAndSave(uuid, m);
        return true;
    }

    private boolean deleteHome(final Map<String, LocationNode> m, final String home) {
        if (m == null || m.isEmpty()) {
            return false;
        }

        final Optional<String> os = Util.getKeyIgnoreCase(m, home);
        if (os.isPresent()) {
            m.remove(os.get());
            return true;
        }

        return false;
    }

    private boolean deleteHome(final UUID uuid, Map<String, LocationNode> m, final String home) {
        if (m == null || m.isEmpty()) {
            return false;
        }

        final Optional<String> os = Util.getKeyIgnoreCase(m, home);
        if (os.isPresent()) {
            m = new HashMap<>(m);
            m.remove(os.get());
            this.setAndSave(uuid, m);
            return true;
        }

        return false;
    }

    private void setAndSave(final UUID uuid, final Map<String, LocationNode> map) {
        this.serviceCollection.storageManager().getUserService().setAndSave(uuid, HomeKeys.HOMES, map);
    }
}
