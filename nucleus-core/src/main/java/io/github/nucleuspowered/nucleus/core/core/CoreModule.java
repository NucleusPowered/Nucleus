/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.core.core;

import io.github.nucleuspowered.nucleus.core.core.commands.CommandInfoCommand;
import io.github.nucleuspowered.nucleus.core.core.commands.NucleusCommand;
import io.github.nucleuspowered.nucleus.core.core.commands.SetNucleusLanguageCommand;
import io.github.nucleuspowered.nucleus.core.core.commands.nucleus.ClearCacheCommand;
import io.github.nucleuspowered.nucleus.core.core.commands.nucleus.CompatibilityCommand;
import io.github.nucleuspowered.nucleus.core.core.commands.nucleus.DebugCommand;
import io.github.nucleuspowered.nucleus.core.core.commands.nucleus.GetUserCommand;
import io.github.nucleuspowered.nucleus.core.core.commands.nucleus.InfoCommand;
import io.github.nucleuspowered.nucleus.core.core.commands.nucleus.MessagesUpdateCommand;
import io.github.nucleuspowered.nucleus.core.core.commands.nucleus.PrintPermsCommand;
import io.github.nucleuspowered.nucleus.core.core.commands.nucleus.RebuildUserCacheCommand;
import io.github.nucleuspowered.nucleus.core.core.commands.nucleus.ReloadCommand;
import io.github.nucleuspowered.nucleus.core.core.commands.nucleus.ResetFirstJoinCommand;
import io.github.nucleuspowered.nucleus.core.core.commands.nucleus.ResetUserCommand;
import io.github.nucleuspowered.nucleus.core.core.commands.nucleus.SaveCommand;
import io.github.nucleuspowered.nucleus.core.core.commands.nucleus.debug.RefreshUniqueVisitors;
import io.github.nucleuspowered.nucleus.core.core.commands.nucleus.debug.VerifyCommandDescriptionsCommand;
import io.github.nucleuspowered.nucleus.core.core.config.CoreConfig;
import io.github.nucleuspowered.nucleus.core.core.listeners.ChatChannelListener;
import io.github.nucleuspowered.nucleus.core.core.listeners.CoreListener;
import io.github.nucleuspowered.nucleus.core.core.listeners.WarmupListener;
import io.github.nucleuspowered.nucleus.core.core.runnables.CoreTask;
import io.github.nucleuspowered.nucleus.core.core.services.PlayerMetadataService;
import io.github.nucleuspowered.nucleus.core.core.services.UniqueUserService;
import io.github.nucleuspowered.nucleus.core.module.IModule;
import io.github.nucleuspowered.nucleus.core.scaffold.command.ICommandExecutor;
import io.github.nucleuspowered.nucleus.core.scaffold.listener.ListenerBase;
import io.github.nucleuspowered.nucleus.core.scaffold.task.TaskBase;
import io.github.nucleuspowered.nucleus.core.services.INucleusServiceCollection;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;

public class CoreModule implements IModule.Configurable<CoreConfig> {

    public static final String ID = "core";

    @Override
    public void init(final INucleusServiceCollection serviceCollection) {
        serviceCollection.registerService(UniqueUserService.class, new UniqueUserService(serviceCollection), false);
        serviceCollection.registerService(PlayerMetadataService.class, new PlayerMetadataService(serviceCollection), false);
    }

    @Override
    public Collection<Class<? extends ICommandExecutor>> getCommands() {
        return Arrays.asList(
                RefreshUniqueVisitors.class,
                VerifyCommandDescriptionsCommand.class,
                ClearCacheCommand.class,
                CompatibilityCommand.class,
                DebugCommand.class,
                GetUserCommand.class,
                InfoCommand.class,
                MessagesUpdateCommand.class,
                PrintPermsCommand.class,
                RebuildUserCacheCommand.class,
                ReloadCommand.class,
                ResetFirstJoinCommand.class,
                ResetUserCommand.class,
                SaveCommand.class,
                CommandInfoCommand.class,
                NucleusCommand.class,
                SetNucleusLanguageCommand.class
        );
    }

    @Override
    public Optional<Class<?>> getPermissions() {
        return Optional.of(CorePermissions.class);
    }

    @Override
    public Collection<Class<? extends ListenerBase>> getListeners() {
        return Arrays.asList(
                ChatChannelListener.class,
                CoreListener.class,
                WarmupListener.class
        );
    }

    @Override
    public Collection<Class<? extends TaskBase>> getAsyncTasks() {
        return Collections.singletonList(CoreTask.class);
    }

    @Override
    public Class<CoreConfig> getConfigClass() {
        return CoreConfig.class;
    }



}
