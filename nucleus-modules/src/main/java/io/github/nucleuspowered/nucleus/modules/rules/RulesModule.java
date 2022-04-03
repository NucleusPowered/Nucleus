/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.rules;

import com.google.inject.Inject;
import io.github.nucleuspowered.nucleus.core.io.TextFileController;
import io.github.nucleuspowered.nucleus.core.module.IModule;
import io.github.nucleuspowered.nucleus.core.services.interfaces.INucleusTextTemplateFactory;
import io.github.nucleuspowered.nucleus.core.services.interfaces.ITextFileControllerCollection;
import io.github.nucleuspowered.nucleus.modules.rules.commands.RulesCommand;
import io.github.nucleuspowered.nucleus.modules.rules.config.RulesConfig;
import io.github.nucleuspowered.nucleus.core.scaffold.command.ICommandExecutor;
import io.github.nucleuspowered.nucleus.core.scaffold.listener.ListenerBase;
import io.github.nucleuspowered.nucleus.core.services.INucleusServiceCollection;
import org.spongepowered.api.Engine;
import org.spongepowered.api.Server;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.lifecycle.StartingEngineEvent;
import org.spongepowered.api.event.lifecycle.StoppingEngineEvent;
import org.spongepowered.api.resource.ResourcePath;
import org.spongepowered.api.resource.pack.Pack;
import org.spongepowered.plugin.PluginContainer;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;

public class RulesModule implements IModule.Configurable<RulesConfig> {

    public static final String ID = "rules";

    private final ITextFileControllerCollection textFileControllerCollection;
    private final INucleusTextTemplateFactory textTemplateFactory;
    private final PluginContainer pluginContainer;
    private final Path configDir;

    @Inject
    public RulesModule(final INucleusServiceCollection nucleusServiceCollection) {
        this.pluginContainer = nucleusServiceCollection.pluginContainer();
        this.textFileControllerCollection = nucleusServiceCollection.textFileControllerCollection();
        this.textTemplateFactory = nucleusServiceCollection.textTemplateFactory();
        this.configDir = nucleusServiceCollection.configDir();
    }

    @Override
    public void init(final INucleusServiceCollection serviceCollection) {
        this.textFileControllerCollection.register(ID,
                new TextFileController(
                        this.pluginContainer,
                        this.textTemplateFactory,
                        "rules.txt",
                        this.configDir.resolve("rules.txt")
                ));
    }

    @Override public Collection<Class<? extends ICommandExecutor>> getCommands() {
        return Collections.singleton(RulesCommand.class);
    }

    @Override public Optional<Class<?>> getPermissions() {
        return Optional.of(RulesPermissions.class);
    }

    @Override public Collection<Class<? extends ListenerBase>> getListeners() {
        return Collections.emptyList();
    }

    @Override public Class<RulesConfig> getConfigClass() {
        return RulesConfig.class;
    }
}
