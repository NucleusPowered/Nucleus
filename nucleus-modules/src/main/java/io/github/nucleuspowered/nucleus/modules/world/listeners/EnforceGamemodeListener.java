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
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.entity.Transform;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.gamemode.GameModes;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.entity.MoveEntityEvent;
import org.spongepowered.api.event.filter.Getter;
import org.spongepowered.api.event.network.ClientConnectionEvent;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.service.context.Context;
import org.spongepowered.api.world.World;

import java.util.Set;

import javax.inject.Inject;

public class EnforceGamemodeListener implements ListenerBase.Conditional {

    private final PluginContainer pluginContainer;

    @Inject
    public EnforceGamemodeListener(INucleusServiceCollection serviceCollection) {
        this.pluginContainer = serviceCollection.pluginContainer();
    }

    @Listener(order = Order.POST)
    public void onPlayerLogin(ClientConnectionEvent.Join event, @Getter("getTargetEntity") Player player) {
        Task.builder().execute(() -> enforce(player, player.getWorld())).submit(this.pluginContainer);
    }

    @Listener(order = Order.POST)
    public void onPlayerTeleport(MoveEntityEvent.Teleport event,
            @Getter("getTargetEntity") Player player,
            @Getter("getFromTransform") Transform<World> from,
            @Getter("getToTransform") Transform<World> to) {
        if (!from.getExtent().getUniqueId().equals(to.getExtent().getUniqueId())) {
            enforce(player, to.getExtent());
        }
    }

    private void enforce(Player player, World world) {
        if (world.getProperties().getGameMode() == GameModes.NOT_SET) {
            return;
        }

        Set<Context> contextSet = Sets.newHashSet(player.getActiveContexts());
        contextSet.removeIf(x -> x.getKey().equals(Context.WORLD_KEY));
        contextSet.add(new Context(Context.WORLD_KEY, world.getName()));
        if (!player.hasPermission(contextSet, WorldPermissions.WORLD_FORCE_GAMEMODE_OVERRIDE)) {
            // set their gamemode accordingly.
            player.offer(Keys.GAME_MODE, world.getProperties().getGameMode());
        }
    }

    @Override
    public boolean shouldEnable(INucleusServiceCollection serviceCollection) {
        return serviceCollection.moduleDataProvider().getModuleConfig(WorldConfig.class).isEnforceGamemodeOnWorldChange();
    }

}
