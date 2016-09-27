/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.afk.handlers;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import io.github.nucleuspowered.nucleus.NucleusPlugin;
import io.github.nucleuspowered.nucleus.internal.CommandPermissionHandler;
import io.github.nucleuspowered.nucleus.internal.PermissionRegistry;
import io.github.nucleuspowered.nucleus.modules.afk.commands.AFKCommand;
import io.github.nucleuspowered.nucleus.modules.afk.config.AFKConfig;
import io.github.nucleuspowered.nucleus.modules.afk.config.AFKConfigAdapter;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.text.channel.MessageChannel;
import org.spongepowered.api.text.serializer.TextSerializers;
import org.spongepowered.api.util.Identifiable;
import org.spongepowered.api.util.Tuple;

import javax.annotation.concurrent.GuardedBy;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class AFKHandler {

    @Inject private NucleusPlugin plugin;
    @Inject private AFKConfigAdapter aca;
    @Inject private PermissionRegistry permissionRegistry;

    @GuardedBy("setLock")
    private final Set<Player> staged = Sets.newHashSet();
    private final Map<UUID, Data> afkData = Maps.newHashMap();
    private final String exempttoggle = "exempt.toggle";
    private final String exemptkick = "exempt.kick";

    private final Object setLock = new Object();

    private CommandPermissionHandler s = null;

    public void stageUserActivityUpdate(Player player) {
        synchronized (setLock) {
            staged.add(player);
        }
    }

    /**
     * Updates all staged users.
     */
    public void updateUserActivity() {
        Set<Player> thisRun;
        synchronized (setLock) {
            thisRun = Sets.newHashSet(staged);
            staged.clear();
        }

        Instant now = Instant.now();
        thisRun.stream().filter(User::isOnline).forEach(x -> updateUserActivity(x, now));
    }

    /**
     * Updates a specific user.
     *
     * @param player The {@link Player} of the user to update.
     */
    public void updateUserActivity(Player player) {
        synchronized (setLock) {
            staged.remove(player);
        }

        updateUserActivity(player, Instant.now());
    }

    /**
     * Returns whether a player is AFK.
     *
     * @param player The {@link Player} to check
     * @return If they are AFK.
     */
    public boolean isAfk(Player player) {
        return afkData.getOrDefault(player.getUniqueId(), new Data()).afk;
    }

    /**
     * Sets a player as AFK
     *
     * @param player The {@link Player} to set.
     * @return <code>true</code> if the player has not got the exemption permission.
     */
    public boolean setAsAfk(Player player) {
        if (!getPermissionUtil().testSuffix(player, exempttoggle)) {
            UUID uuid = player.getUniqueId();
            Data data = afkData.get(uuid);
            if (data == null) {
                data = new Data();
                afkData.put(uuid, data);
            }

            data.afk = true;
            sendAFKMessage(player, true);
            return true;
        }

        return false;
    }

    /**
     * Updates all players' AFK status if they are inactive.
     */
    public void updateAfkStatus() {
        purgeNotOnline();
        final AFKConfig config = aca.getNodeOrDefault();

        long afkTime = config.getAfkTime();
        long afkTimeKick = config.getAfkTimeToKick();
        Instant now = Instant.now();
        CommandPermissionHandler cph = getPermissionUtil();
        if (afkTime > 0) {
            workOnAfkPlayers(now.minus(afkTime, ChronoUnit.SECONDS), cph, exempttoggle, x -> !x.getValue().afk, x -> {
                x.getSecond().afk = true;
                sendAFKMessage(x.getFirst(), true);
            });
        }

        if (afkTimeKick > 0) {
            workOnAfkPlayers(now.minus(afkTimeKick, ChronoUnit.SECONDS), cph, exemptkick, x -> true, x -> {
                String message = config.getMessages().getKickMessage().trim();
                if (message.isEmpty()) {
                    message = NucleusPlugin.getNucleus().getMessageProvider().getMessageWithFormat("afk.kickreason");
                }

                final String messageToServer = config.getMessages().getOnKick().trim();
                final String messageToGetAroundJavaRestrictions = message;
                Sponge.getScheduler().createSyncExecutor(plugin).execute(() -> x.getFirst().kick(TextSerializers.FORMATTING_CODE.deserialize(messageToGetAroundJavaRestrictions)));
                if (!messageToServer.isEmpty()) {
                    MessageChannel mc;
                    if (config.isBroadcastOnKick()) {
                        mc = MessageChannel.TO_ALL;
                    } else {
                        mc = MessageChannel.permission(getPermissionUtil().getPermissionWithSuffix("notify"));
                    }

                    mc.send(plugin.getChatUtil().getPlayerMessageFromTemplate(messageToServer, x.getFirst(), true));
                }
            });
        }
    }

    /**
     * Remove all those who are not online from the data pool.
     */
    private void purgeNotOnline() {
        Set<UUID> uuids = Sponge.getServer().getOnlinePlayers().stream()
                .map(Identifiable::getUniqueId).collect(Collectors.toSet());
        afkData.entrySet().removeIf(x -> !uuids.contains(x.getKey()));
    }

    @SuppressWarnings("all")
    private void workOnAfkPlayers(Instant now, CommandPermissionHandler cph, String permissionSuffix, Predicate<Map.Entry<UUID, Data>> firstCheck, Consumer<Tuple<Player, Data>> forEach) {
        afkData.entrySet().stream()
                .filter(firstCheck)
                .filter(x -> x.getValue().lastActivity.isBefore(now))
                .map(x -> new Tuple<>(Sponge.getServer().getPlayer(x.getKey()).orElse(null), x.getValue()))
                .filter(x -> x.getFirst() != null && !cph.testSuffix(x.getFirst(), permissionSuffix))
                .forEach(forEach);
    }

    private void updateUserActivity(Player player, Instant now) {
        UUID uuid = player.getUniqueId();
        Data data = afkData.get(uuid);
        if (data == null) {
            afkData.put(uuid, new Data());
            return;
        }

        data.lastActivity = now;

        // Only tell players that this player is AFK if they are supposed to be able to go AFK in the first place.
        if (data.afk && player.isOnline() && !getPermissionUtil().testSuffix(player, exempttoggle)) {
            sendAFKMessage(player, false);
        }

        data.afk = false;
    }

    private void sendAFKMessage(Player player, boolean isAfk) {
        // If we have the config set to true, or the player is NOT invisible, send an AFK message
        if (aca.getNodeOrDefault().isAfkOnVanish() || !player.get(Keys.INVISIBLE).orElse(false)) {
            String template = isAfk ? aca.getNodeOrDefault().getMessages().getAfkMessage().trim() : aca.getNodeOrDefault().getMessages().getReturnAfkMessage().trim();
            if (!template.isEmpty()) {
                MessageChannel.TO_ALL.send(plugin.getChatUtil().getPlayerMessageFromTemplate(template, player, true));
            }
        } else {
            // Tell the user in question about them going AFK
            player.sendMessage(NucleusPlugin.getNucleus().getMessageProvider().getTextMessageWithFormat(isAfk ? "command.afk.from.vanish" : "command.afk.to.vanish"));
        }
    }

    /**
     * Gets the permission utility from the AFK command.
     *
     * @return The permission util.
     */
    private CommandPermissionHandler getPermissionUtil() {
        if (s == null) {
            s = permissionRegistry.getService(AFKCommand.class).orElseGet(() -> new CommandPermissionHandler(new AFKCommand(), plugin));
        }

        return s;
    }

    private static class Data {
        private Instant lastActivity = Instant.now();
        private boolean afk = false;
    }
}
