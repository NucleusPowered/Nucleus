/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.freezeplayer.commands;

import io.github.nucleuspowered.nucleus.modules.freezeplayer.FreezePlayerPermissions;
import io.github.nucleuspowered.nucleus.modules.freezeplayer.services.FreezePlayerService;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandContext;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandExecutor;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandResult;
import io.github.nucleuspowered.nucleus.scaffold.command.NucleusParameters;
import io.github.nucleuspowered.nucleus.scaffold.command.annotation.Command;
import io.github.nucleuspowered.nucleus.scaffold.command.annotation.CommandModifier;
import io.github.nucleuspowered.nucleus.scaffold.command.modifier.CommandModifiers;
import io.github.nucleuspowered.nucleus.services.INucleusServiceCollection;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.util.annotation.NonnullByDefault;

@NonnullByDefault
@Command(
        aliases = {"freezeplayer", "freeze"},
        basePermission = FreezePlayerPermissions.BASE_FREEZEPLAYER,
        commandDescriptionKey = "freezeplayer",
        async = true,
        modifiers = {
                @CommandModifier(value = CommandModifiers.HAS_COOLDOWN, exemptPermission = FreezePlayerPermissions.EXEMPT_COOLDOWN_FREEZEPLAYER),
                @CommandModifier(value = CommandModifiers.HAS_WARMUP, exemptPermission =  FreezePlayerPermissions.EXEMPT_WARMUP_FREEZEPLAYER),
                @CommandModifier(value = CommandModifiers.HAS_COST, exemptPermission = FreezePlayerPermissions.EXEMPT_COST_FREEZEPLAYER)
        },
        associatedPermissions = FreezePlayerPermissions.OTHERS_FREEZEPLAYER
)
public class FreezePlayerCommand implements ICommandExecutor<CommandSource> {

    @Override
    public CommandElement[] parameters(INucleusServiceCollection serviceCollection) {
        return new CommandElement[] {
                serviceCollection.commandElementSupplier().createOtherUserPermissionElement(false, FreezePlayerPermissions.OTHERS_FREEZEPLAYER),
                GenericArguments.optional(NucleusParameters.ONE_TRUE_FALSE)
        };
    }

    @Override public ICommandResult execute(ICommandContext<? extends CommandSource> context) throws CommandException {
        User pl = context.getUserFromArgs();
        FreezePlayerService service = context.getServiceCollection().getServiceUnchecked(FreezePlayerService.class);
        final boolean f = context.getOne(NucleusParameters.Keys.BOOL, Boolean.class).orElseGet(() -> !service.isFrozen(pl));
        service.setFrozen(pl, f);
        context.sendMessage(
            f ? "command.freezeplayer.success.frozen" : "command.freezeplayer.success.unfrozen",
                context.getServiceCollection().playerDisplayNameService().getDisplayName(pl));
        return context.successResult();
    }
}
