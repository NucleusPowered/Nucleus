/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.world.commands;

import io.github.nucleuspowered.nucleus.Util;
import io.github.nucleuspowered.nucleus.internal.annotations.command.NoModifiers;
import io.github.nucleuspowered.nucleus.internal.annotations.command.Permissions;
import io.github.nucleuspowered.nucleus.internal.annotations.command.RegisterCommand;
import io.github.nucleuspowered.nucleus.internal.command.AbstractCommand;
import io.github.nucleuspowered.nucleus.internal.permissions.SuggestedLevel;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.action.TextActions;
import org.spongepowered.api.text.serializer.TextSerializers;
import org.spongepowered.api.util.annotation.NonnullByDefault;
import org.spongepowered.api.world.storage.WorldProperties;

import java.text.MessageFormat;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@NoModifiers
@NonnullByDefault
@Permissions(prefix = "world", suggestedLevel = SuggestedLevel.ADMIN)
@RegisterCommand(value = { "gamerule" }, subcommandOf = WorldCommand.class, rootAliasRegister = "gamerules")
public class GameruleCommand extends AbstractCommand<CommandSource> {

    private static final String worldKey = "world";

    @Override public CommandElement[] getArguments() {
        return new CommandElement[] {
            GenericArguments.optionalWeak(GenericArguments.onlyOne(GenericArguments.world(Text.of(worldKey))))
        };
    }

    @Override public CommandResult executeCommand(CommandSource src, CommandContext args) throws Exception {
        WorldProperties worldProperties = getWorldFromUserOrArgs(src, worldKey, args);
        Map<String, String> gameRules = worldProperties.getGameRules();

        String message = plugin.getMessageProvider().getMessageWithFormat("command.world.gamerule.key");
        List<Text> text = gameRules.entrySet().stream().sorted(Comparator.comparing(Map.Entry::getKey))
            .map(x -> Text.of(
                TextActions.suggestCommand(String.format("/world gamerule set %s %s ", worldProperties.getWorldName(), x.getKey())),
                TextSerializers.FORMATTING_CODE.deserialize(MessageFormat.format(message, x.getKey(), x.getValue()))
            ))
            .collect(Collectors.toList());

        Util.getPaginationBuilder(src)
            .title(plugin.getMessageProvider().getTextMessageWithFormat("command.world.gamerule.header", worldProperties.getWorldName()))
            .contents(text)
            .sendTo(src);

        return CommandResult.success();
    }
}
