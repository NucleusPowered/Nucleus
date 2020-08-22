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
import org.spongepowered.api.util.annotation.NonnullByDefault;

/**
 * Sets kit cost.
 */
@Command(
        aliases = { "cost", "setcost" },
        basePermission = KitPermissions.BASE_KIT_COST,
        commandDescriptionKey = "kit.cost",
        parentCommand = KitCommand.class,
        async = true
)
@NonnullByDefault
public class KitCostCommand implements ICommandExecutor<CommandSource> {

    private final String costKey = "cost";

    @Override
    public CommandElement[] parameters(INucleusServiceCollection serviceCollection) {
        return new CommandElement[] {
                serviceCollection.getServiceUnchecked(KitService.class).createKitElement(false),
                GenericArguments.onlyOne(GenericArguments.doubleNum(Text.of(this.costKey)))
        };
    }

    @Override public ICommandResult execute(ICommandContext<? extends CommandSource> context) throws CommandException {
        Kit kit = context.requireOne(KitParameter.KIT_PARAMETER_KEY, Kit.class);
        double cost = context.requireOne(this.costKey, Double.class);

        if (cost < 0) {
            cost = 0;
        }

        kit.setCost(cost);
        context.getServiceCollection().getServiceUnchecked(KitService.class).saveKit(kit);
        context.sendMessage("command.kit.cost.success", kit.getName(), String.valueOf(cost));
        return context.successResult();
    }
}
