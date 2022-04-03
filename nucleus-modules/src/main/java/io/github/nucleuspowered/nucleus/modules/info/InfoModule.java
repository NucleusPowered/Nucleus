/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.info;

import io.github.nucleuspowered.nucleus.core.io.TextFileController;
import io.github.nucleuspowered.nucleus.core.module.IModule;
import io.github.nucleuspowered.nucleus.modules.info.commands.InfoCommand;
import io.github.nucleuspowered.nucleus.modules.info.commands.MotdCommand;
import io.github.nucleuspowered.nucleus.modules.info.config.InfoConfig;
import io.github.nucleuspowered.nucleus.modules.info.listeners.InfoListener;
import io.github.nucleuspowered.nucleus.modules.info.services.InfoHandler;
import io.github.nucleuspowered.nucleus.core.scaffold.command.ICommandExecutor;
import io.github.nucleuspowered.nucleus.core.scaffold.listener.ListenerBase;
import io.github.nucleuspowered.nucleus.core.services.INucleusServiceCollection;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.resource.ResourcePath;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;

public class InfoModule implements IModule.Configurable<InfoConfig> {

    public static final String ID = "info";
    public static final String MOTD_KEY = "motd";

    @Override
    public void init(final INucleusServiceCollection serviceCollection) {
        serviceCollection.registerService(InfoHandler.class, new InfoHandler(), false);
        final TextFileController motdController = new TextFileController(
                serviceCollection.pluginContainer(),
                serviceCollection.textTemplateFactory(),
                "motd.txt",
                serviceCollection.configDir().resolve("motd.txt"));
        serviceCollection.textFileControllerCollection().register(InfoModule.MOTD_KEY, motdController);

    }

    @Override
    public Collection<Class<? extends ICommandExecutor>> getCommands() {
        return Arrays.asList(
                InfoCommand.class,
                MotdCommand.class
        );
    }

    @Override
    public Optional<Class<?>> getPermissions() {
        return Optional.of(InfoPermissions.class);
    }

    @Override
    public Collection<Class<? extends ListenerBase>> getListeners() {
        return Collections.singleton(InfoListener.class);
    }

    @Override
    public Class<InfoConfig> getConfigClass() {
        return InfoConfig.class;
    }

    @Override
    public InfoConfig createInstance() {
        return new InfoConfig();
    }

}
