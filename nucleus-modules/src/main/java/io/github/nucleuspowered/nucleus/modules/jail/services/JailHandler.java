/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.jail.services;

import com.flowpowered.math.vector.Vector3d;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import io.github.nucleuspowered.nucleus.Util;
import io.github.nucleuspowered.nucleus.api.EventContexts;
import io.github.nucleuspowered.nucleus.api.core.exception.NoSuchLocationException;
import io.github.nucleuspowered.nucleus.api.module.jail.NucleusJailService;
import io.github.nucleuspowered.nucleus.api.module.jail.data.Jailing;
import io.github.nucleuspowered.nucleus.api.teleport.data.TeleportScanners;
import io.github.nucleuspowered.nucleus.api.util.data.NamedLocation;
import io.github.nucleuspowered.nucleus.datatypes.EndTimestamp;
import io.github.nucleuspowered.nucleus.datatypes.LocationData;
import io.github.nucleuspowered.nucleus.modules.fly.FlyKeys;
import io.github.nucleuspowered.nucleus.modules.jail.JailKeys;
import io.github.nucleuspowered.nucleus.modules.jail.data.JailData;
import io.github.nucleuspowered.nucleus.modules.jail.events.JailEvent;
import io.github.nucleuspowered.nucleus.scaffold.service.ServiceBase;
import io.github.nucleuspowered.nucleus.scaffold.service.annotations.APIService;
import io.github.nucleuspowered.nucleus.services.INucleusServiceCollection;
import io.github.nucleuspowered.nucleus.services.impl.storage.dataobjects.modular.IGeneralDataObject;
import io.github.nucleuspowered.nucleus.services.impl.storage.dataobjects.modular.IUserDataObject;
import io.github.nucleuspowered.nucleus.services.interfaces.IMessageProviderService;
import io.github.nucleuspowered.nucleus.services.interfaces.INucleusTeleportService;
import io.github.nucleuspowered.nucleus.services.interfaces.IStorageManager;
import io.github.nucleuspowered.nucleus.util.CauseStackHelper;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.event.CauseStackManager;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.service.context.Context;
import org.spongepowered.api.service.context.ContextCalculator;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.text.serializer.TextSerializers;
import org.spongepowered.api.util.annotation.NonnullByDefault;
import org.spongepowered.api.world.Locatable;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;
import org.spongepowered.api.world.storage.WorldProperties;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Nullable;
import javax.inject.Inject;

@NonnullByDefault
@APIService(NucleusJailService.class)
public class JailHandler implements NucleusJailService, ContextCalculator<Subject>, ServiceBase {

    @Nullable private Map<String, NamedLocation> jailLocations = null;
    private final IStorageManager storageManager;
    private final INucleusTeleportService teleportService;
    private final IMessageProviderService messageProviderService;

    // Used for the context calculator
    private final Map<UUID, Context> jailDataCache = Maps.newHashMap();
    private final static Context jailContext = new Context(NucleusJailService.JAILED_CONTEXT, "true");
    private final PluginContainer pluginContainer;

    @Inject
    public JailHandler(INucleusServiceCollection serviceCollection) {
        this.storageManager = serviceCollection.storageManager();
        this.teleportService = serviceCollection.teleportService();
        this.messageProviderService = serviceCollection.messageProvider();
        this.pluginContainer = serviceCollection.pluginContainer();
    }

    public Map<String, NamedLocation> getJailLocations() {
        if (this.jailLocations == null) {
            updateCache();
        }

        return this.jailLocations;
    }

    public void updateCache() {
        this.jailLocations = new HashMap<>();
        IGeneralDataObject dataObject = this.storageManager.getGeneralService().getOrNewOnThread();

        Map<String, NamedLocation> jails = dataObject.get(JailKeys.JAILS).orElseGet(HashMap::new);
        jails.forEach((k, v) -> this.jailLocations.put(k.toLowerCase(), v));
    }

    public void saveFromCache() {
        if (this.jailLocations == null) {
            return; // not loaded
        }

        IGeneralDataObject dataObject = this.storageManager.getGeneralService().getOrNewOnThread();
        dataObject.set(JailKeys.JAILS, new HashMap<>(this.jailLocations));
        this.storageManager.getGeneralService().save(dataObject);
    }

    @Override
    public Optional<NamedLocation> getJail(String warpName) {
        return Optional.ofNullable(getJailLocations().get(warpName.toLowerCase()));
    }

    @Override
    public boolean removeJail(String warpName) {
        return getJailLocations().remove(warpName.toLowerCase()) != null;
    }

    @Override
    public boolean setJail(String warpName, Location<World> location, Vector3d rotation) {
        Map<String, NamedLocation> locationMap = getJailLocations();
        if (locationMap.containsKey(warpName.toLowerCase())) {
            return false;
        }

        locationMap.put(warpName.toLowerCase(), new LocationData(
                warpName,
                location.getExtent().getUniqueId(),
                location.getPosition(),
                rotation
        ));
        saveFromCache();
        return true;
    }

    @Override
    public Map<String, NamedLocation> getJails() {
        return ImmutableMap.copyOf(getJailLocations());
    }

    public boolean isPlayerJailedCached(User user) {
        return this.jailDataCache.containsKey(user.getUniqueId());
    }

    @Override
    public boolean isPlayerJailed(User user) {
        return getPlayerJailDataInternal(user).isPresent();
    }

    @Override
    public Optional<Jailing> getPlayerJailData(User user) {
        return getPlayerJailDataInternal(user).map(x -> x);
    }

    public Optional<JailData> getPlayerJailDataInternal(User user) {
        try {
            Optional<JailData> data = this.storageManager.getUserOnThread(user.getUniqueId())
                    .flatMap(y -> y.get(JailKeys.JAIL_DATA));
            if (data.isPresent()) {
                this.jailDataCache.put(user.getUniqueId(), new Context(NucleusJailService.JAIL_CONTEXT, data.get().getJailName()));
            } else {
                this.jailDataCache.put(user.getUniqueId(), null);
            }

            return data;
        } catch (Exception e) {
            e.printStackTrace();
            return Optional.empty();
        }
    }

    public boolean shouldJailOnNextLogin(User user) {
        return this.storageManager.getOrCreateUserOnThread(user.getUniqueId()).get(JailKeys.JAIL_ON_NEXT_LOGIN).orElse(false);
    }

    public void setJailOnNextLogin(User user, boolean r) {
        IUserDataObject u = this.storageManager.getOrCreateUserOnThread(user.getUniqueId());
        u.set(JailKeys.JAIL_ON_NEXT_LOGIN, r);
        this.storageManager.saveUser(user.getUniqueId(), u);
    }

    @Override
    public boolean jailPlayer(User victim, String jail, CommandSource jailer, String reason) throws NoSuchLocationException {
        Preconditions.checkNotNull(victim);
        Preconditions.checkNotNull(jail);
        Preconditions.checkNotNull(jailer);
        Preconditions.checkNotNull(reason);
        NamedLocation location = getJail(jail).orElseThrow(NoSuchLocationException::new);
        return jailPlayer(victim,
                new JailData(Util.getUUID(jailer), location.getName(), reason, victim.getPlayer().map(Locatable::getLocation).orElse(null)));
    }

    public boolean jailPlayer(User user, JailData data) {
        IUserDataObject udo = this.storageManager.getOrCreateUserOnThread(user.getUniqueId());
        if (udo.get(JailKeys.JAIL_DATA).isPresent()) {
            return false;
        }

        // Get the jail.
        Optional<NamedLocation> owl = getJail(data.getJailName());
        NamedLocation wl = owl.filter(x -> x.getTransform().isPresent()).orElseGet(() -> {
            if (!getJails().isEmpty()) {
                return null;
            }

            return getJails().entrySet().stream().findFirst().get().getValue();
        });

        if (wl == null) {
            return false;
        }

        udo.set(JailKeys.JAIL_DATA, data);
        if (user.isOnline()) {
            Sponge.getScheduler().createSyncExecutor(this.pluginContainer).execute(() -> {
                try (CauseStackManager.StackFrame frame = Sponge.getCauseStackManager().pushCauseFrame()) {
                    frame.addContext(EventContexts.IS_JAILING_ACTION, true);
                    Player player = user.getPlayer().get();
                            this.teleportService.teleportPlayerSmart(
                                    player,
                                    owl.get().getTransform().get(), // The transform exists.
                                    true,
                                    false,
                                    TeleportScanners.NO_SCAN.get()
                            );
                    player.offer(Keys.IS_FLYING, false);
                    player.offer(Keys.CAN_FLY, false);
                    udo.set(FlyKeys.FLY_TOGGLE, false);
                }
            });
        } else {
            udo.set(JailKeys.JAIL_ON_NEXT_LOGIN, true);
        }

        this.storageManager.saveUser(user.getUniqueId(), udo);
        this.jailDataCache.put(user.getUniqueId(), new Context(NucleusJailService.JAIL_CONTEXT, data.getJailName()));
        this.storageManager.saveUser(user.getUniqueId(), udo);

        Sponge.getEventManager().post(new JailEvent.Jailed(
                user,
                CauseStackHelper.createCause(Util.getObjectFromUUID(data.getJailerInternal())),
                data.getJailName(),
                TextSerializers.FORMATTING_CODE.deserialize(data.getReason()),
                data.getRemainingTime().orElse(null)));

        return true;
    }

    public void updateJailData(User user, JailData data) {
        IUserDataObject udo = this.storageManager.getOrCreateUserOnThread(user.getUniqueId());
        udo.set(JailKeys.JAIL_DATA, data);
        this.storageManager.saveUser(user.getUniqueId(), udo);
    }

    // Test
    @Override
    public boolean unjailPlayer(User user) {
        return unjailPlayer(user, Sponge.getCauseStackManager().getCurrentCause());
    }

    public boolean unjailPlayer(User user, Cause cause) {
        IUserDataObject udo = this.storageManager.getOrCreateUser(user.getUniqueId()).join();
        Optional<JailData> ojd = udo.get(JailKeys.JAIL_DATA);
        if (!ojd.isPresent()) {
            return false;
        }

        Optional<Location<World>> ow = ojd.get().getPreviousLocation();
        this.jailDataCache.put(user.getUniqueId(), null);
        if (user.isOnline()) {
            Player player = user.getPlayer().get();
            Sponge.getScheduler().createSyncExecutor(this.pluginContainer).execute(() -> {
                this.teleportService.setLocation(player, ow.orElseGet(() -> player.getWorld().getSpawnLocation()));
                this.messageProviderService.sendMessageTo(player, "jail.elapsed");

                // Remove after the teleport for the back data.
                udo.remove(JailKeys.JAIL_DATA);
                udo.remove(JailKeys.JAIL_ON_NEXT_LOGIN);
            });
        } else {
            if (ow.isPresent()) {
                Location<World> l = ow.get();
                user.setLocation(l.getPosition(), l.getExtent().getUniqueId());
            } else {
                WorldProperties w = Sponge.getServer().getDefaultWorld().get();
                user.setLocation(
                        w.getSpawnPosition().toDouble(), w.getUniqueId()
                );
            }

            udo.remove(JailKeys.JAIL_DATA);
        }

        this.storageManager.saveUser(user.getUniqueId(), udo);

        Sponge.getEventManager().post(new JailEvent.Unjailed(user, cause));
        return true;
    }

    public Optional<NamedLocation> getWarpLocation(User user) {
        if (!isPlayerJailed(user)) {
            return Optional.empty();
        }

        Optional<NamedLocation> owl = getJail(getPlayerJailDataInternal(user).get().getJailName());
        if (!owl.isPresent()) {
            Collection<NamedLocation> wl = getJails().values();
            if (wl.isEmpty()) {
                return Optional.empty();
            }

            owl = wl.stream().findFirst();
        }

        return owl;
    }

    @Override public void accumulateContexts(Subject calculable, Set<Context> accumulator) {
        if (calculable instanceof User) {
            UUID c = ((User) calculable).getUniqueId();
            if (!this.jailDataCache.containsKey(c)) {
                getPlayerJailDataInternal((User) calculable);
            }

            Context co = this.jailDataCache.get(c);
            if (co != null) {
                accumulator.add(co);
                accumulator.add(jailContext);
            }
        }
    }

    @Override public boolean matches(Context context, Subject subject) {
        if (context.getKey().equals(NucleusJailService.JAIL_CONTEXT)) {
            if (subject instanceof User) {
                UUID u = ((User) subject).getUniqueId();
                return context.equals(this.jailDataCache.get(u));
            }
        } else if (context.getKey().equals(NucleusJailService.JAILED_CONTEXT)) {
            if (subject instanceof User) {
                UUID u = ((User) subject).getUniqueId();
                return this.jailDataCache.get(u) != null;
            }
        }

        return false;
    }

    public boolean checkJail(final User player, boolean sendMessage) {
        // if the jail doesn't exist, treat it as expired.
        if (!getPlayerJailDataInternal(player).map(EndTimestamp::expired).orElse(true)) {
            if (sendMessage) {
                IUserDataObject udo = this.storageManager.getOrCreateUserOnThread(player.getUniqueId());
                udo.set(FlyKeys.FLY_TOGGLE, false);
                player.offer(Keys.CAN_FLY, false);
                player.offer(Keys.IS_FLYING, false);
                this.storageManager.saveUser(player.getUniqueId(), udo);
                player.getPlayer().ifPresent(this::onJail);
            }

            return true;
        }

        return false;
    }

    private void onJail(Player user) {
        getPlayerJailDataInternal(user).ifPresent(x -> onJail(x, user));
    }

    public void onJail(JailData md, Player user) {
        if (md.getEndTimestamp().isPresent()) {
            this.messageProviderService.sendMessageTo(
                    user, "jail.playernotify.time",
                    this.messageProviderService.getTimeString(user.getLocale(), Instant.now().until(md.getEndTimestamp().get(), ChronoUnit.SECONDS))
            );
        } else {
            this.messageProviderService.sendMessageTo(user, "jail.playernotify.standard");
        }

        this.messageProviderService.sendMessageTo(user,"standard.reasoncoloured", md.getReason());
    }
}
