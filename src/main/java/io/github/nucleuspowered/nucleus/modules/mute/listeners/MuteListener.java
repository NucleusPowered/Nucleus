/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.mute.listeners;

import com.google.common.collect.Sets;
import com.google.inject.Inject;
import io.github.nucleuspowered.nucleus.Util;
import io.github.nucleuspowered.nucleus.api.data.MuteData;
import io.github.nucleuspowered.nucleus.api.events.NucleusMessageEvent;
import io.github.nucleuspowered.nucleus.internal.CommandPermissionHandler;
import io.github.nucleuspowered.nucleus.internal.PermissionRegistry;
import io.github.nucleuspowered.nucleus.internal.listeners.ListenerBase;
import io.github.nucleuspowered.nucleus.modules.message.events.InternalNucleusHelpOpEvent;
import io.github.nucleuspowered.nucleus.modules.mute.commands.MuteCommand;
import io.github.nucleuspowered.nucleus.modules.mute.commands.VoiceCommand;
import io.github.nucleuspowered.nucleus.modules.mute.config.MuteConfigAdapter;
import io.github.nucleuspowered.nucleus.modules.mute.handler.MuteHandler;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandMapping;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.command.SendCommandEvent;
import org.spongepowered.api.event.filter.cause.Root;
import org.spongepowered.api.event.message.MessageChannelEvent;
import org.spongepowered.api.event.network.ClientConnectionEvent;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.channel.MessageChannel;
import org.spongepowered.api.text.channel.type.PermissionMessageChannel;
import org.spongepowered.api.text.chat.ChatType;
import org.spongepowered.api.text.serializer.TextSerializers;

import javax.annotation.Nullable;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class MuteListener extends ListenerBase {

    @Inject private MuteHandler handler;
    @Inject private MuteConfigAdapter mca;
    @Inject private PermissionRegistry permissionRegistry;
    private CommandPermissionHandler cph;

    /**
     * At the time the player joins, check to see if the player is muted.
     *
     * @param event The event.
     */
    @Listener
    public void onPlayerLogin(final ClientConnectionEvent.Join event) {
        // Kick off a scheduled task.
        Sponge.getScheduler().createTaskBuilder().async().delay(500, TimeUnit.MILLISECONDS).execute(() -> {
            Player user = event.getTargetEntity();
            Optional<MuteData> omd = handler.getPlayerMuteData(user);
            if (omd.isPresent()) {
                MuteData md = omd.get();
                md.nextLoginToTimestamp();

                omd = Util.testForEndTimestamp(handler.getPlayerMuteData(user), () -> handler.unmutePlayer(user));
                if (omd.isPresent()) {
                    md = omd.get();
                    onMute(md, event.getTargetEntity());
                }
            }
        }).submit(plugin);
    }

    /**
     * Checks for blocked commands when muted.
     *
     * @param event The {@link SendCommandEvent} containing the command.
     * @param player The {@link Player} who executed the command.
     */
    @Listener(order = Order.FIRST)
    public void onPlayerSendCommand(SendCommandEvent event, @Root Player player) {
        List<String> commands = mca.getNodeOrDefault().getBlockedCommands();
        if (commands.isEmpty()) {
            return;
        }

        String command = event.getCommand().toLowerCase();
        Optional<? extends CommandMapping> oc = Sponge.getCommandManager().get(command, player);
        Set<String> cmd;

        // If the command exists, then get all aliases.
        if (oc.isPresent()) {
            cmd = oc.get().getAllAliases().stream().map(String::toLowerCase).collect(Collectors.toSet());
        } else {
            cmd = Sets.newHashSet(command);
        }

        // If the command is in the list, block it.
        if (commands.stream().map(String::toLowerCase).anyMatch(cmd::contains)) {
            Optional<MuteData> omd = Util.testForEndTimestamp(handler.getPlayerMuteData(player), () -> handler.unmutePlayer(player));
            if (omd.isPresent()) {
                onMute(omd.get(), player);
                MessageChannel.TO_CONSOLE.send(Text.builder().append(Text.of(player.getName() + " (")).append(plugin.getMessageProvider().getTextMessageWithFormat("standard.muted"))
                        .append(Text.of("): ")).append(Text.of("/" + event.getCommand() + " " + event.getArguments())).build());
                event.setCancelled(true);
            }
        }
    }

    @Listener(order = Order.FIRST)
    public void onPlayerChat(MessageChannelEvent.Chat event, @Root Player player) {
        boolean cancel = false;
        Optional<MuteData> omd = Util.testForEndTimestamp(handler.getPlayerMuteData(player), () -> handler.unmutePlayer(player));
        if (omd.isPresent()) {
            onMute(omd.get(), player);
            MessageChannel.TO_CONSOLE.send(Text.builder().append(Text.of(player.getName() + " (")).append(plugin.getMessageProvider().getTextMessageWithFormat("standard.muted"))
                    .append(Text.of("): ")).append(event.getRawMessage()).build());
            cancel = true;
        }

        if (cancelOnGlobalMute(player, event.isCancelled())) {
            cancel = true;
        }

        if (cancel) {
            if (mca.getNodeOrDefault().isShowMutedChat()) {
                // Send it to admins only.
                event.setChannel(new AdminChannel());
            } else {
                event.setCancelled(true);
            }
        }
    }

    @Listener
    public void onPlayerMessage(NucleusMessageEvent event) {
        if (!(event.getSender() instanceof Player)) {
            return;
        }

        Player user = (Player)event.getSender();
        Optional<MuteData> omd = Util.testForEndTimestamp(handler.getPlayerMuteData(user), () -> handler.unmutePlayer(user));
        if (omd.isPresent()) {
            if (user.isOnline()) {
                onMute(omd.get(), user.getPlayer().get());
            }

            event.setCancelled(true);
        }

        if (cancelOnGlobalMute(user, event.isCancelled())) {
            event.setCancelled(true);
        }
    }

    @Listener
    public void onPlayerHelpOp(InternalNucleusHelpOpEvent event, @Root Player user) {
        Optional<MuteData> omd = Util.testForEndTimestamp(handler.getPlayerMuteData(user), () -> handler.unmutePlayer(user));
        if (omd.isPresent()) {
            if (user.isOnline()) {
                onMute(omd.get(), user.getPlayer().get());
            }

            event.setCancelled(true);
        }

        if (cancelOnGlobalMute(user, event.isCancelled())) {
            event.setCancelled(true);
        }
    }

    private void onMute(MuteData md, Player user) {
        if (md.getEndTimestamp().isPresent()) {
            user.sendMessage(plugin.getMessageProvider().getTextMessageWithFormat("mute.playernotify.time",
                    Util.getTimeStringFromSeconds(Instant.now().until(md.getEndTimestamp().get(), ChronoUnit.SECONDS))));
        } else {
            user.sendMessage(plugin.getMessageProvider().getTextMessageWithFormat("mute.playernotify.standard"));
        }
    }

    private boolean cancelOnGlobalMute(Player player, boolean isCancelled) {
        if (isCancelled || !handler.isGlobalMuteEnabled() || getCommandPermission().testSuffix(player, "auto")) {
            return false;
        }

        if (handler.isVoiced(player.getUniqueId())) {
            return false;
        }

        player.sendMessage(plugin.getMessageProvider().getTextMessageWithFormat("globalmute.novoice"));
        return true;
    }

    private CommandPermissionHandler getCommandPermission() {
        if (cph == null) {
            cph = permissionRegistry.getService(VoiceCommand.class).orElse(new CommandPermissionHandler(new VoiceCommand(), plugin));
        }

        return cph;
    }

    private class AdminChannel extends PermissionMessageChannel {

        public AdminChannel() {
            super(MuteCommand.getMutedChatPermission());
        }

        @Override
        public void send(@Nullable Object sender, Text original, ChatType type) {
            Text tag = TextSerializers.FORMATTING_CODE.deserialize(MuteListener.this.mca.getNodeOrDefault().getCancelledTag());
            super.send(sender, Text.of(tag, original), type);
        }
    }
}
