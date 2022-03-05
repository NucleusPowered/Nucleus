/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.home;

import io.github.nucleuspowered.nucleus.api.module.home.data.Home;
import io.github.nucleuspowered.nucleus.core.module.IModule;
import io.github.nucleuspowered.nucleus.modules.home.commands.DeleteHomeCommand;
import io.github.nucleuspowered.nucleus.modules.home.commands.HomeCommand;
import io.github.nucleuspowered.nucleus.modules.home.commands.HomeLimitCommand;
import io.github.nucleuspowered.nucleus.modules.home.commands.ListHomeCommand;
import io.github.nucleuspowered.nucleus.modules.home.commands.SetHomeCommand;
import io.github.nucleuspowered.nucleus.modules.home.config.HomeConfig;
import io.github.nucleuspowered.nucleus.modules.home.listeners.RespawnConditionalListener;
import io.github.nucleuspowered.nucleus.modules.home.services.HomeService;
import io.github.nucleuspowered.nucleus.core.scaffold.command.ICommandExecutor;
import io.github.nucleuspowered.nucleus.core.scaffold.listener.ListenerBase;
import io.github.nucleuspowered.nucleus.core.services.INucleusServiceCollection;
import io.github.nucleuspowered.nucleus.modules.home.services.NucleusHome;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;

public class HomeModule implements IModule.Configurable<HomeConfig> {

    public static final String ID = "home";

    @Override
    public void init(final INucleusServiceCollection serviceCollection) {
        serviceCollection.registerService(HomeService.class, new HomeService(serviceCollection), false);

        serviceCollection.game().dataManager().registerBuilder(Home.class, new NucleusHome.DataBuilder());
    }

    @Override
    public Collection<Class<? extends ICommandExecutor>> getCommands() {
        return Arrays.asList(
                DeleteHomeCommand.class,
                HomeCommand.class,
                HomeLimitCommand.class,
                ListHomeCommand.class,
                SetHomeCommand.class
        );
    }

    @Override public Optional<Class<?>> getPermissions() {
        return Optional.of(HomePermissions.class);
    }

    @Override public Collection<Class<? extends ListenerBase>> getListeners() {
        return Collections.singleton(RespawnConditionalListener.class);
    }

    @Override public Class<HomeConfig> getConfigClass() {
        return HomeConfig.class;
    }
}
