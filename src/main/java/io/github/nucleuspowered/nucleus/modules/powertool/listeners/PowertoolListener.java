/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.powertool.listeners;

import com.google.inject.Inject;
import io.github.nucleuspowered.nucleus.dataservices.UserService;
import io.github.nucleuspowered.nucleus.dataservices.loaders.UserDataManager;
import io.github.nucleuspowered.nucleus.internal.CommandPermissionHandler;
import io.github.nucleuspowered.nucleus.internal.ListenerBase;
import io.github.nucleuspowered.nucleus.internal.PermissionRegistry;
import io.github.nucleuspowered.nucleus.modules.core.config.CoreConfigAdapter;
import io.github.nucleuspowered.nucleus.modules.powertool.commands.PowertoolCommand;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.type.HandTypes;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.action.InteractEvent;
import org.spongepowered.api.event.entity.InteractEntityEvent;
import org.spongepowered.api.event.filter.cause.Root;
import org.spongepowered.api.item.ItemType;

public class PowertoolListener extends ListenerBase {

    @Inject private UserDataManager loader;
    @Inject private CoreConfigAdapter config;

    @Inject private PermissionRegistry permissionRegistry;

    private CommandPermissionHandler s = null;

    private CommandPermissionHandler getPermissionUtil() {
        if (s == null) {
            s = permissionRegistry.getService(PowertoolCommand.class);
        }

        return s;
    }

    @Listener
    public void onUserInteract(final InteractEvent event, @Root Player player) {
        // No item in hand or no permission -> no powertool.
        if (!getPermissionUtil().testBase(player) || !player.getItemInHand(HandTypes.MAIN_HAND).isPresent()) {
            return;
        }

        // Get the item and the user.
        ItemType item = player.getItemInHand(HandTypes.MAIN_HAND).get().getItem();
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

                final Player interacting;
                if (event instanceof InteractEntityEvent && ((InteractEntityEvent) event).getTargetEntity() instanceof Player) {
                    interacting = (Player)((InteractEntityEvent) event).getTargetEntity();
                } else {
                    interacting = null;
                }

                // Run each command.
                if (interacting == null && x.stream().allMatch(i -> i.contains("{{player}}"))) {
                    player.sendMessage(plugin.getMessageProvider().getTextMessageWithFormat("powertool.playeronly"));
                    return;
                }

                x.forEach(s -> {
                    if (s.contains("{{player}}")) {
                        if (interacting != null) {
                            s = s.replace("{{player}}", interacting.getName());
                        } else {
                            // Don't execute when no player is in the way.
                            return;
                        }
                    }

                    Sponge.getCommandManager().process(player, s);
                });
            });
        }
    }
}
