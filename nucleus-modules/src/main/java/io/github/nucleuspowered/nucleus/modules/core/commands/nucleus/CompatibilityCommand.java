/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.core.commands.nucleus;

import io.github.nucleuspowered.nucleus.Util;
import io.github.nucleuspowered.nucleus.modules.core.CorePermissions;
import io.github.nucleuspowered.nucleus.modules.core.commands.NucleusCommand;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandContext;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandExecutor;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandResult;
import io.github.nucleuspowered.nucleus.scaffold.command.annotation.Command;
import io.github.nucleuspowered.nucleus.services.interfaces.ICompatibilityService;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.text.Text;

import java.util.Collection;
import java.util.Comparator;

@Command(
        aliases = { "compatibility", "compat" },
        basePermission = CorePermissions.BASE_NUCLEUS_COMPATIBILITY,
        commandDescriptionKey = "nucleus.compatibility",
        parentCommand = NucleusCommand.class,
        async = true
)
public class CompatibilityCommand implements ICommandExecutor<CommandSource> {

    @Override
    public ICommandResult execute(final ICommandContext<? extends CommandSource> context) {
        ICompatibilityService compatibilityService = context.getServiceCollection().compatibilityService();
        Collection<ICompatibilityService.CompatibilityMessages> messages = compatibilityService.getApplicableMessages();
        if (messages.isEmpty()) {
            context.sendMessage("command.nucleus.compat.none");
            return context.successResult();
        }

        // Create pagination
        Text text = messages.stream()
                .sorted(Comparator.comparing(x -> -x.getSeverity().getIndex()))
                .map(x -> {
                    Text modulesAffected =
                            x.getModules().isEmpty() ?
                                    context.getMessage("command.nucleus.compat.all") :
                                    Text.of(String.join(" ,", x.getModules()));
                    return Text.joinWith(Text.NEW_LINE,
                        context.getMessage("command.nucleus.compat.severity.base",
                            "loc:command.nucleus.compat.severity." + x.getSeverity().name().toLowerCase()),
                            context.getMessage("command.nucleus.compat.modulesaffected", modulesAffected),
                            context.getMessage("command.nucleus.compat.mod", x.getModId()),
                            context.getMessage("command.nucleus.compat.symptom", x.getSeverity()),
                            context.getMessage("command.nucleus.compat.message", x.getMessage()),
                            context.getMessage("command.nucleus.compat.resolution", x.getResolution())
                    );
                })
                .reduce((text1, text2) -> Text.of(text1, Text.NEW_LINE, Text.NEW_LINE, text2))
                .orElse(Text.EMPTY);
        Util.getPaginationBuilder(context.getCommandSourceUnchecked())
                .header(context.getMessage("command.nucleus.compat.header"))
                .contents(text)
                .sendTo(context.getCommandSourceUnchecked());
        return context.successResult();
    }
}