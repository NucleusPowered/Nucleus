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
import io.github.nucleuspowered.nucleus.scaffold.command.NucleusParameters;
import io.github.nucleuspowered.nucleus.scaffold.command.annotation.Command;
import io.github.nucleuspowered.nucleus.services.INucleusServiceCollection;
import io.github.nucleuspowered.nucleus.services.interfaces.IReloadableService;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.util.annotation.NonnullByDefault;

import java.time.Duration;

/**
 * Gives a kit to a subject.
 */
@Command(
        aliases = { "give" },
        basePermission = KitPermissions.BASE_KIT_GIVE,
        commandDescriptionKey = "kit.give",
        parentCommand = KitCommand.class,
        associatedPermissions = KitPermissions.KIT_GIVE_OVERRIDE
)
@NonnullByDefault
public class KitGiveCommand implements ICommandExecutor<CommandSource>, IReloadableService.Reloadable {

    private boolean mustGetAll;
    private boolean isDrop;

    @Override
    public CommandElement[] parameters(INucleusServiceCollection serviceCollection) {
        return new CommandElement[] {
                GenericArguments.flags().permissionFlag(KitPermissions.KIT_GIVE_OVERRIDE, "i", "-ignore")
                    .buildWith(GenericArguments.seq(
                        NucleusParameters.ONE_PLAYER.get(serviceCollection),
                        serviceCollection.getServiceUnchecked(KitService.class).createKitElement(false)
                ))
        };
    }

    @Override
    public ICommandResult execute(ICommandContext<? extends CommandSource> context) throws CommandException {
        KitService service = context.getServiceCollection().getServiceUnchecked(KitService.class);
        Kit kit = context.requireOne(KitParameter.KIT_PARAMETER_KEY, Kit.class);
        Player player = context.requireOne(NucleusParameters.Keys.PLAYER, Player.class);
        boolean skip = context.hasAny("i");
        if (context.is(player)) {
            return context.errorResult("command.kit.give.self");
        }

        Text playerName = context.getDisplayName(player.getUniqueId());
        Text kitName = Text.of(kit.getName());
        KitRedeemResult redeemResult = service.redeemKit(kit, player, !skip, this.mustGetAll);
        if (redeemResult.isSuccess()) {
            if (!redeemResult.rejectedItems().isEmpty()) {
                // If we drop them, tell the user
                if (this.isDrop) {
                    context.sendMessage("command.kit.give.itemsdropped", playerName);
                    redeemResult.rejectedItems().forEach(x -> Util.dropItemOnFloorAtLocation(x, player.getLocation()));
                } else {
                    context.sendMessage("command.kit.give.fullinventory", playerName);
                }
            }

            context.sendMessage("command.kit.give.spawned", playerName, kitName);
            if (kit.isDisplayMessageOnRedeem()) {
                context.sendMessage("command.kit.spawned", kit.getName());
            }

            return context.successResult();
        } else {
            switch (redeemResult.getStatus()) {
                case ALREADY_REDEEMED_ONE_TIME:
                    return context.errorResult("command.kit.give.onetime.alreadyredeemed", kitName, playerName);
                case COOLDOWN_NOT_EXPIRED:
                    return context.errorResult("command.kit.give.cooldown",
                            playerName,
                            context.getTimeString(redeemResult.getCooldownDuration().orElse(Duration.ZERO)),
                            kitName);
                case PRE_EVENT_CANCELLED:
                    return redeemResult
                            .getMessage()
                            .map(context::errorResultLiteral)
                            .orElseGet(() -> context.errorResult("command.kit.cancelledpre", kit.getName()));
                case NO_SPACE:
                    return context.errorResult("command.kit.give.fullinventorynosave", playerName);
                case UNKNOWN:
                default:
                    return context.errorResult("command.kit.give.fail", playerName, kitName);
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
