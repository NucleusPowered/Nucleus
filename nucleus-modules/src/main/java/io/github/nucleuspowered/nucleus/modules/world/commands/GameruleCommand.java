/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.world.commands;

import io.github.nucleuspowered.nucleus.Util;
import io.github.nucleuspowered.nucleus.modules.world.WorldPermissions;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandContext;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandExecutor;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandResult;
import io.github.nucleuspowered.nucleus.scaffold.command.NucleusParameters;
import io.github.nucleuspowered.nucleus.scaffold.command.annotation.Command;
import io.github.nucleuspowered.nucleus.services.INucleusServiceCollection;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandElement;
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

@NonnullByDefault
@Command(
        aliases = {"gamerule"},
        basePermission = WorldPermissions.BASE_WORLD_GAMERULE,
        commandDescriptionKey = "world.gamerule",
        parentCommand = WorldCommand.class
)
public class GameruleCommand implements ICommandExecutor<CommandSource> {

    private static final String worldKey = "world";

    @Override public CommandElement[] parameters(INucleusServiceCollection serviceCollection) {
        return new CommandElement[] {
                NucleusParameters.OPTIONAL_WORLD_PROPERTIES_ENABLED_ONLY.get(serviceCollection)
        };
    }

    @Override public ICommandResult execute(ICommandContext<? extends CommandSource> context) throws CommandException {
        WorldProperties worldProperties = context.getWorldPropertiesOrFromSelf(NucleusParameters.Keys.WORLD)
                .orElseThrow(() -> context.createException("command.world.player"));
        Map<String, String> gameRules = worldProperties.getGameRules();

        String message = context.getMessageString("command.world.gamerule.key");
        List<Text> text = gameRules.entrySet().stream().sorted(Comparator.comparing(Map.Entry::getKey))
            .map(x -> Text.of(
                TextActions.suggestCommand(String.format("/world gamerule set %s %s ", worldProperties.getWorldName(), x.getKey())),
                TextSerializers.FORMATTING_CODE.deserialize(MessageFormat.format(message, x.getKey(), x.getValue()))
            ))
            .collect(Collectors.toList());

        Util.getPaginationBuilder(context.getCommandSourceUnchecked())
            .title(context.getMessage("command.world.gamerule.header", worldProperties.getWorldName()))
            .contents(text)
            .sendTo(context.getCommandSourceUnchecked());

        return context.successResult();
    }
}
