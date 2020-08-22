/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.world.listeners;

import com.google.common.collect.Sets;
import io.github.nucleuspowered.nucleus.modules.world.WorldPermissions;
import io.github.nucleuspowered.nucleus.modules.world.config.WorldConfig;
import io.github.nucleuspowered.nucleus.scaffold.listener.ListenerBase;
import io.github.nucleuspowered.nucleus.services.INucleusServiceCollection;
import io.github.nucleuspowered.nucleus.services.interfaces.IPermissionService;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.entity.MoveEntityEvent;
import org.spongepowered.api.event.filter.Getter;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;
import org.spongepowered.api.world.teleport.TeleportHelperFilters;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

import javax.inject.Inject;

public class WorldListener implements ListenerBase.Conditional {

    private final Set<UUID> messageSent = Sets.newHashSet();
    private final INucleusServiceCollection serviceCollection;

    @Inject
    public WorldListener(INucleusServiceCollection serviceCollection) {
        this.serviceCollection = serviceCollection;
    }

    @Listener
    public void onPlayerTeleport(MoveEntityEvent.Teleport event, @Getter("getTargetEntity") Player player) {
        World target = event.getToTransform().getExtent();
        if (player.getWorld().equals(target)) return;

        IPermissionService permissionService = this.serviceCollection.permissionService();
        if (!permissionService.isConsoleOverride(event.getCause().first(CommandSource.class).orElse(player)) &&
                !this.serviceCollection.permissionService().hasPermission(player, WorldPermissions.getWorldAccessPermission(target.getName()))) {
            event.setCancelled(true);
            if (!this.messageSent.contains(player.getUniqueId())) {
                this.serviceCollection.messageProvider().sendMessageTo(player, "world.access.denied", target.getName());
            }

            if (event instanceof MoveEntityEvent.Teleport.Portal) {
                this.messageSent.add(player.getUniqueId());
                Sponge.getScheduler().createTaskBuilder()
                    .delayTicks(1)
                    .execute(relocate(player))
                    .submit(this.serviceCollection.pluginContainer());
            }
        }
    }

    @Override
    public boolean shouldEnable(INucleusServiceCollection serviceCollection) {
        return this.serviceCollection.moduleDataProvider().getModuleConfig(WorldConfig.class).isSeparatePermissions();
    }

    private Consumer<Task> relocate(Player player) {
        return task -> {
            Optional<Location<World>> location = Sponge.getTeleportHelper().getSafeLocationWithBlacklist(player.getLocation(), 5, 5, 5, TeleportHelperFilters.NO_PORTAL);
            if (location.isPresent()) {
                player.setLocation(location.get());
            } else {
                player.setLocationSafely(player.getWorld().getSpawnLocation());
            }

            this.messageSent.remove(player.getUniqueId());
        };
    }

}
