/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.services.interfaces;

import com.google.inject.ImplementedBy;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandExecutor;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandInterceptor;
import io.github.nucleuspowered.nucleus.scaffold.command.annotation.Command;
import io.github.nucleuspowered.nucleus.scaffold.command.control.CommandControl;
import io.github.nucleuspowered.nucleus.services.INucleusServiceCollection;
import io.github.nucleuspowered.nucleus.services.impl.commandmetadata.CommandMetadataService;
import org.spongepowered.api.command.CommandSource;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

@ImplementedBy(CommandMetadataService.class)
public interface ICommandMetadataService {

    void registerCommand(
            String id,
            String name,
            Command command,
            Class<? extends ICommandExecutor<?>> associatedContext
    );

    void completeRegistrationPhase(INucleusServiceCollection serviceCollection);

    default void addMapping(Map<String, String> mappings) {
        for (Map.Entry<String, String> entry : mappings.entrySet()) {
            addMapping(entry.getKey(), entry.getValue());
        }
    }

    void addMapping(String newCommand, String remapped);

    void activate();

    void deactivate();

    boolean isNucleusCommand(String command);

    Optional<CommandControl> getControl(Class<? extends ICommandExecutor<? extends CommandSource>> executorClass);

    Collection<CommandControl> getCommands();

    Collection<CommandControl> getCommandsAndSubcommands();

    void registerInterceptor(ICommandInterceptor impl);

    Collection<ICommandInterceptor> interceptors();
}
