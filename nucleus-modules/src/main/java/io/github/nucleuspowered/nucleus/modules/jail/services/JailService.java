/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.jail.services;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import io.github.nucleuspowered.nucleus.core.Util;
import io.github.nucleuspowered.nucleus.api.EventContexts;
import io.github.nucleuspowered.nucleus.api.module.jail.NucleusJailService;
import io.github.nucleuspowered.nucleus.api.module.jail.data.Jail;
import io.github.nucleuspowered.nucleus.api.module.jail.data.Jailing;
import io.github.nucleuspowered.nucleus.api.teleport.data.TeleportScanners;
import io.github.nucleuspowered.nucleus.api.util.data.NamedLocation;
import io.github.nucleuspowered.nucleus.modules.jail.JailKeys;
import io.github.nucleuspowered.nucleus.modules.jail.config.JailConfig;
import io.github.nucleuspowered.nucleus.modules.jail.events.JailEvent;
import io.github.nucleuspowered.nucleus.core.services.INucleusServiceCollection;
import io.github.nucleuspowered.nucleus.core.services.interfaces.IMessageProviderService;
import io.github.nucleuspowered.nucleus.core.services.interfaces.IPlayerDisplayNameService;
import io.github.nucleuspowered.nucleus.core.services.interfaces.IReloadableService;
import io.vavr.control.Either;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.event.Cause;
import org.spongepowered.api.event.CauseStackManager;
import org.spongepowered.api.util.Identifiable;
import org.spongepowered.api.world.server.ServerLocation;
import org.spongepowered.api.world.server.ServerWorld;
import org.spongepowered.api.world.storage.WorldProperties;
import org.spongepowered.api.world.teleport.TeleportHelperFilters;
import org.spongepowered.math.vector.Vector3d;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class JailService implements NucleusJailService, IReloadableService.DataLocationReloadable, IReloadableService.Reloadable {

    public static final Jailing NOT_JAILED = new Jailing() {
        @Override public String getReason() {
            return null;
        }

        @Override public String getJailName() {
            return null;
        }

        @Override public Optional<UUID> getJailer() {
            return Optional.empty();
        }

        @Override public Optional<ServerLocation> getPreviousLocation() {
            return Optional.empty();
        }

        @Override public Optional<Instant> getCreationInstant() {
            return Optional.empty();
        }

        @Override public Optional<Duration> getRemainingTime() {
            return Optional.empty();
        }

        @Override public boolean expired() {
            return false;
        }

        @Override public boolean isCurrentlyTicking() {
            return false;
        }
    };

    private boolean isPopulated = false;
    private boolean isOnlineOnly = false;

    private final INucleusServiceCollection serviceCollection;
    private final Map<String, Jail> jails = new HashMap<>();
    private final LoadingCache<UUID, Jailing> jailings;

    public JailService(final INucleusServiceCollection serviceCollection) {
        this.serviceCollection = serviceCollection;
        this.jailings = Caffeine.newBuilder()
                .build(key -> this.serviceCollection.storageManager().getOrCreateUserOnThread(key).get(JailKeys.JAIL_DATA)
                        .<Jailing>map(data -> JailingEntry.fromJailingData(key, data, this.isOnlineOnly))
                        .orElse(JailService.NOT_JAILED));
    }

    @Override
    public Optional<Jail> setJail(final String name, final ServerLocation location, final Vector3d rotation) {
        this.populateJails();
        return Optional.empty();
    }

    @Override
    public Map<String, Jail> getJails() {
        this.populateJails();
        return Collections.unmodifiableMap(this.jails);
    }

    @Override
    public Optional<Jail> getJail(final String name) {
        this.populateJails();
        return Optional.ofNullable(this.jails.get(name));
    }

    @Override
    public boolean removeJail(final String name) {
        this.populateJails();
        return false;
    }

    @Override
    public boolean isPlayerJailed(final UUID user) {
        final Jailing j = this.jailings.get(user);
        return j != null && j != JailService.NOT_JAILED;
    }

    @Override
    public Optional<Jailing> getPlayerJailData(final UUID uuid) {
        final Jailing jailing = this.jailings.get(uuid);
        if (jailing == null || jailing == JailService.NOT_JAILED) {
            return Optional.empty();
        }
        return Optional.of(jailing);
    }

    @Override
    public boolean jailPlayer(final UUID victim, final Jail jail, final String reason, @Nullable final Duration duration) {
        this.populateJails();
        if (this.isPlayerJailed(victim)) {
            return false;
        }

        final ServerLocation location = jail.getLocation().orElseThrow(() -> new IllegalArgumentException("Jail does not have a valid location."));
        if (location.isValid()) {
            this.serviceCollection.schedulerService().runOnMainThread(() -> Sponge.server().worldManager().loadWorld(location.worldKey())).join();
        } else {
            throw new IllegalArgumentException("Jail does not have a valid location.");
        }

        final Cause cause = Sponge.server().causeStackManager().currentCause();

        // Create the jailing
        final JailingEntry jailingEntry = JailingEntry.fromJailingRequest(
                victim,
                reason,
                jail.getName(),
                cause.first(ServerPlayer.class).map(Identifiable::uniqueId).orElse(null),
                this.eitherToLocation(this.getUserEither(victim)),
                Instant.now(),
                duration);
        this.jailings.put(victim, jailingEntry);
        this.serviceCollection.storageManager().getUserService().setAndSave(victim, JailKeys.JAIL_DATA, jailingEntry.asJailData(this.isOnlineOnly));
        // Time to jail
        final Optional<ServerPlayer> serverPlayer = Sponge.server().player(victim);
        if (serverPlayer.isPresent()) {
            final ServerPlayer player = serverPlayer.get();
            this.serviceCollection.schedulerService().runOnMainThread(() -> {
                try (final CauseStackManager.StackFrame frame = Sponge.server().causeStackManager().pushCauseFrame()) {
                    frame.addContext(EventContexts.IS_JAILING_ACTION, true);
                    player.setLocation(location);
                    player.setRotation(jail.getRotation());
                    player.offer(Keys.IS_FLYING, false);
                    player.offer(Keys.CAN_FLY, false);
                    this.onJail(jailingEntry, player);
                }
            });
        }

        Sponge.eventManager().post(new JailEvent.Jailed(
                victim,
                cause,
                jail.getName(),
                LegacyComponentSerializer.legacySection().deserialize(reason),
                jailingEntry.getRemainingTime().orElse(null)));

        return true;
    }

    @Override
    public boolean unjailPlayer(final UUID user) {
        this.populateJails();
        final Optional<Jailing> o = this.getPlayerJailData(user);
        if (!o.isPresent()) {
            return false;
        }

        final Jailing jailing = o.get();
        final Either<ServerPlayer, User> either = Sponge.server().player(user)
                .<Either<ServerPlayer, User>>map(Either::left)
                .orElseGet(() -> this.getUserEither(user));

        final ServerLocation destination = jailing.getPreviousLocation().orElseGet(() -> either.fold(player -> {
            final ServerWorld world = player.world();
            return ServerLocation.of(world.key(), world.properties().spawnPosition());
        }, u -> {
            final ServerWorld def = Sponge.server().worldManager().defaultWorld();
            if (u == null) {
                return ServerLocation.of(def.key(), def.properties().spawnPosition());
            }
            final ServerWorld target =
                    Sponge.server().worldManager().world(u.worldKey()).orElse(def);
            return ServerLocation.of(target.key(), target.properties().spawnPosition());
        }));

        this.jailings.put(user, JailService.NOT_JAILED);
        this.serviceCollection.storageManager().getUserService().removeAndSave(user, JailKeys.JAIL_DATA);
        this.serviceCollection.schedulerService().runOnMainThread(() -> {
            final ServerLocation serverLocation = this.serviceCollection.teleportService().getSafeLocation(
                    destination,
                    TeleportScanners.NO_SCAN.get(),
                    TeleportHelperFilters.DEFAULT.get()
            ).orElseGet(() -> {
                final ServerWorld def = Sponge.server().worldManager().defaultWorld();
                return ServerLocation.of(def.key(), def.properties().spawnPosition());
            });

            if (either.isLeft()) {
                either.getLeft().setLocation(serverLocation);
                this.serviceCollection.messageProvider().sendMessageTo(either.getLeft(), "jail.elapsed");
            } else if (either.get() != null) {
                either.get().setLocation(serverLocation.worldKey(), serverLocation.position());
            }
        });

        // Return player to the specified location
        Sponge.eventManager().post(new JailEvent.Unjailed(user, Sponge.server().causeStackManager().currentCause()));
        return true;
    }

    public void clearCacheFor(final UUID player) {
        final Jailing jailData = this.jailings.get(player);
        if (jailData == JailService.NOT_JAILED) {
            this.serviceCollection.storageManager().getUserService().removeAndSave(player, JailKeys.JAIL_DATA);
        } if (jailData instanceof JailingEntry) {
            this.serviceCollection.storageManager().getUserService().setAndSave(player, JailKeys.JAIL_DATA, ((JailingEntry) jailData).asJailData(this.isOnlineOnly));
        }
        this.jailings.invalidate(player);
    }

    public Jailing onPlayerLogin(final UUID player) {
        this.jailings.refresh(player);
        return this.jailings.get(player);
    }

    @Override
    public void onDataFileLocationChange(final INucleusServiceCollection serviceCollection) {
        this.jails.clear();
        this.jailings.invalidateAll();
        this.isPopulated = false;
    }

    private void populateJails() {
        if (!this.isPopulated) {
            synchronized (this) {
                if (!this.isPopulated) {
                    this.jails.clear();
                    for (final Map.Entry<String, NamedLocation> entry :
                            this.serviceCollection.storageManager().getGeneral().get(JailKeys.JAILS).orElseGet(Collections::emptyMap).entrySet()) {
                        this.jails.put(entry.getKey(), new JailLocationEntry(entry.getValue()));
                    }
                    this.isPopulated = true;
                }
            }
        }
    }

    public void onJail(final JailingEntry entry, final ServerPlayer serverPlayer) {
        final IMessageProviderService messageProviderService = this.serviceCollection.messageProvider();
        final IPlayerDisplayNameService playerDisplayNameService = this.serviceCollection.playerDisplayNameService();
        try (final CauseStackManager.StackFrame frame = Sponge.server().causeStackManager().pushCauseFrame()) {
            frame.addContext(EventContexts.IS_JAILING_ACTION, true);
            // It exists.
            final Optional<Duration> timeLeft = entry.getRemainingTime();
            final Component message = timeLeft.map(duration ->
                    messageProviderService.getMessageFor(
                            serverPlayer,
                            "command.jail.jailedfor",
                            entry.getJailName(),
                            playerDisplayNameService.getDisplayName(entry.getJailer().orElse(Util.CONSOLE_FAKE_UUID)),
                            messageProviderService.getTimeString(serverPlayer.locale(), duration.getSeconds()))
            )
                    .orElseGet(() -> messageProviderService.getMessageFor(serverPlayer, "command.jail.jailedperm", entry.getJailName(),
                            playerDisplayNameService.getDisplayName(entry.getJailer().orElse(Util.CONSOLE_FAKE_UUID)), "", ""));

            serverPlayer.sendMessage(message);
            messageProviderService.sendMessageTo(serverPlayer, "standard.reasoncoloured", entry.getReason());
        }
    }

    public void notify(final ServerPlayer user) {
        this.getPlayerJailData(user.uniqueId()).ifPresent(entry -> {
            final IMessageProviderService messageProviderService = this.serviceCollection.messageProvider();
            final Optional<Duration> timeLeft = entry.getRemainingTime();
            if (timeLeft.isPresent()) {
                messageProviderService.sendMessageTo(
                        user, "jail.playernotify.time",
                        messageProviderService.getTimeString(user.locale(), timeLeft.get().getSeconds())
                );
            } else {
                messageProviderService.sendMessageTo(user, "jail.playernotify.standard");
            }

            messageProviderService.sendMessageTo(user,"standard.reasoncoloured",
                    LegacyComponentSerializer.legacyAmpersand().deserialize(entry.getReason()));
        });

    }

    private Either<ServerPlayer, User> getUserEither(final UUID uuid) {
        return Sponge.server().userManager().load(uuid).join().<Either<ServerPlayer, User>>map(Either::right).orElse(null);
    }

    private ServerLocation eitherToLocation(final Either<ServerPlayer, User> either) {
        if (either.isRight()) {
            return ServerLocation.of(either.get().worldKey(), either.get().position());
        } else {
            return either.getLeft().serverLocation();
        }
    }

    public void checkExpiry() {
        for (final ServerPlayer uuid : Sponge.server().onlinePlayers()) {
            final Jailing jailing = this.jailings.get(uuid.uniqueId());
            if (jailing != null && jailing != JailService.NOT_JAILED && jailing.expired()) {
                this.serviceCollection.schedulerService().runOnMainThread(() -> this.unjailPlayer(uuid.uniqueId()));
            }
        }
    }

    public Optional<Jail> getPlayerJail(final UUID uniqueId) {
        return this.getPlayerJailData(uniqueId).flatMap(x -> this.getJail(x.getJailName()));
    }

    @Override
    public void onReload(final INucleusServiceCollection serviceCollection) {
        this.isOnlineOnly = serviceCollection.configProvider().getModuleConfig(JailConfig.class).isJailOnlineOnly();
    }
}
