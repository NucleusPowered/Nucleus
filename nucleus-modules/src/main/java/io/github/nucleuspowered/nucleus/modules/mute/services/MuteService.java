/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.mute.services;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import io.github.nucleuspowered.nucleus.api.module.mute.NucleusMuteService;
import io.github.nucleuspowered.nucleus.api.module.mute.data.Mute;
import io.github.nucleuspowered.nucleus.api.util.data.TimedEntry;
import io.github.nucleuspowered.nucleus.modules.mute.MuteKeys;
import io.github.nucleuspowered.nucleus.modules.mute.config.MuteConfig;
import io.github.nucleuspowered.nucleus.modules.mute.events.MuteEvent;
import io.github.nucleuspowered.nucleus.core.services.INucleusServiceCollection;
import io.github.nucleuspowered.nucleus.core.services.interfaces.IMessageProviderService;
import io.github.nucleuspowered.nucleus.core.services.interfaces.IReloadableService;
import net.kyori.adventure.text.Component;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.persistence.DataContainer;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class MuteService implements NucleusMuteService, IReloadableService.DataLocationReloadable, IReloadableService.Reloadable {

    public static final Mute NOT_MUTED = new Mute() {
        @Override
        public int contentVersion() {
            return 0;
        }

        @Override
        public DataContainer toContainer() {
            return null;
        }

        @Override public String getReason() {
            return null;
        }

        @Override public Optional<UUID> getMuter() {
            return Optional.empty();
        }

        @Override public Optional<Instant> getCreationInstant() {
            return Optional.empty();
        }

        @Override
        public Optional<TimedEntry> getTimedEntry() {
            return Optional.empty();
        }

    };

    private boolean isOnlineOnly = false;

    private boolean globalMuteEnabled = false;
    private final List<UUID> voicedUsers = new ArrayList<>();

    private final INucleusServiceCollection serviceCollection;
    private final LoadingCache<UUID, Mute> mutes;

    public MuteService(final INucleusServiceCollection serviceCollection) {
        this.serviceCollection = serviceCollection;
        this.mutes = Caffeine.newBuilder()
                .build(key -> this.serviceCollection.storageManager().getOrCreateUserOnThread(key)
                        .get(MuteKeys.MUTE_DATA)
                        .map(x -> {
                            if (!this.isOnlineOnly || Sponge.server().player(key).isPresent()) {
                                return ((NucleusMute) x).start();
                            } else {
                                return ((NucleusMute) x).stop();
                            }
                        })
                        .orElse(MuteService.NOT_MUTED));
    }

    public void checkExpiry() {
        for (final ServerPlayer uuid : Sponge.server().onlinePlayers()) {
            final Mute mute = this.mutes.get(uuid.uniqueId());
            if (mute != null && mute != MuteService.NOT_MUTED && mute.getTimedEntry().map(TimedEntry::expired).orElse(false)) {
                this.serviceCollection.schedulerService().runOnMainThread(() -> this.unmutePlayer(uuid.uniqueId()));
            }
        }
    }

    @Override
    public void onReload(final INucleusServiceCollection serviceCollection) {
        this.isOnlineOnly = serviceCollection.configProvider().getModuleConfig(MuteConfig.class).isMuteOnlineOnly();
    }

    @Override
    public boolean isMuted(final UUID uuid) {
        final Mute mute = this.mutes.get(uuid);
        return mute != null && mute != MuteService.NOT_MUTED;
    }

    @Override
    public Optional<Mute> getPlayerMuteInfo(final UUID user) {
        final Mute mute = this.mutes.get(user);
        if (mute == null || mute == MuteService.NOT_MUTED) {
            return Optional.empty();
        }
        return Optional.of(mute);
    }

    @Override
    public boolean mutePlayer(final UUID user, final String reason, @Nullable final Duration duration) {
        if (this.isMuted(user)) {
            return false; // already muted
        }

        final Object root = Sponge.server().causeStackManager().currentCause().root();
        @Nullable final UUID uuid;
        if (root instanceof ServerPlayer) {
            uuid = ((ServerPlayer) root).uniqueId();
        } else {
            uuid = null;
        }
        final NucleusMute entry = NucleusMute.fromMutingRequest(user, reason, uuid, Instant.now(), duration, () -> this.isOnlineOnly);
        this.serviceCollection.storageManager().getUserService().setAndSave(user, MuteKeys.MUTE_DATA, entry);
        Sponge.eventManager().post(new MuteEvent.Muted(
                Sponge.server().causeStackManager().currentCause(),
                user,
                duration,
                Component.text(reason)
        ));

        Sponge.server().player(user).ifPresent(x -> {
            this.mutes.invalidate(user);
            this.mutes.put(user, entry);
            this.onMute(entry, x);
        });
        return true;
    }

    @Override
    public boolean unmutePlayer(final UUID uuid) {
        final Optional<Mute> mute = this.getPlayerMuteInfo(uuid);
        if (mute.isPresent()) {
            this.serviceCollection.storageManager().getUserService().removeAndSave(uuid, MuteKeys.MUTE_DATA);
            this.mutes.invalidate(uuid);
            Sponge.eventManager().post(new MuteEvent.Unmuted(
                    Sponge.server().causeStackManager().currentCause(),
                    uuid,
                    mute.get().getTimedEntry().map(TimedEntry::expired).orElse(false)));

            Sponge.server().player(uuid).ifPresent(x -> {
                this.mutes.put(uuid, MuteService.NOT_MUTED);
                this.serviceCollection.messageProvider().sendMessageTo(x, "mute.elapsed");
            });
            return true;
        }
        return false;
    }

    public void clearCacheFor(final UUID player) {
        final Mute muteData = this.mutes.get(player);
        if (muteData == MuteService.NOT_MUTED) {
            this.serviceCollection.storageManager().getUserService().removeAndSave(player, MuteKeys.MUTE_DATA);
        } else if (muteData instanceof NucleusMute) {
            this.serviceCollection.storageManager().getUserService().setAndSave(player, MuteKeys.MUTE_DATA, muteData);
        }
        this.mutes.invalidate(player);
    }

    public void onPlayerLogin(final ServerPlayer player) {
        this.mutes.refresh(player.uniqueId());
        final Mute mute = this.mutes.get(player.uniqueId());
        if (mute != MuteService.NOT_MUTED && mute instanceof NucleusMute) {
            this.onMute(mute, player);
        }
    }

    public boolean isGlobalMuteEnabled() {
        return this.globalMuteEnabled;
    }

    public void setGlobalMuteEnabled(final boolean globalMuteEnabled) {
        if (this.globalMuteEnabled != globalMuteEnabled) {
            this.voicedUsers.clear();
        }

        this.globalMuteEnabled = globalMuteEnabled;
    }

    public boolean isVoiced(final UUID uuid) {
        return this.voicedUsers.contains(uuid);
    }

    public void addVoice(final UUID uuid) {
        this.voicedUsers.add(uuid);
    }

    public void removeVoice(final UUID uuid) {
        this.voicedUsers.remove(uuid);
    }

    @Override
    public void onDataFileLocationChange(final INucleusServiceCollection serviceCollection) {
        this.mutes.invalidateAll();
    }

    public void onMute(final Mute md, final ServerPlayer user) {
        final IMessageProviderService messageProviderService = this.serviceCollection.messageProvider();
        if (md.getTimedEntry().isPresent()) {
            messageProviderService.sendMessageTo(user, "mute.playernotify.time",
                    messageProviderService.getTimeString(user.locale(), md.getTimedEntry().get().getRemainingTime().getSeconds()));
        } else {
            messageProviderService.sendMessageTo(user, "mute.playernotify.standard");
        }
    }

    public boolean isOnlineOnly() {
        return this.isOnlineOnly;
    }

}
