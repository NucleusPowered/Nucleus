/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.world.listeners;

import io.github.nucleuspowered.nucleus.Nucleus;
import io.github.nucleuspowered.nucleus.internal.ListenerBase;
import io.github.nucleuspowered.nucleus.internal.PermissionRegistry;
import io.github.nucleuspowered.nucleus.internal.annotations.ConditionalListener;
import io.github.nucleuspowered.nucleus.modules.world.WorldModule;
import io.github.nucleuspowered.nucleus.modules.world.config.WorldConfig;
import io.github.nucleuspowered.nucleus.modules.world.config.WorldConfigAdapter;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.entity.MoveEntityEvent;
import org.spongepowered.api.event.filter.Getter;
import org.spongepowered.api.world.World;

import java.util.function.Predicate;

@ConditionalListener(WorldListener.Condition.class)
public class WorldListener extends ListenerBase {

    @Listener
    public void onPlayerTeleport(MoveEntityEvent.Teleport event, @Getter("getTargetEntity") Player player) {
        World target = event.getToTransform().getExtent();
        if (player.getWorld().equals(target)) return;

        if (!player.hasPermission(PermissionRegistry.PERMISSIONS_PREFIX + "worlds." + target.getName().toLowerCase())) {
            event.setCancelled(true);
            player.sendMessage(plugin.getMessageProvider().getTextMessageWithFormat("world.access.denied", target.getName()));
        }
    }

    public static class Condition implements Predicate<Nucleus> {

        @Override public boolean test(Nucleus nucleus) {
            return nucleus.getConfigValue(WorldModule.ID, WorldConfigAdapter.class, WorldConfig::isSeparatePermissions).orElse(false);
        }
    }
}
