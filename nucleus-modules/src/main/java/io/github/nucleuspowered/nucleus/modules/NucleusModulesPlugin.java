/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules;

import com.google.inject.Inject;
import io.github.nucleuspowered.nucleus.core.event.RegisterModuleEvent;
import org.apache.logging.log4j.Logger;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.Order;
import org.spongepowered.plugin.builtin.jvm.Plugin;

@Plugin("nucleus-modules")
public final class NucleusModulesPlugin {

    private final Logger logger;

    @Inject
    public NucleusModulesPlugin(final Logger logger) {
        this.logger = logger;
    }

    @Listener(order = Order.FIRST)
    public void onModuleContainerRegistration(final RegisterModuleEvent event) {
        this.logger.info("Providing module information to Nucleus Core...");
        event.registerModuleProvider(new NucleusModuleProvider());
    }

}
