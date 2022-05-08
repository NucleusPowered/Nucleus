/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.docgen.module.command;

import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import io.github.nucleuspowered.docgen.module.DocgenPermissions;
import io.github.nucleuspowered.docgen.module.service.DocumentationGenerationService;
import io.github.nucleuspowered.nucleus.core.core.CorePermissions;
import io.github.nucleuspowered.nucleus.core.guice.DataDirectory;
import io.github.nucleuspowered.nucleus.core.core.commands.NucleusCommand;
import io.github.nucleuspowered.nucleus.core.scaffold.command.ICommandContext;
import io.github.nucleuspowered.nucleus.core.scaffold.command.ICommandExecutor;
import io.github.nucleuspowered.nucleus.core.scaffold.command.ICommandResult;
import io.github.nucleuspowered.nucleus.core.scaffold.command.annotation.Command;
import io.github.nucleuspowered.nucleus.core.services.INucleusServiceCollection;
import net.kyori.adventure.text.Component;
import org.spongepowered.api.command.exception.CommandException;

import java.io.IOException;
import java.nio.file.Path;
import java.util.function.Supplier;

/**
 * Intended as a local command.
 */
@Command(
        aliases = "docgen",
        basePermission = DocgenPermissions.BASE_DOCGEN,
        commandDescriptionKey = "docgen"
)
public class DocGenCommand implements ICommandExecutor {

    @Override
    public ICommandResult execute(final ICommandContext context) throws CommandException {
        context.sendMessage("command.nucleus.docgen.start");
        final INucleusServiceCollection serviceCollection = context.getServiceCollection();
        final Path dataPath = serviceCollection.dataDir().get();
        try {
            DocumentationGenerationService.Holder.INSTANCE.generate(dataPath, context.getServiceCollection());
        } catch (final IOException e) {
            throw new CommandException(Component.text("Could not generate docs"), e);
        }
        context.sendMessage("command.nucleus.docgen.complete");
        return context.successResult();
    }

}
