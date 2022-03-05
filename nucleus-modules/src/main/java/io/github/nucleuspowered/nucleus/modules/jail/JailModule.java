/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.jail;

import io.github.nucleuspowered.nucleus.api.module.jail.data.Jail;
import io.github.nucleuspowered.nucleus.api.module.jail.data.Jailing;
import io.github.nucleuspowered.nucleus.core.module.IModule;
import io.github.nucleuspowered.nucleus.modules.jail.commands.CheckJailCommand;
import io.github.nucleuspowered.nucleus.modules.jail.commands.CheckJailedCommand;
import io.github.nucleuspowered.nucleus.modules.jail.commands.DeleteJailCommand;
import io.github.nucleuspowered.nucleus.modules.jail.commands.JailCommand;
import io.github.nucleuspowered.nucleus.modules.jail.commands.JailTeleportCommand;
import io.github.nucleuspowered.nucleus.modules.jail.commands.JailsCommand;
import io.github.nucleuspowered.nucleus.modules.jail.commands.SetJailCommand;
import io.github.nucleuspowered.nucleus.modules.jail.commands.UnjailCommand;
import io.github.nucleuspowered.nucleus.modules.jail.config.JailConfig;
import io.github.nucleuspowered.nucleus.modules.jail.infoprovider.JailInfoProvider;
import io.github.nucleuspowered.nucleus.modules.jail.listeners.ChatJailListener;
import io.github.nucleuspowered.nucleus.modules.jail.listeners.InterceptTeleportListener;
import io.github.nucleuspowered.nucleus.modules.jail.listeners.JailListener;
import io.github.nucleuspowered.nucleus.modules.jail.runnables.JailTask;
import io.github.nucleuspowered.nucleus.modules.jail.services.JailService;
import io.github.nucleuspowered.nucleus.core.scaffold.command.ICommandExecutor;
import io.github.nucleuspowered.nucleus.core.scaffold.listener.ListenerBase;
import io.github.nucleuspowered.nucleus.core.scaffold.task.TaskBase;
import io.github.nucleuspowered.nucleus.core.services.INucleusServiceCollection;
import io.github.nucleuspowered.nucleus.core.services.impl.playerinformation.NucleusProvider;
import io.github.nucleuspowered.nucleus.core.services.interfaces.IPlaceholderService;
import io.github.nucleuspowered.nucleus.modules.jail.services.NucleusJail;
import io.github.nucleuspowered.nucleus.modules.jail.services.NucleusJailing;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.placeholder.PlaceholderParser;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;

public class JailModule implements IModule.Configurable<JailConfig> {

    public static final String ID = "jail";

    @Override
    public void init(final INucleusServiceCollection serviceCollection) {
        final JailService handler = new JailService(serviceCollection);
        serviceCollection.registerService(JailService.class, handler, false);
        serviceCollection.userCacheService().setJailProcessor(x -> x.get(JailKeys.JAIL_DATA).map(Jailing::getJailName).orElse(null));
        final IPlaceholderService placeholderService = serviceCollection.placeholderService();
        placeholderService.registerToken("jailed", PlaceholderParser.builder()
                .parser(p -> {
                    if (p.associatedObject().filter(x -> x instanceof ServerPlayer)
                            .map(x -> handler.isPlayerJailed(((ServerPlayer) x).uniqueId()))
                            .orElse(false)) {
                        return Component.text("[Jailed]", NamedTextColor.GRAY);
                    }
                    return Component.empty();
                }).build());
        placeholderService.registerToken("jail", PlaceholderParser.builder()
                .parser(placeholder -> {
                    if (placeholder.associatedObject().filter(x -> x instanceof ServerPlayer).isPresent()) {
                        return handler
                                .getPlayerJailData(((ServerPlayer) placeholder.associatedObject().get()).uniqueId())
                                .<Component>map(x -> Component.text(x.getJailName()))
                                .orElseGet(Component::empty);
                    }

                    return Component.empty();
                }).build());

        serviceCollection.game().dataManager().registerBuilder(Jail.class, new NucleusJail.DataBuilder());
        serviceCollection.game().dataManager().registerBuilder(Jailing.class, new NucleusJailing.DataBuilder(handler::isOnlineOnly));
    }

    @Override
    public Optional<NucleusProvider> getInfoProvider() {
        return Optional.of(new JailInfoProvider());
    }

    @Override
    public Collection<Class<? extends ICommandExecutor>> getCommands() {
        return Arrays.asList(
                CheckJailCommand.class,
                CheckJailedCommand.class,
                DeleteJailCommand.class,
                JailCommand.class,
                JailsCommand.class,
                JailTeleportCommand.class,
                SetJailCommand.class,
                UnjailCommand.class
        );
    }

    @Override
    public Optional<Class<?>> getPermissions() {
        return Optional.of(JailPermissions.class);
    }

    @Override
    public Collection<Class<? extends ListenerBase>> getListeners() {
        return Arrays.asList(
                ChatJailListener.class,
                InterceptTeleportListener.class,
                JailListener.class
        );
    }

    @Override
    public Class<JailConfig> getConfigClass() {
        return JailConfig.class;
    }

    @Override
    public Collection<Class<? extends TaskBase>> getAsyncTasks() {
        return Collections.singleton(JailTask.class);
    }
}
