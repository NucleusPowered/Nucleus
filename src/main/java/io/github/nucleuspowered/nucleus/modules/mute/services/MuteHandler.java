/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.mute.services;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import io.github.nucleuspowered.nucleus.NucleusPlugin;
import io.github.nucleuspowered.nucleus.Util;
import io.github.nucleuspowered.nucleus.api.nucleusdata.MuteInfo;
import io.github.nucleuspowered.nucleus.api.service.NucleusMuteService;
import io.github.nucleuspowered.nucleus.internal.annotations.APIService;
import io.github.nucleuspowered.nucleus.internal.interfaces.ServiceBase;
import io.github.nucleuspowered.nucleus.internal.traits.IDataManagerTrait;
import io.github.nucleuspowered.nucleus.internal.traits.MessageProviderTrait;
import io.github.nucleuspowered.nucleus.modules.mute.MuteKeys;
import io.github.nucleuspowered.nucleus.modules.mute.data.MuteData;
import io.github.nucleuspowered.nucleus.modules.mute.events.MuteEvent;
import io.github.nucleuspowered.nucleus.storage.dataobjects.modular.IUserDataObject;
import io.github.nucleuspowered.nucleus.util.CauseStackHelper;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.service.context.Context;
import org.spongepowered.api.service.context.ContextCalculator;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.util.Identifiable;

import javax.annotation.Nullable;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

@APIService(NucleusMuteService.class)
public class MuteHandler implements ContextCalculator<Subject>, NucleusMuteService, MessageProviderTrait, IDataManagerTrait, ServiceBase {

    private final Map<UUID, Boolean> muteContextCache = Maps.newHashMap();
    private final Context mutedContext = new Context(NucleusMuteService.MUTED_CONTEXT, "true");

    private boolean globalMuteEnabled = false;
    private final List<UUID> voicedUsers = Lists.newArrayList();

    public void onMute(Player user) {
        this.getPlayerMuteData(user).ifPresent(x -> onMute(x, user));
    }

    public void onMute(MuteData md, Player user) {
        if (md.getRemainingTime().isPresent()) {
            sendMessageTo(user, "mute.playernotify.time",
                    Util.getTimeStringFromSeconds(md.getRemainingTime().get().getSeconds()));
        } else {
            sendMessageTo(user, "mute.playernotify.standard");
        }
    }

    @Override public boolean isMuted(User user) {
        return getPlayerMuteData(user).isPresent();
    }

    @Override public Optional<MuteInfo> getPlayerMuteInfo(User user) {
        return getPlayerMuteData(user).map(x -> x);
    }

    // Internal
    public Optional<MuteData> getPlayerMuteData(User user) {
        Optional<MuteData> nu = getOrCreateUserOnThread(user.getUniqueId()).get(MuteKeys.MUTE_DATA);
        this.muteContextCache.put(user.getUniqueId(), nu.isPresent());
        return nu;
    }

    @Override public boolean mutePlayer(User user, String reason, @Nullable Duration duration, Cause cause) {
        UUID first = cause.first(User.class).map(Identifiable::getUniqueId).orElse(Util.consoleFakeUUID);
        return mutePlayer(user, new MuteData(first, reason, duration), cause);
    }

    public boolean mutePlayer(User user, MuteData data) {
        return mutePlayer(user, data, CauseStackHelper.createCause((Util.getObjectFromUUID(data.getMuterInternal()))));
    }

    public boolean mutePlayer(User user, MuteData data, Cause cause) {
        Preconditions.checkNotNull(user);
        Preconditions.checkNotNull(data);

        Optional<IUserDataObject> nu = getUserOnThread(user.getUniqueId());
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
        saveUser(user.getUniqueId(), u);
        this.muteContextCache.put(user.getUniqueId(), true);
        Sponge.getEventManager().post(new MuteEvent.Muted(
                cause,
                user,
                d,
                Text.of(data.getReason())));
        return true;
    }

    public boolean unmutePlayer(User user) {
        return unmutePlayer(user, CauseStackHelper.createCause(), true);
    }

    @Override public boolean unmutePlayer(User user, Cause cause) {
        return unmutePlayer(user, cause, false);
    }

    public boolean unmutePlayer(User user, Cause cause, boolean expired) {
        if (isMuted(user)) {
            Optional<IUserDataObject> o = getUserOnThread(user.getUniqueId());
            if (o.isPresent()) {
                IUserDataObject udo = o.get();
                udo.remove(MuteKeys.MUTE_DATA);
                saveUser(user.getUniqueId(), udo);
                this.muteContextCache.put(user.getUniqueId(), false);
                Sponge.getEventManager().post(new MuteEvent.Unmuted(
                        cause,
                        user,
                        expired));

                user.getPlayer().ifPresent(x ->
                    x.sendMessage(NucleusPlugin.getNucleus().getMessageProvider().getTextMessageWithFormat("mute.elapsed")));
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
