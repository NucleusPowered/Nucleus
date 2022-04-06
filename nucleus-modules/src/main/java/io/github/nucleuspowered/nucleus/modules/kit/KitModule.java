/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.kit;

import io.github.nucleuspowered.nucleus.api.module.kit.data.Kit;
import io.github.nucleuspowered.nucleus.core.module.IModule;
import io.github.nucleuspowered.nucleus.modules.kit.commands.KitAutoRedeemCommand;
import io.github.nucleuspowered.nucleus.modules.kit.commands.KitCommand;
import io.github.nucleuspowered.nucleus.modules.kit.commands.KitCostCommand;
import io.github.nucleuspowered.nucleus.modules.kit.commands.KitCreateCommand;
import io.github.nucleuspowered.nucleus.modules.kit.commands.KitEditCommand;
import io.github.nucleuspowered.nucleus.modules.kit.commands.KitGiveCommand;
import io.github.nucleuspowered.nucleus.modules.kit.commands.KitHiddenCommand;
import io.github.nucleuspowered.nucleus.modules.kit.commands.KitInfoCommand;
import io.github.nucleuspowered.nucleus.modules.kit.commands.KitListCommand;
import io.github.nucleuspowered.nucleus.modules.kit.commands.KitOneTimeCommand;
import io.github.nucleuspowered.nucleus.modules.kit.commands.KitPermissionBypassCommand;
import io.github.nucleuspowered.nucleus.modules.kit.commands.KitRedeemMessageCommand;
import io.github.nucleuspowered.nucleus.modules.kit.commands.KitReloadCommand;
import io.github.nucleuspowered.nucleus.modules.kit.commands.KitRemoveCommand;
import io.github.nucleuspowered.nucleus.modules.kit.commands.KitRenameCommand;
import io.github.nucleuspowered.nucleus.modules.kit.commands.KitResetUsageCommand;
import io.github.nucleuspowered.nucleus.modules.kit.commands.KitSetCommand;
import io.github.nucleuspowered.nucleus.modules.kit.commands.KitSetCooldownCommand;
import io.github.nucleuspowered.nucleus.modules.kit.commands.KitSetFirstJoinCommand;
import io.github.nucleuspowered.nucleus.modules.kit.commands.KitViewCommand;
import io.github.nucleuspowered.nucleus.modules.kit.commands.command.KitAddCommandCommand;
import io.github.nucleuspowered.nucleus.modules.kit.commands.command.KitClearCommandCommand;
import io.github.nucleuspowered.nucleus.modules.kit.commands.command.KitCommandCommand;
import io.github.nucleuspowered.nucleus.modules.kit.commands.command.KitRemoveCommandCommand;
import io.github.nucleuspowered.nucleus.modules.kit.config.KitConfig;
import io.github.nucleuspowered.nucleus.modules.kit.data.KitDataBuilder;
import io.github.nucleuspowered.nucleus.modules.kit.listeners.KitAutoRedeemListener;
import io.github.nucleuspowered.nucleus.modules.kit.listeners.KitListener;
import io.github.nucleuspowered.nucleus.modules.kit.services.KitService;
import io.github.nucleuspowered.nucleus.modules.kit.storage.KitStorageModule;
import io.github.nucleuspowered.nucleus.core.scaffold.command.ICommandExecutor;
import io.github.nucleuspowered.nucleus.core.scaffold.listener.ListenerBase;
import io.github.nucleuspowered.nucleus.core.scaffold.task.TaskBase;
import io.github.nucleuspowered.nucleus.core.services.INucleusServiceCollection;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.DataRegistration;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.lifecycle.RegisterDataEvent;
import org.spongepowered.api.event.lifecycle.RegisterRegistryValueEvent;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;

public class KitModule implements IModule.Configurable<KitConfig> {

    public static final String ID = "kit";

    @Override
    public Class<KitConfig> getConfigClass() {
        return KitConfig.class;
    }

    @Override
    public void init(final INucleusServiceCollection serviceCollection) {
        serviceCollection.storageManager().register(new KitStorageModule(serviceCollection));
        final KitService kitService = new KitService(serviceCollection);
        serviceCollection.registerService(KitService.class, kitService, false);
        // Data
        serviceCollection.game().dataManager().registerBuilder(Kit.class, new KitDataBuilder());
    }

    @Override
    public Collection<Class<? extends ICommandExecutor>> getCommands() {
        return Arrays.asList(
                // Command
                KitAddCommandCommand.class,
                KitClearCommandCommand.class,
                KitClearCommandCommand.class,
                KitRemoveCommandCommand.class,
                KitCommandCommand.class,

                // Kit
                KitAutoRedeemCommand.class,
                KitCommand.class,
                KitCostCommand.class,
                KitCreateCommand.class,
                KitEditCommand.class,
                KitGiveCommand.class,
                KitGiveCommand.class,
                KitHiddenCommand.class,
                KitInfoCommand.class,
                KitListCommand.class,
                KitOneTimeCommand.class,
                KitPermissionBypassCommand.class,
                KitRedeemMessageCommand.class,
                KitReloadCommand.class,
                KitRemoveCommand.class,
                KitRenameCommand.class,
                KitResetUsageCommand.class,
                KitSetCommand.class,
                KitSetCooldownCommand.class,
                KitSetFirstJoinCommand.class,
                KitViewCommand.class
        );
    }

    @Override
    public Optional<Class<?>> getPermissions() {
        return Optional.of(KitPermissions.class);
    }

    @Override
    public Collection<Class<? extends ListenerBase>> getListeners() {
        return Arrays.asList(
                KitAutoRedeemListener.class,
                KitListener.class
        );
    }

    @Override
    public Collection<Class<? extends TaskBase>> getAsyncTasks() {
        return Collections.emptyList();
    }

}
