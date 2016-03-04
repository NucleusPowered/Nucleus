/*
 * This file is part of Essence, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.essencepowered.essence.listeners;

import com.google.inject.Inject;
import io.github.essencepowered.essence.api.PluginModule;
import io.github.essencepowered.essence.internal.ListenerBase;
import io.github.essencepowered.essence.internal.annotations.Modules;
import io.github.essencepowered.essence.internal.interfaces.InternalEssenceUser;
import io.github.essencepowered.essence.internal.services.datastore.UserConfigLoader;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.filter.cause.Root;
import org.spongepowered.api.event.network.ClientConnectionEvent;
import org.spongepowered.api.text.Text;

import java.io.IOException;
import java.util.Optional;

@Modules(PluginModule.NICKNAME)
public class NicknameListener extends ListenerBase {

    @Inject private UserConfigLoader ucl;

    @Listener
    public void onPlayerJoin(ClientConnectionEvent.Join event, @Root Player player) {
        InternalEssenceUser iqsu;
        try {
            iqsu = ucl.getUser(player);
        } catch (IOException | ObjectMappingException e) {
            e.printStackTrace();
            return;
        }

        Optional<Text> d = iqsu.getNicknameAsText();
        player.offer(Keys.SHOWS_DISPLAY_NAME, true);
        if (d.isPresent()) {
            player.offer(Keys.DISPLAY_NAME, d.get());
        } else {
            player.remove(Keys.DISPLAY_NAME);
        }
    }
}
