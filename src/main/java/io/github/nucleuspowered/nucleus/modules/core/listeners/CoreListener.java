/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.core.listeners;

import com.google.inject.Inject;
import io.github.nucleuspowered.nucleus.Util;
import io.github.nucleuspowered.nucleus.dataservices.UserService;
import io.github.nucleuspowered.nucleus.dataservices.loaders.UserDataManager;
import io.github.nucleuspowered.nucleus.internal.listeners.ListenerBase;
import io.github.nucleuspowered.nucleus.modules.core.config.CoreConfigAdapter;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.filter.Getter;
import org.spongepowered.api.event.game.GameReloadEvent;
import org.spongepowered.api.event.game.state.GameStoppingServerEvent;
import org.spongepowered.api.event.network.ClientConnectionEvent;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.text.serializer.TextSerializers;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.time.Instant;
import java.util.Iterator;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

public class CoreListener extends ListenerBase {

    @Inject private UserDataManager loader;
    @Inject private CoreConfigAdapter cca;
    private boolean runSync = false;

    /* (non-Javadoc)
     * We do this first to try to get the first play status as quick as possible.
     */
    @Listener(order = Order.FIRST)
    public void onPlayerJoinFirst(final ClientConnectionEvent.Join event) {
        try {
            Player player = event.getTargetEntity();
            UserService qsu = loader.get(player).get();
            qsu.setLastLogin(Instant.now());
            qsu.setFirstPlay(Util.isFirstPlay(player));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Listener(order = Order.LAST)
    public void onPlayerJoinLast(final ClientConnectionEvent.Join event) {
        Player player = event.getTargetEntity();
        UserService qsu = loader.get(player).get();

        // If we have a location to send them to in the config, send them there now!
        Optional<Location<World>> olw = qsu.getLocationOnLogin();
        if (olw.isPresent()) {
            event.getTargetEntity().setLocation(olw.get());
            qsu.removeLocationOnLogin();
        }
    }

    @Listener
    public void onPlayerQuit(final ClientConnectionEvent.Disconnect event, @Getter("getTargetEntity") final Player player) {
        final Location<World> location = player.getLocation();
        if (runSync) {
            // Work around things not existing, run quit events sync just as the server draws to a close.
            onPlayerQuitInner(player, location);
        } else {
            Sponge.getScheduler().createAsyncExecutor(plugin).schedule(() -> onPlayerQuitInner(player, location), 200, TimeUnit.MILLISECONDS);
        }
    }

    private void onPlayerQuitInner(final Player player, final Location<World> location) {
        try {
            this.plugin.getUserDataManager().get(player).ifPresent(x -> x.setOnLogout(location));
        } catch (Exception e) {
            if (cca.getNodeOrDefault().isDebugmode()) {
                e.printStackTrace();
            }
        }
    }

    @Listener
    public void onServerAboutToStop(final GameStoppingServerEvent event) {
        runSync = true;
        if (cca.getNodeOrDefault().isKickOnStop()) {
            Iterator<Player> players = Sponge.getServer().getOnlinePlayers().iterator();
            while (players.hasNext()) {
                Text msg = TextSerializers.FORMATTING_CODE.deserialize(cca.getNodeOrDefault().getKickOnStopMessage());
                if (msg.isEmpty()) {
                    players.next().kick();
                } else {
                    players.next().kick(msg);
                }
            }
        }
    }

    @Listener
    public void onGameReload(final GameReloadEvent event) {
        plugin.reload();
        CommandSource requester = event.getCause().first(CommandSource.class).orElse(Sponge.getServer().getConsole());
        requester.sendMessage(Text.of(TextColors.YELLOW, "[NucleusPlugin] ", plugin.getMessageProvider().getTextMessageWithFormat("command.reload.one")));
        requester.sendMessage(Text.of(TextColors.YELLOW, "[NucleusPlugin] ", plugin.getMessageProvider().getTextMessageWithFormat("command.reload.two")));
    }
}
