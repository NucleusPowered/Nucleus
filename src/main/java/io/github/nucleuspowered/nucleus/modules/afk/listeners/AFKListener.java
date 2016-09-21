/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.afk.listeners;

import com.google.inject.Inject;
import io.github.nucleuspowered.nucleus.internal.annotations.RegisterCommand;
import io.github.nucleuspowered.nucleus.internal.listeners.ListenerBase;
import io.github.nucleuspowered.nucleus.modules.afk.commands.AFKCommand;
import io.github.nucleuspowered.nucleus.modules.afk.handlers.AFKHandler;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.action.InteractEvent;
import org.spongepowered.api.event.command.SendCommandEvent;
import org.spongepowered.api.event.entity.DisplaceEntityEvent;
import org.spongepowered.api.event.filter.Getter;
import org.spongepowered.api.event.filter.cause.Root;
import org.spongepowered.api.event.message.MessageChannelEvent;
import org.spongepowered.api.event.network.ClientConnectionEvent;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class AFKListener extends ListenerBase {

    @Inject private AFKHandler handler;
    private final List<String> commands;

    @Inject
    private AFKListener() {
        commands = Arrays.stream(AFKCommand.class.getAnnotation(RegisterCommand.class).value()).map(String::toLowerCase).collect(Collectors.toList());
    }

    @Listener(order = Order.FIRST)
    public void onPlayerJoin(final ClientConnectionEvent.Join event, @Getter("getTargetEntity") Player player) {
        handler.stageUserActivityUpdate(player);
    }

    @Listener(order = Order.LAST)
    public void onPlayerInteract(final InteractEvent event, @Root Player player) {
        handler.stageUserActivityUpdate(player);
    }

    @Listener(order = Order.LAST)
    public void onPlayerMove(final DisplaceEntityEvent event, @Root Player player) {
        handler.stageUserActivityUpdate(player);
    }

    @Listener
    public void onPlayerChat(final MessageChannelEvent.Chat event, @Root Player player) {
        handler.stageUserActivityUpdate(player);
    }

    @Listener
    public void onPlayerCommand(final SendCommandEvent event, @Root Player player) {
        // Did the player run /afk? Then don't do anything, we'll toggle it
        // anyway.
        if (!commands.contains(event.getCommand().toLowerCase())) {
            handler.stageUserActivityUpdate(player);
        }
    }
}
