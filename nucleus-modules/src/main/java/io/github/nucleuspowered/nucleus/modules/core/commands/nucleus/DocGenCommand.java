/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.core.commands.nucleus;

import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import io.github.nucleuspowered.nucleus.guice.DataDirectory;
import io.github.nucleuspowered.nucleus.modules.core.CorePermissions;
import io.github.nucleuspowered.nucleus.modules.core.commands.NucleusCommand;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandContext;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandExecutor;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandResult;
import io.github.nucleuspowered.nucleus.scaffold.command.annotation.Command;
import io.github.nucleuspowered.nucleus.services.INucleusServiceCollection;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.util.annotation.NonnullByDefault;

import java.io.IOException;
import java.nio.file.Path;
import java.util.function.Supplier;

/**
 * Intended as a local command.
 */
@SuppressWarnings("UnstableApiUsage")
@Command(
        aliases = "docgen",
        basePermission = CorePermissions.BASE_DOCGEN,
        commandDescriptionKey = "docgen",
        parentCommand = NucleusCommand.class
)
@NonnullByDefault
public class DocGenCommand implements ICommandExecutor<CommandSource> {

    @Override
    public ICommandResult execute(ICommandContext<? extends CommandSource> context) throws CommandException {
        context.sendMessage("command.nucleus.docgen.start");
        final INucleusServiceCollection serviceCollection = context.getServiceCollection();
        final Path dataPath = serviceCollection.injector().getInstance(Key.get(new TypeLiteral<Supplier<Path>>() {}, DataDirectory.class)).get();
        try {
            serviceCollection.documentationGenerationService().generate(dataPath);
        } catch (IOException | ObjectMappingException e) {
            throw new CommandException(Text.of("Could not generate docs"), e);
        }
        context.sendMessage("command.nucleus.docgen.complete");
        return context.successResult();
    }

}
