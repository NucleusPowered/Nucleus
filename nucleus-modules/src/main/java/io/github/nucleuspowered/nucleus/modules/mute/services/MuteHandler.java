/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.mute.services;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import io.github.nucleuspowered.nucleus.Util;
import io.github.nucleuspowered.nucleus.api.module.mute.NucleusMuteService;
import io.github.nucleuspowered.nucleus.api.module.mute.data.Mute;
import io.github.nucleuspowered.nucleus.modules.mute.MuteKeys;
import io.github.nucleuspowered.nucleus.modules.mute.data.MuteData;
import io.github.nucleuspowered.nucleus.modules.mute.events.MuteEvent;
import io.github.nucleuspowered.nucleus.scaffold.service.ServiceBase;
import io.github.nucleuspowered.nucleus.scaffold.service.annotations.APIService;
import io.github.nucleuspowered.nucleus.services.INucleusServiceCollection;
import io.github.nucleuspowered.nucleus.services.impl.storage.dataobjects.modular.IUserDataObject;
import io.github.nucleuspowered.nucleus.services.interfaces.IMessageProviderService;
import io.github.nucleuspowered.nucleus.services.interfaces.IStorageManager;
import io.github.nucleuspowered.nucleus.util.CauseStackHelper;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.service.context.Context;
import org.spongepowered.api.service.context.ContextCalculator;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.util.Identifiable;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Nullable;
import javax.inject.Inject;

@APIService(NucleusMuteService.class)
public class MuteHandler implements ContextCalculator<Subject>, NucleusMuteService, ServiceBase {

    private final IMessageProviderService messageProviderService;
    private final IStorageManager storageManager;
    private final PluginContainer pluginContainer;
    private final Map<UUID, Boolean> muteContextCache = Maps.newHashMap();
    private final Context mutedContext = new Context(NucleusMuteService.MUTED_CONTEXT, "true");

    private boolean globalMuteEnabled = false;
    private final List<UUID> voicedUsers = Lists.newArrayList();

    @Inject
    public MuteHandler(INucleusServiceCollection serviceCollection) {
        this.messageProviderService = serviceCollection.messageProvider();
        this.storageManager = serviceCollection.storageManager();
        this.pluginContainer = serviceCollection.pluginContainer();
    }

    public void onMute(Player user) {
        this.getPlayerMuteData(user).ifPresent(x -> onMute(x, user));
    }

    public void onMute(MuteData md, Player user) {
        if (md.getRemainingTime().isPresent()) {
            this.messageProviderService.sendMessageTo(user, "mute.playernotify.time",
                    this.messageProviderService.getTimeString(user.getLocale(), md.getRemainingTime().get().getSeconds()));
        } else {
            this.messageProviderService.sendMessageTo(user, "mute.playernotify.standard");
        }
    }

    @Override public boolean isMuted(User user) {
        return getPlayerMuteData(user).isPresent();
    }

    @Override public Optional<Mute> getPlayerMuteInfo(User user) {
        return getPlayerMuteData(user).map(x -> x);
    }

    // Internal
    public Optional<MuteData> getPlayerMuteData(User user) {
        Optional<MuteData> nu = this.storageManager.getOrCreateUserOnThread(user.getUniqueId()).get(MuteKeys.MUTE_DATA);
        this.muteContextCache.put(user.getUniqueId(), nu.isPresent());
        return nu;
    }

    @Override public boolean mutePlayer(User user, String reason, @Nullable Duration duration, Cause cause) {
        UUID first = cause.first(User.class).map(Identifiable::getUniqueId).orElse(Util.CONSOLE_FAKE_UUID);
        return mutePlayer(user, new MuteData(first, reason, duration), cause);
    }

    public boolean mutePlayer(User user, MuteData data) {
        return mutePlayer(user, data, CauseStackHelper.createCause((Util.getObjectFromUUID(data.getMuterInternal()))));
    }

    public boolean mutePlayer(User user, MuteData data, Cause cause) {
        Preconditions.checkNotNull(user);
        Preconditions.checkNotNull(data);

        Optional<IUserDataObject> nu = this.storageManager.getUserOnThread(user.getUniqueId());
        if (!nu.isPresent()) {
            return false;
        }

        Instant time = Instant.now();
        IUserDataObject u = nu.get();
        final Duration d = data.getRemainingTime().orElse(null);
        if (user.isOnline() && data.getTimeFromNextLogin().isPresent() && !data.getEndTimestamp().isPresent()) {
            data.setEndtimestamp(time.plus(data.getTimeFromNextLogin().get()));
        }

        u.set(MuteKeys.MUTE_DATA, data);
        this.storageManager.saveUser(user.getUniqueId(), u);
        this.muteContextCache.put(user.getUniqueId(), true);
        Sponge.getEventManager().post(new MuteEvent.Muted(
                cause,
                user,
                d,
                Text.of(data.getReason())));
        return true;
    }

    public boolean unmutePlayer(User user) {
        return unmutePlayer(user, CauseStackHelper.createCause(this.pluginContainer), true);
    }

    @Override public boolean unmutePlayer(User user, Cause cause) {
        return unmutePlayer(user, cause, false);
    }

    public boolean unmutePlayer(User user, Cause cause, boolean expired) {
        if (isMuted(user)) {
            Optional<IUserDataObject> o = this.storageManager.getUserOnThread(user.getUniqueId());
            if (o.isPresent()) {
                IUserDataObject udo = o.get();
                udo.remove(MuteKeys.MUTE_DATA);
                this.storageManager.saveUser(user.getUniqueId(), udo);
                this.muteContextCache.put(user.getUniqueId(), false);
                Sponge.getEventManager().post(new MuteEvent.Unmuted(
                        cause,
                        user,
                        expired));

                user.getPlayer().ifPresent(x ->
                        this.messageProviderService.sendMessageTo(x, "mute.elapsed"));
                return true;
            }
        }

        return false;
    }

    public boolean isGlobalMuteEnabled() {
        return this.globalMuteEnabled;
    }

    public void setGlobalMuteEnabled(boolean globalMuteEnabled) {
        if (this.globalMuteEnabled != globalMuteEnabled) {
            this.voicedUsers.clear();
        }

        this.globalMuteEnabled = globalMuteEnabled;
    }

    public boolean isVoiced(UUID uuid) {
        return this.voicedUsers.contains(uuid);
    }

    public void addVoice(UUID uuid) {
        this.voicedUsers.add(uuid);
    }

    public void removeVoice(UUID uuid) {
        this.voicedUsers.remove(uuid);
    }

    @Override public void accumulateContexts(Subject calculable, Set<Context> accumulator) {
        if (calculable instanceof User) {
            UUID u = ((User) calculable).getUniqueId();
            if (this.muteContextCache.computeIfAbsent(u, k -> isMuted((User) calculable))) {
                accumulator.add(this.mutedContext);
            }
        }
    }

    @Override public boolean matches(Context context, Subject subject) {
        return context.getKey().equals(NucleusMuteService.MUTED_CONTEXT) && subject instanceof User &&
                this.muteContextCache.computeIfAbsent(((User) subject).getUniqueId(), k -> isMuted((User) subject));
    }

    public boolean isMutedCached(User x) {
        return this.muteContextCache.containsKey(x.getUniqueId());
    }
}
