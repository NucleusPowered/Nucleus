/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.world.commands;

import io.github.nucleuspowered.nucleus.argumentparsers.NucleusWorldPropertiesArgument;
import io.github.nucleuspowered.nucleus.internal.annotations.command.NoModifiers;
import io.github.nucleuspowered.nucleus.internal.annotations.command.Permissions;
import io.github.nucleuspowered.nucleus.internal.annotations.command.RegisterCommand;
import io.github.nucleuspowered.nucleus.internal.command.AbstractCommand;
import io.github.nucleuspowered.nucleus.internal.command.ReturnMessageException;
import io.github.nucleuspowered.nucleus.internal.permissions.SuggestedLevel;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.util.annotation.NonnullByDefault;
import org.spongepowered.api.world.storage.WorldProperties;

@NonnullByDefault
@NoModifiers
@Permissions(prefix = "world", suggestedLevel = SuggestedLevel.NONE)
@RegisterCommand(value = "rename", subcommandOf = WorldCommand.class)
public class RenameWorldCommand extends AbstractCommand<CommandSource> {

    private final String worldKey = "world";
    private final String newNameKey = "new name";

    @Override protected CommandElement[] getArguments() {
        return new CommandElement[] {
                new NucleusWorldPropertiesArgument(Text.of(this.worldKey), NucleusWorldPropertiesArgument.Type.UNLOADED_ONLY),
                GenericArguments.string(Text.of(this.newNameKey))
        };
    }

    @Override
    protected CommandResult executeCommand(CommandSource src, CommandContext args) throws Exception {
        WorldProperties worldProperties = args.<WorldProperties>getOne(this.worldKey).get();
        String oldName = worldProperties.getWorldName();
        String newName =  args.<String>getOne(this.newNameKey).get();
        if (Sponge.getServer().renameWorld(worldProperties, newName).isPresent()) {
            src.sendMessage(this.plugin.getMessageProvider().getTextMessageWithFormat("command.world.rename.success", oldName, newName));
            return CommandResult.success();
        }

        throw ReturnMessageException.fromKey("command.world.rename.failed", worldProperties.getWorldName(), newName);
    }
}
