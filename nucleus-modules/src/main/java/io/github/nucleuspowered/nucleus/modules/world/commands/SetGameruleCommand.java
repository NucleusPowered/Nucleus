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
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.util.annotation.NonnullByDefault;
import org.spongepowered.api.world.storage.WorldProperties;

@NonnullByDefault
@Command(
        aliases = {"set"},
        basePermission = WorldPermissions.BASE_WORLD_GAMERULE_SET,
        commandDescriptionKey = "world.gamerule.set",
        parentCommand = GameruleCommand.class
)
public class SetGameruleCommand implements ICommandExecutor<CommandSource> {

    private static final String gameRuleKey = "gamerule";
    private static final String valueKey = "value";

    @Override public CommandElement[] parameters(INucleusServiceCollection serviceCollection) {
        return new CommandElement[] {
                NucleusParameters.OPTIONAL_WORLD_PROPERTIES_ENABLED_ONLY.get(serviceCollection),
                GenericArguments.string(Text.of(gameRuleKey)),
                GenericArguments.string(Text.of(valueKey))
        };
    }

    @Override public ICommandResult execute(ICommandContext<? extends CommandSource> context) throws CommandException {
        WorldProperties worldProperties = context.getWorldPropertiesOrFromSelf(NucleusParameters.Keys.WORLD)
                .orElseThrow(() -> context.createException("command.world.player"));
        String gameRule = context.requireOne(gameRuleKey, String.class);
        String value = context.requireOne(valueKey, String.class);

        worldProperties.setGameRule(gameRule, value);

        context.sendMessage("command.world.gamerule.set.success", gameRule, value, worldProperties.getWorldName());
        return context.successResult();
    }
}
