/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.kit.commands;

import io.github.nucleuspowered.nucleus.api.module.kit.data.Kit;
import io.github.nucleuspowered.nucleus.modules.kit.KitPermissions;
import io.github.nucleuspowered.nucleus.modules.kit.parameters.KitParameter;
import io.github.nucleuspowered.nucleus.modules.kit.services.KitService;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandContext;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandExecutor;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandResult;
import io.github.nucleuspowered.nucleus.scaffold.command.annotation.Command;
import io.github.nucleuspowered.nucleus.services.INucleusServiceCollection;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.util.annotation.NonnullByDefault;

@NonnullByDefault
@Command(
        aliases = { "rename" },
        async = true,
        basePermission = KitPermissions.BASE_KIT_RENAME,
        commandDescriptionKey = "kit.rename",
        parentCommand = KitCommand.class
)
public class KitRenameCommand implements ICommandExecutor<CommandSource> {

    private final String name = "target name";

    @Override
    public CommandElement[] parameters(INucleusServiceCollection serviceCollection) {
        return new CommandElement[] {
                serviceCollection.getServiceUnchecked(KitService.class).createKitElement(false),
                GenericArguments.onlyOne(GenericArguments.string(Text.of(this.name)))
        };
    }

    @Override public ICommandResult execute(ICommandContext<? extends CommandSource> context) throws CommandException {
        try {
            String name1 = context.requireOne(KitParameter.KIT_PARAMETER_KEY, Kit.class).getName();
            String name2 = context.requireOne(this.name, String.class);
            context.getServiceCollection().getServiceUnchecked(KitService.class).renameKit(name1, name2);
            context.sendMessage("command.kit.rename.renamed", name1, name2);
            return context.successResult();
        } catch (IllegalArgumentException e) {
            return context.errorResultLiteral(Text.of(TextColors.RED, e.getMessage()));
        }
    }
}
