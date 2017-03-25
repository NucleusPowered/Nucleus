/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.ticket.listeners;

import io.github.nucleuspowered.nucleus.internal.ListenerBase;
import io.github.nucleuspowered.nucleus.modules.ticket.handlers.TicketHandler;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.game.state.GameStoppingServerEvent;

import javax.inject.Inject;

public class TicketServerStopListener extends ListenerBase {
    @Inject private TicketHandler handler;

    @Listener
    public void onShutdown(GameStoppingServerEvent event) {
        handler.invalidateCache();
    }
}
