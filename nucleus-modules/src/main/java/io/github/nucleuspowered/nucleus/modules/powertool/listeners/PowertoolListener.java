/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.powertool.listeners;

import io.github.nucleuspowered.nucleus.modules.powertool.PowertoolPermissions;
import io.github.nucleuspowered.nucleus.modules.powertool.services.PowertoolService;
import io.github.nucleuspowered.nucleus.scaffold.listener.ListenerBase;
import io.github.nucleuspowered.nucleus.services.INucleusServiceCollection;
import io.github.nucleuspowered.nucleus.services.impl.userprefs.NucleusKeysProvider;
import io.github.nucleuspowered.nucleus.services.interfaces.IMessageProviderService;
import io.github.nucleuspowered.nucleus.services.interfaces.IPermissionService;
import io.github.nucleuspowered.nucleus.services.interfaces.IUserPreferenceService;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.type.HandTypes;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.action.InteractEvent;
import org.spongepowered.api.event.block.InteractBlockEvent;
import org.spongepowered.api.event.entity.InteractEntityEvent;
import org.spongepowered.api.event.filter.cause.Root;
import org.spongepowered.api.event.filter.type.Exclude;
import org.spongepowered.api.event.network.ClientConnectionEvent;
import org.spongepowered.api.item.ItemType;

import javax.inject.Inject;

public class PowertoolListener implements ListenerBase {

    private final PowertoolService service;
    private final IUserPreferenceService userPreferenceService;
    private final IPermissionService permissionService;
    private final IMessageProviderService messageProviderService;

    @Inject
    public PowertoolListener(INucleusServiceCollection serviceCollection) {
        this.service = serviceCollection.getServiceUnchecked(PowertoolService.class);
        this.userPreferenceService = serviceCollection.userPreferenceService();
        this.permissionService = serviceCollection.permissionService();
        this.messageProviderService = serviceCollection.messageProvider();
    }

    @Listener
    public void onLogout(ClientConnectionEvent.Disconnect event) {
        this.service.reset(event.getTargetEntity().getUniqueId());
    }

    @Listener
    @Exclude(InteractBlockEvent.class)
    public void onUserInteract(final InteractEvent event, @Root Player player) {
        // No item in hand or no permission -> no powertool.
        if (!this.permissionService.hasPermission(player, PowertoolPermissions.BASE_POWERTOOL)
                || !player.getItemInHand(HandTypes.MAIN_HAND).isPresent()) {
            return;
        }

        // Get the item and the user.
        ItemType item = player.getItemInHand(HandTypes.MAIN_HAND).get().getType();

        // If the powertools are toggled on.
        if (this.userPreferenceService.get(player.getUniqueId(), NucleusKeysProvider.POWERTOOL_ENABLED).orElse(true)) {
            // Execute all powertools if they exist.
            this.service.getPowertoolForItem(player.getUniqueId(), item).ifPresent(x -> {
                // Cancel the interaction.
                event.setCancelled(true);

                final Player interacting;
                if (event instanceof InteractEntityEvent && ((InteractEntityEvent) event).getTargetEntity() instanceof Player) {
                    interacting = (Player)((InteractEntityEvent) event).getTargetEntity();
                } else {
                    interacting = null;
                }

                // Run each command.
                if (interacting == null && x.stream().allMatch(i -> i.contains("{{subject}}"))) {
                    this.messageProviderService.sendMessageTo(player, "powertool.playeronly");
                    return;
                }

                x.forEach(s -> {
                    if (s.contains("{{subject}}")) {
                        if (interacting != null) {
                            s = s.replace("{{subject}}", interacting.getName());
                        } else {
                            // Don't execute when no subject is in the way.
                            return;
                        }
                    }

                    Sponge.getCommandManager().process(player, s);
                });
            });
        }
    }
}
