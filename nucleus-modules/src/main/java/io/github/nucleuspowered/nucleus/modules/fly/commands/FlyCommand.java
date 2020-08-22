/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.fly.commands;

import io.github.nucleuspowered.nucleus.modules.fly.FlyKeys;
import io.github.nucleuspowered.nucleus.modules.fly.FlyPermissions;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandContext;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandExecutor;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandResult;
import io.github.nucleuspowered.nucleus.scaffold.command.NucleusParameters;
import io.github.nucleuspowered.nucleus.scaffold.command.annotation.Command;
import io.github.nucleuspowered.nucleus.scaffold.command.annotation.CommandModifier;
import io.github.nucleuspowered.nucleus.scaffold.command.annotation.EssentialsEquivalent;
import io.github.nucleuspowered.nucleus.scaffold.command.modifier.CommandModifiers;
import io.github.nucleuspowered.nucleus.services.INucleusServiceCollection;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.util.annotation.NonnullByDefault;

@EssentialsEquivalent("fly")
@NonnullByDefault
@Command(
        aliases = "fly",
        basePermission = FlyPermissions.BASE_FLY,
        commandDescriptionKey = "fly",
        modifiers = {
                @CommandModifier(value = CommandModifiers.HAS_COOLDOWN, exemptPermission = FlyPermissions.EXEMPT_COOLDOWN_FLY),
                @CommandModifier(value = CommandModifiers.HAS_WARMUP, exemptPermission = FlyPermissions.EXEMPT_WARMUP_FLY),
                @CommandModifier(value = CommandModifiers.HAS_COST, exemptPermission = FlyPermissions.EXEMPT_COST_FLY)
        },
        associatedPermissions = FlyPermissions.OTHERS_FLY
)
public class FlyCommand implements ICommandExecutor<CommandSource> { // extends AbstractCommand.SimpleTargetOtherPlayer {

    @Override public CommandElement[] parameters(INucleusServiceCollection serviceCollection) {
        return new CommandElement[] {
                serviceCollection.commandElementSupplier().createOtherUserPermissionElement(true, FlyPermissions.OTHERS_FLY),
                NucleusParameters.OPTIONAL_ONE_TRUE_FALSE
        };
    }

    @Override public ICommandResult execute(ICommandContext<? extends CommandSource> context) throws CommandException {
        Player player = context.getPlayerFromArgs();
        boolean fly = context.getOne(NucleusParameters.Keys.BOOL, Boolean.class).orElse(!player.get(Keys.CAN_FLY).orElse(false));

        if (!setFlying(player, fly)) {
            return context.errorResult("command.fly.error");
        }

        context.getServiceCollection().storageManager()
                .getOrCreateUser(player.getUniqueId()).thenAccept(x -> x.set(FlyKeys.FLY_TOGGLE, fly));
        if (!context.is(player)) {
            context.sendMessage(fly ? "command.fly.player.on" : "command.fly.player.off", player.getName());
        }

        context.sendMessage(fly ? "command.fly.on" : "command.fly.off");
        return context.successResult();
    }

    private boolean setFlying(Player pl, boolean fly) {
        // Only if we don't want to fly, offer IS_FLYING as false.
        return !(!fly && !pl.offer(Keys.IS_FLYING, false).isSuccessful()) && pl.offer(Keys.CAN_FLY, fly).isSuccessful();
    }

}
