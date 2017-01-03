/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.afk.handlers;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import io.github.nucleuspowered.nucleus.NucleusPlugin;
import io.github.nucleuspowered.nucleus.Util;
import io.github.nucleuspowered.nucleus.internal.CommandPermissionHandler;
import io.github.nucleuspowered.nucleus.modules.afk.commands.AFKCommand;
import io.github.nucleuspowered.nucleus.modules.afk.config.AFKConfig;
import io.github.nucleuspowered.nucleus.modules.afk.config.AFKConfigAdapter;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.channel.MessageChannel;
import org.spongepowered.api.text.serializer.TextSerializers;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.annotation.concurrent.GuardedBy;

public class AFKHandler {

    private final Map<UUID, AFKData> data = Maps.newHashMap();
    private final AFKConfigAdapter afkConfigAdapter;
    private final NucleusPlugin plugin;
    private CommandPermissionHandler afkPermissionHandler;
    private AFKConfig config;

    @GuardedBy("lock")
    private final Set<UUID> activity = Sets.newHashSet();
    private final Object lock = new Object();

    private final String exempttoggle = "exempt.toggle";
    private final String exemptkick = "exempt.kick";

    private final String afkOption = "nucleus.afk.toggletime";
    private final String afkKickOption = "nucleus.afk.kicktime";

    public AFKHandler(NucleusPlugin plugin, AFKConfigAdapter afkConfigAdapter) {
        this.plugin = plugin;
        this.afkConfigAdapter = afkConfigAdapter;
        plugin.registerReloadable(this::onReload);
        onReload();
    }

    public void stageUserActivityUpdate(Player player) {
        if (player.isOnline()) {
            stageUserActivityUpdate(player.getUniqueId());
        }
    }

    public void stageUserActivityUpdate(UUID uuid) {
        synchronized (lock) {
            activity.add(uuid);
        }
    }

    public void onTick() {
        synchronized (lock) {
            activity.forEach(u -> data.compute(u, ((uuid, afkData) -> afkData == null ? new AFKData(uuid) : updateActivity(uuid, afkData))));
            activity.clear();
        }

        List<UUID> uuidList = Sponge.getServer().getOnlinePlayers().stream().map(Player::getUniqueId).collect(Collectors.toList());

        // Remove all offline players.
        Set<Map.Entry<UUID, AFKData>> entries = data.entrySet();
        entries.removeIf(refactor -> !uuidList.contains(refactor.getKey()));
        entries.stream().filter(x -> !x.getValue().cacheValid).forEach(x -> x.getValue().updateFromPermissions());

        long now = System.currentTimeMillis();

        // Check AFK status.
        entries.stream().filter(x -> x.getValue().isKnownAfk && !x.getValue().willKick && x.getValue().timeToKick > 0).forEach(e -> {
            if (now - e.getValue().lastActivityTime > e.getValue().timeToKick) {
                // Kick them
                e.getValue().willKick = true;
                String message = config.getMessages().getKickMessage().trim();
                if (message.isEmpty()) {
                    message = plugin.getMessageProvider().getMessageWithFormat("afk.kickreason");
                }

                final String messageToServer = config.getMessages().getOnKick().trim();
                final String messageToGetAroundJavaRestrictions = message;

                Sponge.getServer().getPlayer(e.getKey()).ifPresent(player -> {
                    Sponge.getScheduler().createSyncExecutor(plugin)
                        .execute(() -> player.kick(TextSerializers.FORMATTING_CODE.deserialize(messageToGetAroundJavaRestrictions)));
                    if (!messageToServer.isEmpty()) {
                        MessageChannel mc;
                        if (config.isBroadcastOnKick()) {
                            mc = MessageChannel.TO_ALL;
                        } else {
                            mc = MessageChannel.permission(getPermissionUtil().getPermissionWithSuffix("notify"));
                        }

                        mc.send(plugin.getChatUtil().getMessageFromTemplate(messageToServer, player, true));
                    }
                });
            }
        });

        // Check AFK status.
        entries.stream().filter(x -> !x.getValue().isKnownAfk && x.getValue().timeToAfk > 0).forEach(e -> {
            if (now - e.getValue().lastActivityTime  > e.getValue().timeToAfk) {
                // Set AFK, message
                e.getValue().isKnownAfk = true;
                sendAFKMessage(e.getKey(), true);
            }
        });
    }

    public void invalidateAfkCache() {
        data.forEach((k, v) -> v.cacheValid = false);
    }

    public boolean isAfk(Player player) {
        return isAfk(player.getUniqueId());
    }

    public boolean isAfk(UUID uuid) {
        return data.containsKey(uuid) && data.get(uuid).isKnownAfk;
    }

    public boolean setAfk(Player player) {
        if (!player.isOnline()) {
            return false;
        }

        UUID uuid = player.getUniqueId();
        AFKData a = data.compute(uuid, ((u, afkData) -> afkData == null ? new AFKData(u) : afkData));
        if (a.isKnownAfk) {
            return false;
        }

        if (a.canGoAfk()) {
            // Don't accident undo setting AFK, remove any activity from the list.
            synchronized (lock) {
                activity.remove(uuid);
            }

            a.isKnownAfk = true;
            sendAFKMessage(uuid, true);
            return true;
        }

        return false;
    }

    private void onReload() {
        config = afkConfigAdapter.getNodeOrDefault();
    }

    /**
     * Gets the permission utility from the AFK command.
     *
     * @return The permission util.
     */
    private CommandPermissionHandler getPermissionUtil() {
        if (afkPermissionHandler == null) {
            afkPermissionHandler = plugin.getPermissionRegistry().getService(AFKCommand.class);
        }

        return afkPermissionHandler;
    }

    private AFKData updateActivity(UUID uuid, AFKData data) {
        data.lastActivityTime = System.currentTimeMillis();
        if (data.isKnownAfk) {
            data.isKnownAfk = false;

            // Send message.
            sendAFKMessage(uuid, false);
        }

        return data;
    }

    private void sendAFKMessage(UUID uuid, boolean isAfk) {
        Sponge.getServer().getPlayer(uuid).ifPresent(player -> {
            // If we have the config set to true, or the player is NOT invisible, send an AFK message
            if (config.isAfkOnVanish() || !player.get(Keys.INVISIBLE).orElse(false)) {
                String template = isAfk ? config.getMessages().getAfkMessage().trim() : config.getMessages().getReturnAfkMessage().trim();
                if (!template.isEmpty()) {
                    MessageChannel.TO_ALL.send(plugin.getChatUtil().getMessageFromTemplate(template, player, true));
                }
            } else {
                // Tell the user in question about them going AFK
                player.sendMessage(
                    NucleusPlugin.getNucleus().getMessageProvider().getTextMessageWithFormat(isAfk ? "command.afk.from.vanish" : "command.afk.to.vanish"));
            }
        });
    }

    private class AFKData {

        private final UUID uuid;

        private long lastActivityTime = System.currentTimeMillis();
        private boolean isKnownAfk = false;
        private boolean willKick = false;

        private boolean cacheValid = false;
        private long timeToAfk = -1;
        private long timeToKick = -1;

        private AFKData(UUID uuid) {
            this.uuid = uuid;
            updateFromPermissions();
        }

        private boolean canGoAfk() {
            return timeToAfk > 0;
        }

        private void updateFromPermissions() {
            // Get the subject.
            Sponge.getServer().getPlayer(uuid).ifPresent(x -> {
                if (getPermissionUtil().testSuffix(x, exempttoggle)) {
                    timeToAfk = -1;
                } else {
                    timeToAfk = Util.getPositiveLongOptionFromSubject(x, afkOption).orElseGet(() -> config.getAfkTime()) * 1000;
                }

                if (getPermissionUtil().testSuffix(x, exemptkick)) {
                    timeToKick = -1;
                } else {
                    timeToKick = Util.getPositiveLongOptionFromSubject(x, afkKickOption).orElseGet(() -> config.getAfkTimeToKick()) * 1000;
                }

                cacheValid = true;
            });
        }
    }
}
