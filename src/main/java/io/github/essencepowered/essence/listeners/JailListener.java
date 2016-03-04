/*
 * This file is part of Essence, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.essencepowered.essence.listeners;

import com.google.inject.Inject;
import io.github.essencepowered.essence.NameUtil;
import io.github.essencepowered.essence.Util;
import io.github.essencepowered.essence.api.PluginModule;
import io.github.essencepowered.essence.api.data.JailData;
import io.github.essencepowered.essence.api.data.WarpLocation;
import io.github.essencepowered.essence.commands.jail.JailCommand;
import io.github.essencepowered.essence.config.MainConfig;
import io.github.essencepowered.essence.internal.ListenerBase;
import io.github.essencepowered.essence.internal.annotations.Modules;
import io.github.essencepowered.essence.internal.interfaces.InternalEssenceUser;
import io.github.essencepowered.essence.internal.services.JailHandler;
import io.github.essencepowered.essence.internal.services.datastore.UserConfigLoader;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.action.InteractEvent;
import org.spongepowered.api.event.block.ChangeBlockEvent;
import org.spongepowered.api.event.command.SendCommandEvent;
import org.spongepowered.api.event.entity.living.humanoid.player.RespawnPlayerEvent;
import org.spongepowered.api.event.filter.cause.Root;
import org.spongepowered.api.event.network.ClientConnectionEvent;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.channel.MessageChannel;
import org.spongepowered.api.text.format.TextColors;

import java.io.IOException;
import java.text.MessageFormat;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Modules(PluginModule.JAILS)
public class JailListener extends ListenerBase {

    @Inject
    private UserConfigLoader loader;

    @Inject
    private JailHandler handler;

    @Inject
    private MainConfig config;

    /**
     * At the time the player joins, check to see if the player is muted.
     *
     * @param event The event.
     */
    @Listener(order = Order.LATE)
    public void onPlayerLogin(final ClientConnectionEvent.Join event) {
        final Player user = event.getTargetEntity();
        InternalEssenceUser qs;
        try {
            qs = loader.getUser(user);
        } catch (IOException | ObjectMappingException e) {
            e.printStackTrace();
            return;
        }

        // Jailing the player if we need to.
        if (qs.jailOnNextLogin() && qs.getJailData().isPresent()) {
            Optional<WarpLocation> owl = handler.getWarpLocation(user);
            if (!owl.isPresent()) {
                MessageChannel.permission(JailCommand.notifyPermission).send(Text.of(TextColors.RED, "WARNING: No jail is defined. Jailed players are going free!"));
                handler.unjailPlayer(user);
                return;
            }

            JailData jd = qs.getJailData().get();
            jd.setPreviousLocation(user.getLocation());
            qs.setJailData(jd);
            user.setLocationAndRotation(owl.get().getLocation(), owl.get().getRotation());

            Optional<Duration> timeLeft = jd.getTimeLeft();
            Text message;
            if (timeLeft.isPresent()) {
                message = Text.of(TextColors.RED,
                        Util.getMessageWithFormat("command.jail.jailed",
                                owl.get().getName(), NameUtil.getNameFromUUID(jd.getJailer()), Util.getMessageWithFormat("standard.for"), Util.getTimeStringFromSeconds(timeLeft.get().getSeconds())));
            } else {
                message = Text.of(TextColors.RED,
                        Util.getMessageWithFormat("command.jail.jailed",
                                owl.get().getName(), NameUtil.getNameFromUUID(jd.getJailer()), "", ""));
            }

            qs.setFlying(false);
            user.sendMessage(message);
            user.sendMessage(Text.of(TextColors.RED, Util.getMessageWithFormat("standard.reason", jd.getReason())));
        }

        qs.setJailOnNextLogin(false);

        // Kick off a scheduled task.
        Sponge.getScheduler().createTaskBuilder().async().delay(500, TimeUnit.MILLISECONDS)
                .execute(() -> {
                    Optional<JailData> omd = qs.getJailData();
                    if (omd.isPresent()) {
                        JailData md = omd.get();
                        md.nextLoginToTimestamp();

                        omd = Util.testForEndTimestamp(qs.getJailData(), () -> handler.unjailPlayer(user));
                        if (omd.isPresent()) {
                            md = omd.get();
                            onJail(md, event.getTargetEntity());
                        }
                    }
                }).submit(plugin);
    }

    @Listener
    public void onCommand(SendCommandEvent event, @Root Player player) {
        // Only if the command is not in the control list.
        if (checkJail(player, false) && !config.getAllowedCommandsInJail().stream().anyMatch(x -> event.getCommand().equalsIgnoreCase(x))) {
            event.setCancelled(true);

            // This is the easiest way to send the messages.
            checkJail(player, true);
        }
    }

    @Listener
    public void onBlockChange(ChangeBlockEvent event, @Root Player player) {
        event.setCancelled(checkJail(player, true));
    }

    @Listener
    public void onInteract(InteractEvent event, @Root Player player) {
        event.setCancelled(checkJail(player, true));
    }

    @Listener
    public void onSpawn(RespawnPlayerEvent event) {
        if (checkJail(event.getTargetEntity(), false)) {
            event.setToTransform(event.getToTransform().setLocation(handler.getWarpLocation(event.getTargetEntity()).get().getLocation()));
        }
    }

    private boolean checkJail(final Player player, boolean sendMessage) {
        InternalEssenceUser qs;
        try {
            qs = loader.getUser(player);
        } catch (IOException | ObjectMappingException e) {
            e.printStackTrace();
            return false;
        }

        Optional<JailData> omd = Util.testForEndTimestamp(qs.getJailData(), () -> handler.unjailPlayer(player));
        if (omd.isPresent()) {
            if (sendMessage) {
                qs.setFlying(false);
                onJail(omd.get(), player);
            }

            return true;
        }

        return false;
    }

    private void onJail(JailData md, Player user) {
        if (md.getEndTimestamp().isPresent()) {
            user.sendMessage(Text.of(TextColors.RED, MessageFormat.format(
                    Util.getMessageWithFormat("jail.playernotify.time"),
                    Util.getTimeStringFromSeconds(Instant.now().until(md.getEndTimestamp().get(), ChronoUnit.SECONDS)))));
        } else {
            user.sendMessage(Text.of(TextColors.RED, Util.getMessageWithFormat("jail.playernotify")));
        }

        user.sendMessage(Text.of(TextColors.RED, Util.getMessageWithFormat("standard.reason", md.getReason())));
    }

}
