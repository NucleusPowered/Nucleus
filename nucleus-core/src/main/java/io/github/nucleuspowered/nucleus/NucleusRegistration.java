/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus;

import io.github.nucleuspowered.nucleus.modules.core.teleport.filters.NoCheckFilter;
import io.github.nucleuspowered.nucleus.modules.core.teleport.filters.WallCheckFilter;
import io.github.nucleuspowered.nucleus.services.INucleusServiceCollection;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.game.GameRegistryEvent;
import org.spongepowered.api.text.placeholder.PlaceholderParser;
import org.spongepowered.api.world.teleport.TeleportHelperFilter;

public class NucleusRegistration {

    private final INucleusServiceCollection serviceCollection;

    NucleusRegistration(INucleusServiceCollection serviceCollection) {
        this.serviceCollection = serviceCollection;
    }

    @Listener
    public void onRegisterTeleportHelperFilters(GameRegistryEvent.Register<TeleportHelperFilter> event) {
        event.register(new NoCheckFilter());
        event.register(new WallCheckFilter());
    }

    @Listener
    public void onRegisterPlaceholders(GameRegistryEvent.Register<PlaceholderParser> event) {
        this.serviceCollection.placeholderService().getParsers().forEach(event::register);
        this.serviceCollection.logger().info("Registered placeholder parsers.");
    }

}
