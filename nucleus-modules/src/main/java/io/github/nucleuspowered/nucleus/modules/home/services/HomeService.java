/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.home.services;

import com.flowpowered.math.vector.Vector3d;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import io.github.nucleuspowered.nucleus.Util;
import io.github.nucleuspowered.nucleus.api.module.home.NucleusHomeService;
import io.github.nucleuspowered.nucleus.api.module.home.data.Home;
import io.github.nucleuspowered.nucleus.api.module.home.exception.HomeException;
import io.github.nucleuspowered.nucleus.api.teleport.data.TeleportResult;
import io.github.nucleuspowered.nucleus.api.teleport.data.TeleportScanners;
import io.github.nucleuspowered.nucleus.configurate.datatypes.LocationNode;
import io.github.nucleuspowered.nucleus.modules.home.HomeKeys;
import io.github.nucleuspowered.nucleus.modules.home.HomePermissions;
import io.github.nucleuspowered.nucleus.modules.home.events.AbstractHomeEvent;
import io.github.nucleuspowered.nucleus.modules.home.events.CreateHomeEvent;
import io.github.nucleuspowered.nucleus.modules.home.events.DeleteHomeEvent;
import io.github.nucleuspowered.nucleus.modules.home.events.ModifyHomeEvent;
import io.github.nucleuspowered.nucleus.modules.home.events.UseHomeEvent;
import io.github.nucleuspowered.nucleus.scaffold.service.ServiceBase;
import io.github.nucleuspowered.nucleus.scaffold.service.annotations.APIService;
import io.github.nucleuspowered.nucleus.services.INucleusServiceCollection;
import io.github.nucleuspowered.nucleus.services.impl.storage.dataobjects.modular.IUserDataObject;
import io.github.nucleuspowered.nucleus.services.interfaces.INucleusTeleportService;
import io.github.nucleuspowered.nucleus.services.interfaces.IPermissionService;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.event.CauseStackManager;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.service.user.UserStorageService;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;
import org.spongepowered.api.world.teleport.TeleportHelperFilter;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

import javax.annotation.Nullable;
import javax.inject.Inject;

@APIService(NucleusHomeService.class)
public class HomeService implements NucleusHomeService, ServiceBase {

    private final INucleusServiceCollection serviceCollection;

    @Inject
    public HomeService(INucleusServiceCollection serviceCollection) {
        this.serviceCollection = serviceCollection;
    }

    @Override
    public List<Home> getHomes(UUID user) {
        Optional<IUserDataObject> service = this.serviceCollection.storageManager().getUserOnThread(user); //.get().getHome;
        return service.map(modularUserService -> getHomes(user, modularUserService)).orElseGet(ImmutableList::of);

    }

    private List<Home> getHomes(UUID user, IUserDataObject userDataObject) {
        return getHomesFrom(user, userDataObject.get(HomeKeys.HOMES).orElseGet(ImmutableMap::of));
    }

    public Collection<String> getHomeNames(UUID user) {
        return this.serviceCollection.storageManager()
                .getUserOnThread(user).flatMap(x -> x.get(HomeKeys.HOMES).map(Map::keySet)).orElseGet(ImmutableSet::of);
    }

    @Override public Optional<Home> getHome(UUID user, String name) {
        Optional<IUserDataObject> service = this.serviceCollection.storageManager().getUser(user).join();
        return service.flatMap(modularUserService -> getHome(name, user, modularUserService.get(HomeKeys.HOMES).orElse(null)));

    }

    @Override public void createHome(Cause cause, User user, String name, Location<World> location, Vector3d rotation) throws HomeException  {
        // Preconditions.checkState(cause.root() instanceof PluginContainer, "The root must be a PluginContainer");
        createHomeInternal(cause, user, name, location, rotation);
    }

    public void createHomeInternal(Cause cause, User user, String name, Location<World> location, Vector3d rotation) throws HomeException {
        if (!NucleusHomeService.HOME_NAME_PATTERN.matcher(name).matches()) {
            throw new HomeException(
                    this.serviceCollection.messageProvider().getMessageFor(
                            cause.first(CommandSource.class).orElseGet(Sponge.getServer()::getConsole),
                            "command.sethome.name"),
                    HomeException.Reasons.INVALID_NAME
            );
        }

        int max = getMaximumHomes(user);
        IUserDataObject udo = this.serviceCollection.storageManager().getOrCreateUserOnThread(user.getUniqueId());
        Map<String, LocationNode> m = udo.get(HomeKeys.HOMES).orElseGet(ImmutableMap::of);
        if (m.size() >= max) {
            throw new HomeException(
                    this.serviceCollection.messageProvider().getMessageFor(cause.first(CommandSource.class)
                                    .orElseGet(Sponge.getServer()::getConsole), "command.sethome.limit", String.valueOf(max)),
                    HomeException.Reasons.LIMIT_REACHED);
        }

        CreateHomeEvent event = new CreateHomeEvent(name, user, cause, location);
        postEvent(event);

        if (!setHome(user.getUniqueId(), m, name, location, rotation, false)) {
            throw new HomeException(
                    this.serviceCollection.messageProvider().getMessageFor(
                        Util.getSourceFromCause(cause),
                            "command.sethome.seterror",
                            name
                    ),
                    HomeException.Reasons.UNKNOWN);
        }

    }

    @Override public void modifyHome(Cause cause, Home home, Location<World> location, Vector3d rotation) throws HomeException {
        // Preconditions.checkState(cause.root() instanceof PluginContainer, "The root must be a PluginContainer");
        modifyHomeInternal(cause, home, location, rotation);
    }

    public void modifyHomeInternal(Cause cause, Home home, Location<World> location, Vector3d rotation) throws HomeException {
        ModifyHomeEvent event = new ModifyHomeEvent(cause, home, location);
        postEvent(event);

        IUserDataObject udo = this.serviceCollection.storageManager().getOrCreateUserOnThread(home.getOwnersUniqueId());
        Map<String, LocationNode> m = udo.get(HomeKeys.HOMES).orElseGet(ImmutableMap::of);
        if (!setHome(home.getOwnersUniqueId(), m, home.getName(), location, rotation, true)) {
            throw new HomeException(
                    this.serviceCollection.messageProvider().getMessageFor(
                            Util.getSourceFromCause(cause),
                            "command.sethome.seterror",
                            home.getName()
                    ),
                    HomeException.Reasons.UNKNOWN);
        }

    }

    @Override public void removeHome(Cause cause, Home home) throws HomeException {
        // Preconditions.checkState(cause.root() instanceof PluginContainer, "The root must be a PluginContainer");
        removeHomeInternal(cause, home);
    }

    public void removeHomeInternal(Cause cause, Home home) throws HomeException {
        DeleteHomeEvent event = new DeleteHomeEvent(cause, home);
        postEvent(event);

        IUserDataObject udo = this.serviceCollection.storageManager().getOrCreateUserOnThread(home.getOwnersUniqueId());
        Map<String, LocationNode> m = udo.get(HomeKeys.HOMES).orElseGet(ImmutableMap::of);
        if (!deleteHome(home.getOwnersUniqueId(), m, home.getName())) {
                throw new HomeException(
                        this.serviceCollection.messageProvider().getMessageFor(
                                Util.getSourceFromCause(cause),
                                "command.home.delete.fail",
                                home.getName()),
                        HomeException.Reasons.UNKNOWN);
        }
    }

    @Override public int getMaximumHomes(UUID uuid) throws IllegalArgumentException {
        Optional<User> user = Sponge.getServiceManager().provideUnchecked(UserStorageService.class).get(uuid);
        if (!user.isPresent()) {
            throw new IllegalArgumentException("user does not exist.");
        }

        return getMaximumHomes(user.get());
    }

    @Override public int getMaximumHomes(User src) {
        IPermissionService permissionService = this.serviceCollection.permissionService();
        if (permissionService.hasPermission(src, HomePermissions.HOMES_UNLIMITED)) {
            return Integer.MAX_VALUE;
        }

        //noinspection deprecation
        return Math.max(permissionService.getPositiveIntOptionFromSubject(src, NucleusHomeService.HOME_COUNT_OPTION,
                NucleusHomeService.ALTERNATIVE_HOME_COUNT_OPTION)
                .orElse(1), 1);
    }

    public TeleportResult warpToHome(Player src, Home home, boolean safeTeleport) throws HomeException {
        Sponge.getServer().loadWorld(home.getWorldProperties()
                .orElseThrow(() ->
                        new HomeException(
                                this.serviceCollection.messageProvider().getMessageFor(src, "command.home.invalid", home.getName()),
                                HomeException.Reasons.INVALID_LOCATION
                        )));

        Location<World> targetLocation = home.getLocation().orElseThrow((() ->
                        new HomeException(
                                this.serviceCollection.messageProvider().getMessageFor(src, "command.home.invalid", home.getName()),
                                HomeException.Reasons.INVALID_LOCATION
                        )));
                // ReturnMessageException.fromKey("command.home.invalid", home.getName()));

        try (CauseStackManager.StackFrame frame = Sponge.getCauseStackManager().pushCauseFrame()) {
            frame.pushCause(src);
            postEvent(new UseHomeEvent(frame.getCurrentCause(), src, home));
        }

        INucleusTeleportService teleportService = this.serviceCollection.teleportService();
        TeleportHelperFilter filter = teleportService.getAppropriateFilter(src, safeTeleport);

        return teleportService.teleportPlayer(
                        src,
                        targetLocation,
                        home.getRotation(),
                        false,
                        TeleportScanners.NO_SCAN.get(),
                        filter
                );
    }

    private void postEvent(AbstractHomeEvent event) throws HomeException {
        if (Sponge.getEventManager().post(event)) {
            throw new HomeException(event.getCancelMessage().orElseGet(() ->
                    this.serviceCollection.messageProvider().getMessageFor(
                            event.getCause().first(CommandSource.class).orElseGet(Sponge.getServer()::getConsole),
                            "nucleus.eventcancelled")),
                    HomeException.Reasons.PLUGIN_CANCELLED
            );
        }
    }

    private List<Home> getHomesFrom(UUID uuid, Map<String, LocationNode> msln) {
        ImmutableList.Builder<Home> i = ImmutableList.builder();
        for (Map.Entry<String, LocationNode> entry : msln.entrySet()) {
            i.add(getHomeFrom(entry.getKey(), uuid, entry.getValue()));
        }

        return i.build();
    }

    private Home getHomeFrom(String string, UUID user, LocationNode node) {
        return new NucleusHome(string, user, node);
    }

    private Optional<Home> getHome(String home, UUID uuid, @Nullable Map<String, LocationNode> homeData) {
        if (homeData == null) {
            return Optional.empty();
        }
        return Util.getValueIgnoreCase(homeData, home).map(x -> getHomeFrom(home, uuid, x));
    }

    private boolean setHome(UUID uuid, Map<String, LocationNode> m, String home, Location<World> location, Vector3d rotation, boolean overwrite) {
        final Pattern warpName = Pattern.compile("^[a-zA-Z][a-zA-Z0-9]{1,15}$");

        if (m == null) {
            m = Maps.newHashMap();
        } else {
            m = Maps.newHashMap(m);
        }

        Optional<String> os = Util.getKeyIgnoreCase(m, home);
        if (os.isPresent() || !warpName.matcher(home).matches()) {
            if (!overwrite || !deleteHome(m, home)) {
                return false;
            }
        }

        m.put(home, new LocationNode(location, rotation));
        setAndSave(uuid, m);
        return true;
    }

    private boolean deleteHome(Map<String, LocationNode> m, String home) {
        if (m == null || m.isEmpty()) {
            return false;
        }

        Optional<String> os = Util.getKeyIgnoreCase(m, home);
        if (os.isPresent()) {
            m.remove(os.get());
            return true;
        }

        return false;
    }

    private boolean deleteHome(UUID uuid, Map<String, LocationNode> m, String home) {
        if (m == null || m.isEmpty()) {
            return false;
        }

        Optional<String> os = Util.getKeyIgnoreCase(m, home);
        if (os.isPresent()) {
            m = Maps.newHashMap(m);
            m.remove(os.get());
            setAndSave(uuid, m);
            return true;
        }

        return false;
    }

    private void setAndSave(UUID uuid, Map<String, LocationNode> map) {
        this.serviceCollection.storageManager().getUserService().setAndSave(uuid, HomeKeys.HOMES, map);
    }
}
