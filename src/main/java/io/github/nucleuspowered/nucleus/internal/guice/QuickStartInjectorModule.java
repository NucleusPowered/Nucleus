/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.internal.guice;

import com.google.inject.AbstractModule;
import io.github.nucleuspowered.nucleus.NucleusPlugin;
import io.github.nucleuspowered.nucleus.config.CommandsConfig;
import io.github.nucleuspowered.nucleus.dataservices.ItemDataService;
import io.github.nucleuspowered.nucleus.dataservices.KitService;
import io.github.nucleuspowered.nucleus.internal.EconHelper;
import io.github.nucleuspowered.nucleus.internal.InternalServiceManager;
import io.github.nucleuspowered.nucleus.internal.PermissionRegistry;
import io.github.nucleuspowered.nucleus.internal.messages.MessageProvider;
import io.github.nucleuspowered.nucleus.internal.text.TextParsingUtils;
import org.slf4j.Logger;
import org.spongepowered.api.Game;
import org.spongepowered.api.Sponge;
import uk.co.drnaylor.quickstart.ModuleContainer;

public class QuickStartInjectorModule extends AbstractModule {

    private final NucleusPlugin plugin;

    public QuickStartInjectorModule(NucleusPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    protected void configure() {
        bind(NucleusPlugin.class).toProvider(() -> this.plugin);
        bind(Logger.class).toProvider(this.plugin::getLogger);
        bind(CommandsConfig.class).toProvider(this.plugin::getCommandsConfig);
        bind(Game.class).toProvider(Sponge::getGame);
        bind(PermissionRegistry.class).toProvider(this.plugin::getPermissionRegistry);
        bind(EconHelper.class).toProvider(this.plugin::getEconHelper);
        bind(ModuleContainer.class).toProvider(this.plugin::getModuleContainer);
        bind(InternalServiceManager.class).toProvider(this.plugin::getInternalServiceManager);
        bind(TextParsingUtils.class).toProvider(this.plugin::getTextParsingUtils);
        bind(MessageProvider.class).toProvider(this.plugin::getMessageProvider);
        bind(ItemDataService.class).toProvider(this.plugin::getItemDataService);
        bind(KitService.class).toProvider(this.plugin::getKitService);
    }
}
