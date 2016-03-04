/*
 * This file is part of Essence, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.essencepowered.essence.listeners;

import com.google.inject.Inject;
import io.github.essencepowered.essence.Util;
import io.github.essencepowered.essence.api.PluginModule;
import io.github.essencepowered.essence.api.data.EssenceUser;
import io.github.essencepowered.essence.api.data.MuteData;
import io.github.essencepowered.essence.internal.ListenerBase;
import io.github.essencepowered.essence.internal.annotations.Modules;
import io.github.essencepowered.essence.internal.services.datastore.UserConfigLoader;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.filter.cause.First;
import org.spongepowered.api.event.message.MessageChannelEvent;
import org.spongepowered.api.event.network.ClientConnectionEvent;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.channel.MessageChannel;
import org.spongepowered.api.text.format.TextColors;

import java.io.IOException;
import java.text.MessageFormat;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Modules(PluginModule.MUTES)
public class MuteListener extends ListenerBase {

    @Inject private UserConfigLoader loader;

    /**
     * At the time the player joins, check to see if the player is muted.
     *
     * @param event The event.
     */
    @Listener
    public void onPlayerLogin(final ClientConnectionEvent.Join event) {
        // Kick off a scheduled task.
        Sponge.getScheduler().createTaskBuilder().async().delay(500, TimeUnit.MILLISECONDS)
                .execute(() -> {
                    Player user = event.getTargetEntity();
                    EssenceUser qs;
                    try {
                        qs = loader.getUser(user);
                    } catch (IOException | ObjectMappingException e) {
                        e.printStackTrace();
                        return;
                    }

                    Optional<MuteData> omd = qs.getMuteData();
                    if (omd.isPresent()) {
                        MuteData md = omd.get();
                        md.nextLoginToTimestamp();

                        omd = Util.testForEndTimestamp(qs.getMuteData(), qs::removeMuteData);
                        if (omd.isPresent()) {
                            md = omd.get();
                            onMute(md, event.getTargetEntity());
                        }
                    }
                }).submit(plugin);
    }

    @Listener
    public void onPlayerChat(MessageChannelEvent.Chat event, @First Player player) {
        EssenceUser qs;
        try {
            qs = loader.getUser(player);
        } catch (IOException | ObjectMappingException e) {
            e.printStackTrace();
            return;
        }

        Optional<MuteData> omd = Util.testForEndTimestamp(qs.getMuteData(), qs::removeMuteData);
        if (omd.isPresent()) {
            onMute(omd.get(), player);
            MessageChannel.TO_CONSOLE.send(Text.of(player.getName() + " (" + Util.getMessageWithFormat("muted") + "): ").toBuilder().append(event.getRawMessage()).build());
            event.setCancelled(true);
        }
    }

    private void onMute(MuteData md, Player user) {
        if (md.getEndTimestamp().isPresent()) {
            user.sendMessage(Text.of(TextColors.RED, MessageFormat.format(
                    Util.getMessageWithFormat("mute.playernotify.time"),
                    Util.getTimeStringFromSeconds(Instant.now().until(md.getEndTimestamp().get(), ChronoUnit.SECONDS)))));
        } else {
            user.sendMessage(Text.of(TextColors.RED, Util.getMessageWithFormat("mute.playernotify")));
        }
    }
}
