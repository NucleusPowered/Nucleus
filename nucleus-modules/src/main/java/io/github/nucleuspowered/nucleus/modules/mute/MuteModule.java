/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.mute;

import io.github.nucleuspowered.nucleus.api.module.mute.data.Mute;
import io.github.nucleuspowered.nucleus.core.module.IModule;
import io.github.nucleuspowered.nucleus.modules.mute.commands.CheckMuteCommand;
import io.github.nucleuspowered.nucleus.modules.mute.commands.CheckMutedCommand;
import io.github.nucleuspowered.nucleus.modules.mute.commands.GlobalMuteCommand;
import io.github.nucleuspowered.nucleus.modules.mute.commands.MuteCommand;
import io.github.nucleuspowered.nucleus.modules.mute.commands.UnmuteCommand;
import io.github.nucleuspowered.nucleus.modules.mute.commands.VoiceCommand;
import io.github.nucleuspowered.nucleus.modules.mute.config.MuteConfig;
import io.github.nucleuspowered.nucleus.modules.mute.infoprovider.MuteInfoProvider;
import io.github.nucleuspowered.nucleus.modules.mute.listeners.MuteCommandListener;
import io.github.nucleuspowered.nucleus.modules.mute.listeners.MuteListener;
import io.github.nucleuspowered.nucleus.modules.mute.runnables.MuteTask;
import io.github.nucleuspowered.nucleus.modules.mute.services.MuteService;
import io.github.nucleuspowered.nucleus.core.scaffold.command.ICommandExecutor;
import io.github.nucleuspowered.nucleus.core.scaffold.listener.ListenerBase;
import io.github.nucleuspowered.nucleus.core.scaffold.task.TaskBase;
import io.github.nucleuspowered.nucleus.core.services.INucleusServiceCollection;
import io.github.nucleuspowered.nucleus.core.services.impl.playerinformation.NucleusProvider;
import io.github.nucleuspowered.nucleus.modules.mute.services.NucleusMute;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.placeholder.PlaceholderParser;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;

public class MuteModule implements IModule.Configurable<MuteConfig> { // ConfigurableModule<MuteConfig, MuteConfigAdapter> {

    public static final String ID = "mute";

    @Override
    public void init(final INucleusServiceCollection serviceCollection) {
        final MuteService service = new MuteService(serviceCollection);
        serviceCollection.registerService(MuteService.class, service, false);
        serviceCollection.userCacheService().setMutedProcessor(x -> x.get(MuteKeys.MUTE_DATA).isPresent());
        serviceCollection.placeholderService().registerToken(
                "muted",
                PlaceholderParser.builder()
                        .parser(p -> {
                            if (p.associatedObject().filter(x -> x instanceof ServerPlayer)
                                    .map(x -> serviceCollection.getServiceUnchecked(MuteService.class).isMuted(((ServerPlayer) x).uniqueId()))
                                    .orElse(false)) {
                                return Component.text("[Muted]", NamedTextColor.GRAY);
                            }
                            return Component.empty();
                        }).build()
        );

        serviceCollection.game().dataManager().registerBuilder(Mute.class, new NucleusMute.DataBuilder(service::isOnlineOnly));
    }

    @Override
    public Collection<Class<? extends ICommandExecutor>> getCommands() {
        return Arrays.asList(
                CheckMuteCommand.class,
                CheckMutedCommand.class,
                GlobalMuteCommand.class,
                MuteCommand.class,
                UnmuteCommand.class,
                VoiceCommand.class
        );
    }

    @Override
    public Optional<Class<?>> getPermissions() {
        return Optional.of(MutePermissions.class);
    }

    @Override
    public Collection<Class<? extends ListenerBase>> getListeners() {
        return Arrays.asList(
                MuteCommandListener.class,
                MuteListener.class
        );
    }

    @Override
    public Collection<Class<? extends TaskBase>> getAsyncTasks() {
        return Collections.singleton(MuteTask.class);
    }

    @Override
    public Optional<NucleusProvider> getInfoProvider() {
        return Optional.of(new MuteInfoProvider());
    }

    @Override
    public Class<MuteConfig> getConfigClass() {
        return MuteConfig.class;
    }
}
