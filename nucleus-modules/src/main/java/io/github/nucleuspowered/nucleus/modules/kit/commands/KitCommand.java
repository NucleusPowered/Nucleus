/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.kit.commands;

import io.github.nucleuspowered.nucleus.Util;
import io.github.nucleuspowered.nucleus.api.module.kit.KitRedeemResult;
import io.github.nucleuspowered.nucleus.api.module.kit.data.Kit;
import io.github.nucleuspowered.nucleus.modules.kit.KitPermissions;
import io.github.nucleuspowered.nucleus.modules.kit.config.KitConfig;
import io.github.nucleuspowered.nucleus.modules.kit.parameters.KitParameter;
import io.github.nucleuspowered.nucleus.modules.kit.services.KitService;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandContext;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandExecutor;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandResult;
import io.github.nucleuspowered.nucleus.scaffold.command.annotation.Command;
import io.github.nucleuspowered.nucleus.scaffold.command.annotation.CommandModifier;
import io.github.nucleuspowered.nucleus.scaffold.command.annotation.EssentialsEquivalent;
import io.github.nucleuspowered.nucleus.scaffold.command.modifier.CommandModifiers;
import io.github.nucleuspowered.nucleus.services.INucleusServiceCollection;
import io.github.nucleuspowered.nucleus.services.interfaces.IEconomyServiceProvider;
import io.github.nucleuspowered.nucleus.services.interfaces.IReloadableService;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.util.annotation.NonnullByDefault;

import java.time.Duration;

/**
 * Allows a user to redeem a kit.
 */
@NonnullByDefault
@Command(
        aliases = { "kit" },
        basePermission = KitPermissions.BASE_KIT,
        commandDescriptionKey = "kit",
        modifiers = {
                // Cooldowns and cost are determined by the kit itself.
                @CommandModifier(value = CommandModifiers.HAS_WARMUP, exemptPermission = KitPermissions.EXEMPT_WARMUP_KIT)
        },
        associatedPermissions = {
                KitPermissions.KITS,
                KitPermissions.KIT_EXEMPT_ONETIME,
                KitPermissions.KIT_EXEMPT_COOLDOWN,
                KitPermissions.KIT_EXEMPT_COST
        }
)
@EssentialsEquivalent(value = "kit, kits", isExact = false, notes = "'/kit' redeems, '/kits' lists.")
public class KitCommand implements ICommandExecutor<Player>, IReloadableService.Reloadable {

    private boolean isDrop;
    private boolean mustGetAll;

    @Override
    public CommandElement[] parameters(INucleusServiceCollection serviceCollection) {
        return new CommandElement[] {
                serviceCollection.getServiceUnchecked(KitService.class).createKitElement(true)
        };
    }

    @Override public ICommandResult execute(ICommandContext<? extends Player> context) throws CommandException {
        Kit kit = context.requireOne(KitParameter.KIT_PARAMETER_KEY, Kit.class);

        KitService kitService = context.getServiceCollection().getServiceUnchecked(KitService.class);
        Player player = context.getCommandSourceAsPlayerUnchecked();
        IEconomyServiceProvider econHelper = context.getServiceCollection().economyServiceProvider();
        double cost = econHelper.serviceExists() ? kit.getCost() : 0;
        if (context.testPermission(KitPermissions.KIT_EXEMPT_COST)) {
            // If exempt - no cost.
            cost = 0;
        }

        // If we have a cost for the kit, check we have funds.
        if (cost > 0 && !econHelper.hasBalance(player, cost)) {
            return context.errorResult("command.kit.notenough", kit.getName(), econHelper.getCurrencySymbol(cost));
        }

        KitRedeemResult redeemResult = kitService.redeemKit(kit, player, true, this.mustGetAll);
        if (redeemResult.isSuccess()) {
            if (!redeemResult.rejectedItems().isEmpty()) {
                // If we drop them, tell the user
                if (this.isDrop) {
                    context.sendMessage("command.kit.itemsdropped");
                    redeemResult.rejectedItems().forEach(x -> Util.dropItemOnFloorAtLocation(x, player.getLocation()));
                } else {
                    context.sendMessage("command.kit.fullinventory");
                }
            }

            if (kit.isDisplayMessageOnRedeem()) {
                context.sendMessage("command.kit.spawned", kit.getName());
            }

            // Charge, if necessary
            if (cost > 0 && econHelper.serviceExists()) {
                econHelper.withdrawFromPlayer(player, cost);
            }

            return context.successResult();
        } else {
            switch (redeemResult.getStatus()) {
                case ALREADY_REDEEMED_ONE_TIME:
                    return context.errorResult("command.kit.onetime.alreadyredeemed", kit.getName());
                case COOLDOWN_NOT_EXPIRED:
                    return context.errorResult("command.kit.cooldown",
                            context.getTimeString(redeemResult.getCooldownDuration().orElse(Duration.ZERO)),
                            kit.getName());
                case PRE_EVENT_CANCELLED:
                    return redeemResult
                            .getMessage()
                            .map(x -> context.errorResultLiteral(Text.of(x)))
                            .orElseGet(() -> context.errorResult("command.kit.cancelledpre", kit.getName()));
                case NO_SPACE:
                    return context.errorResult("command.kit.fullinventorynosave", kit.getName());
                case UNKNOWN:
                default:
                    return context.errorResult("command.kit.fail", kit.getName());
            }
        }

    }

    @Override
    public void onReload(INucleusServiceCollection serviceCollection) {
        KitConfig kca = serviceCollection.moduleDataProvider().getModuleConfig(KitConfig.class);
        this.isDrop = kca.isDropKitIfFull();
        this.mustGetAll = kca.isMustGetAll();
    }
}
