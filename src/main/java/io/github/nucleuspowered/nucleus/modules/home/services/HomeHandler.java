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
import io.github.nucleuspowered.nucleus.Nucleus;
import io.github.nucleuspowered.nucleus.Util;
import io.github.nucleuspowered.nucleus.api.exceptions.NucleusException;
import io.github.nucleuspowered.nucleus.api.nucleusdata.Home;
import io.github.nucleuspowered.nucleus.api.service.NucleusHomeService;
import io.github.nucleuspowered.nucleus.configurate.datatypes.LocationNode;
import io.github.nucleuspowered.nucleus.internal.annotations.APIService;
import io.github.nucleuspowered.nucleus.internal.interfaces.ServiceBase;
import io.github.nucleuspowered.nucleus.internal.traits.IDataManagerTrait;
import io.github.nucleuspowered.nucleus.internal.traits.MessageProviderTrait;
import io.github.nucleuspowered.nucleus.internal.traits.PermissionTrait;
import io.github.nucleuspowered.nucleus.modules.home.HomeKeys;
import io.github.nucleuspowered.nucleus.modules.home.commands.SetHomeCommand;
import io.github.nucleuspowered.nucleus.modules.home.events.AbstractHomeEvent;
import io.github.nucleuspowered.nucleus.modules.home.events.CreateHomeEvent;
import io.github.nucleuspowered.nucleus.modules.home.events.DeleteHomeEvent;
import io.github.nucleuspowered.nucleus.modules.home.events.ModifyHomeEvent;
import io.github.nucleuspowered.nucleus.storage.dataobjects.modular.IUserDataObject;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.service.user.UserStorageService;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

import javax.annotation.Nullable;

@APIService(NucleusHomeService.class)
public class HomeHandler implements NucleusHomeService, MessageProviderTrait, PermissionTrait, ServiceBase, IDataManagerTrait {

    private final String unlimitedPermission
            = Nucleus.getNucleus().getPermissionRegistry().getPermissionsForNucleusCommand(SetHomeCommand.class).getPermissionWithSuffix("unlimited");

    @Override public List<Home> getHomes(UUID user) {
        Optional<IUserDataObject> service = getUser(user).join(); //.get().getHome;
        return service.map(modularUserService -> getHomes(user, modularUserService)).orElseGet(ImmutableList::of);

    }

    private List<Home> getHomes(UUID user, IUserDataObject userDataObject) {
        return getHomesFrom(user, userDataObject.get(HomeKeys.HOMES).orElseGet(ImmutableMap::of));
    }

    public Collection<String> getHomeNames(UUID user) {
        return getUserOnThread(user).flatMap(x -> x.get(HomeKeys.HOMES).map(Map::keySet)).orElseGet(ImmutableSet::of);
    }

    @Override public Optional<Home> getHome(UUID user, String name) {
        Optional<IUserDataObject> service = getUser(user).join();
        return service.flatMap(modularUserService -> getHome(name, user, modularUserService.get(HomeKeys.HOMES).orElse(null)));

    }

    @Override public void createHome(Cause cause, User user, String name, Location<World> location, Vector3d rotation) throws NucleusException {
        // Preconditions.checkState(cause.root() instanceof PluginContainer, "The root must be a PluginContainer");
        createHomeInternal(cause, user, name, location, rotation);
    }

    public void createHomeInternal(Cause cause, User user, String name, Location<World> location, Vector3d rotation) throws NucleusException {
        if (!NucleusHomeService.HOME_NAME_PATTERN.matcher(name).matches()) {
            throw new NucleusException(
                    Nucleus.getNucleus().getMessageProvider().getTextMessageWithFormat("command.sethome.name"),
                NucleusException.ExceptionType.DISALLOWED_NAME);
        }

        int max = getMaximumHomes(user);
        IUserDataObject udo = getOrCreateUserOnThread(user.getUniqueId());
        Map<String, LocationNode> m = udo.get(HomeKeys.HOMES).orElseGet(ImmutableMap::of);
        if (m.size() >= max) {
            throw new NucleusException(
                    Nucleus.getNucleus().getMessageProvider().getTextMessageWithFormat("command.sethome.limit", String.valueOf(max)),
                NucleusException.ExceptionType.LIMIT_REACHED);
        }

        CreateHomeEvent event = new CreateHomeEvent(name, user, cause, location);
        postEvent(event);

        if (!setHome(m, name, location, rotation, false, udo)) {
            throw new NucleusException(
                    Nucleus.getNucleus().getMessageProvider().getTextMessageWithFormat("command.sethome.seterror", name),
                    NucleusException.ExceptionType.UNKNOWN_ERROR);
        }

    }

    @Override public void modifyHome(Cause cause, Home home, Location<World> location, Vector3d rotation) throws NucleusException {
        // Preconditions.checkState(cause.root() instanceof PluginContainer, "The root must be a PluginContainer");
        modifyHomeInternal(cause, home, location, rotation);
    }

    public void modifyHomeInternal(Cause cause, Home home, Location<World> location, Vector3d rotation) throws NucleusException {
        ModifyHomeEvent event = new ModifyHomeEvent(cause, home, location);
        postEvent(event);

        IUserDataObject udo = getOrCreateUserOnThread(home.getOwnersUniqueId());
        Map<String, LocationNode> m = udo.get(HomeKeys.HOMES).orElseGet(ImmutableMap::of);
        if (!setHome(m, home.getName(), location, rotation, false, udo)) {
            throw new NucleusException(
                    Nucleus.getNucleus().getMessageProvider().getTextMessageWithFormat("command.sethome.seterror", home.getName()),
                    NucleusException.ExceptionType.UNKNOWN_ERROR);
        }

    }

    @Override public void removeHome(Cause cause, Home home) throws NucleusException {
        // Preconditions.checkState(cause.root() instanceof PluginContainer, "The root must be a PluginContainer");
        removeHomeInternal(cause, home);
    }

    public void removeHomeInternal(Cause cause, Home home) throws NucleusException {
        DeleteHomeEvent event = new DeleteHomeEvent(cause, home);
        postEvent(event);

        IUserDataObject udo = getOrCreateUserOnThread(home.getOwnersUniqueId());
        Map<String, LocationNode> m = udo.get(HomeKeys.HOMES).orElseGet(ImmutableMap::of);
        if (!deleteHome(m, home.getName(), udo)) {
                throw new NucleusException(getMessage("command.home.delete.fail", home.getName()), NucleusException.ExceptionType.UNKNOWN_ERROR);
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
        if (hasPermission(src, this.unlimitedPermission)) {
            return Integer.MAX_VALUE;
        }

        return Math.max(Util.getPositiveIntOptionFromSubject(src, "home-count", "homes").orElse(1), 1);
    }

    private void postEvent(AbstractHomeEvent event) throws NucleusException {
        if (Sponge.getEventManager().post(event)) {
            throw new NucleusException(event.getCancelMessage().orElseGet(() ->
                    Nucleus.getNucleus().getMessageProvider().getTextMessageWithFormat("nucleus.eventcancelled")), NucleusException.ExceptionType.EVENT_CANCELLED
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

    private boolean setHome(Map<String, LocationNode> m, String home, Location<World> location, Vector3d rotation, boolean overwrite,
            IUserDataObject udo) {
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
        udo.set(HomeKeys.HOMES, m);
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

    private boolean deleteHome(Map<String, LocationNode> m, String home, IUserDataObject udo) {
        if (m == null || m.isEmpty()) {
            return false;
        }

        Optional<String> os = Util.getKeyIgnoreCase(m, home);
        if (os.isPresent()) {
            m = Maps.newHashMap(m);
            m.remove(os.get());
            udo.set(HomeKeys.HOMES, m);
            return true;
        }

        return false;
    }
}
