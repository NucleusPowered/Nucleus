/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.world.commands;

import io.github.nucleuspowered.nucleus.modules.world.WorldPermissions;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandContext;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandExecutor;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandResult;
import io.github.nucleuspowered.nucleus.scaffold.command.NucleusParameters;
import io.github.nucleuspowered.nucleus.scaffold.command.annotation.Command;
import io.github.nucleuspowered.nucleus.services.INucleusServiceCollection;
import org.spongepowered.api.command.exception.CommandException;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.parameter.Parameter;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.world.storage.WorldProperties;

@Command(
        aliases = {"set"},
        basePermission = WorldPermissions.BASE_WORLD_GAMERULE_SET,
        commandDescriptionKey = "world.gamerule.set",
        parentCommand = GameruleCommand.class
)
public class SetGameruleCommand implements ICommandExecutor {

    private static final String gameRuleKey = "gamerule";
    private static final String valueKey = "value";

    @Override public Parameter[] parameters(final INucleusServiceCollection serviceCollection) {
        return new Parameter[] {
                NucleusParameters.OPTIONAL_WORLD_PROPERTIES_ENABLED_ONLY.get(serviceCollection),
                GenericArguments.string(Text.of(gameRuleKey)),
                GenericArguments.string(Text.of(valueKey))
        };
    }

    @Override public ICommandResult execute(final ICommandContext context) throws CommandException {
        final WorldProperties worldProperties = context.getWorldPropertiesOrFromSelfOptional(NucleusParameters.Keys.WORLD)
                .orElseThrow(() -> context.createException("command.world.player"));
        final String gameRule = context.requireOne(gameRuleKey, String.class);
        final String value = context.requireOne(valueKey, String.class);

        worldProperties.setGameRule(gameRule, value);

        context.sendMessage("command.world.gamerule.set.success", gameRule, value, worldProperties.getWorldName());
        return context.successResult();
    }
}
