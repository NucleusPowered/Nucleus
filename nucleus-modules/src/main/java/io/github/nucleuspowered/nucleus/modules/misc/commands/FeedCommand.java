/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.misc.commands;

import io.github.nucleuspowered.nucleus.modules.misc.MiscPermissions;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandContext;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandExecutor;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandResult;
import io.github.nucleuspowered.nucleus.scaffold.command.annotation.Command;
import io.github.nucleuspowered.nucleus.scaffold.command.annotation.CommandModifier;
import io.github.nucleuspowered.nucleus.scaffold.command.annotation.EssentialsEquivalent;
import io.github.nucleuspowered.nucleus.scaffold.command.modifier.CommandModifiers;
import io.github.nucleuspowered.nucleus.services.INucleusServiceCollection;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.data.manipulator.mutable.entity.FoodData;
import org.spongepowered.api.data.value.mutable.Value;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.util.annotation.NonnullByDefault;

@Command(
        aliases = {"feed", "eat"},
        basePermission = MiscPermissions.BASE_FEED,
        commandDescriptionKey = "feed",
        modifiers = {
                @CommandModifier(value = CommandModifiers.HAS_COOLDOWN, exemptPermission = MiscPermissions.EXEMPT_COOLDOWN_FEED),
                @CommandModifier(value = CommandModifiers.HAS_WARMUP, exemptPermission = MiscPermissions.EXEMPT_WARMUP_FEED),
                @CommandModifier(value = CommandModifiers.HAS_COST, exemptPermission = MiscPermissions.EXEMPT_COST_FEED)
        },
        associatedPermissions = MiscPermissions.OTHERS_FEED
)
@EssentialsEquivalent({"feed", "eat"})
@NonnullByDefault
public class FeedCommand implements ICommandExecutor<CommandSource> {

    @Override public CommandElement[] parameters(INucleusServiceCollection serviceCollection) {
        return new CommandElement[] {
                serviceCollection.commandElementSupplier()
                        .createOnlyOtherUserPermissionElement(true, MiscPermissions.OTHERS_FEED)
        };
    }

    @Override public ICommandResult execute(ICommandContext<? extends CommandSource> context) throws CommandException {
        Player pl = context.getPlayerFromArgs();
        // Get the food data and modify it.
        FoodData foodData = pl.getFoodData();
        Value<Integer> f = foodData.foodLevel().set(foodData.foodLevel().getDefault());
        Value<Double> d = foodData.saturation().set(foodData.saturation().getDefault());
        foodData.set(f, d);

        if (pl.offer(foodData).isSuccessful()) {
            context.sendMessageTo(pl, "command.feed.success.self");
            if (!context.is(pl)) {
                context.sendMessage("command.feed.success.other", pl.getName());
            }

            return context.successResult();
        } else {
            return context.errorResult("command.feed.error");
        }
    }
}
