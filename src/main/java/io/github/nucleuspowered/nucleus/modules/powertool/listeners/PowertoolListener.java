/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.powertool.listeners;

import com.google.inject.Inject;
import io.github.nucleuspowered.nucleus.dataservices.UserService;
import io.github.nucleuspowered.nucleus.dataservices.loaders.UserDataManager;
import io.github.nucleuspowered.nucleus.internal.CommandPermissionHandler;
import io.github.nucleuspowered.nucleus.internal.PermissionRegistry;
import io.github.nucleuspowered.nucleus.internal.listeners.ListenerBase;
import io.github.nucleuspowered.nucleus.modules.core.config.CoreConfigAdapter;
import io.github.nucleuspowered.nucleus.modules.powertool.commands.PowertoolCommand;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.action.InteractEvent;
import org.spongepowered.api.event.filter.cause.Root;
import org.spongepowered.api.item.ItemType;

public class PowertoolListener extends ListenerBase {

    @Inject private UserDataManager loader;
    @Inject private CoreConfigAdapter config;

    @Inject private PermissionRegistry permissionRegistry;

    private CommandPermissionHandler s = null;

    private CommandPermissionHandler getPermissionUtil() {
        if (s == null) {
            s = permissionRegistry.getService(PowertoolCommand.class).orElseGet(() -> new CommandPermissionHandler(new PowertoolCommand(), plugin));
        }

        return s;
    }

    @Listener
    public void onUserInteract(final InteractEvent event, @Root Player player) {
        // No item in hand or no permission -> no powertool.
        if (!getPermissionUtil().testBase(player) || !player.getItemInHand().isPresent()) {
            return;
        }

        // Get the item and the user.
        ItemType item = player.getItemInHand().get().getItem();
        UserService user;
        try {
            user = loader.get(player).get();
        } catch (Exception e) {
            if (config.getNodeOrDefault().isDebugmode()) {
                e.printStackTrace();
            }

            return;
        }

        // If the powertools are toggled on.
        if (user.isPowertoolToggled()) {
            // Execute all powertools if they exist.
            user.getPowertoolForItem(item).ifPresent(x -> {
                // Cancel the interaction.
                event.setCancelled(true);

                // Run each command.
                x.forEach(s -> Sponge.getCommandManager().process(player, s));
            });
        }
    }
}
