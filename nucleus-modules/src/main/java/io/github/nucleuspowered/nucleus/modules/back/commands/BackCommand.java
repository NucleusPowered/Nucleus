/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.back.commands;

import io.github.nucleuspowered.nucleus.api.teleport.data.TeleportResult;
import io.github.nucleuspowered.nucleus.api.teleport.data.TeleportScanners;
import io.github.nucleuspowered.nucleus.modules.back.BackPermissions;
import io.github.nucleuspowered.nucleus.modules.back.config.BackConfig;
import io.github.nucleuspowered.nucleus.modules.back.services.BackHandler;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandContext;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandExecutor;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandResult;
import io.github.nucleuspowered.nucleus.scaffold.command.annotation.Command;
import io.github.nucleuspowered.nucleus.scaffold.command.annotation.CommandModifier;
import io.github.nucleuspowered.nucleus.scaffold.command.annotation.EssentialsEquivalent;
import io.github.nucleuspowered.nucleus.scaffold.command.modifier.CommandModifiers;
import io.github.nucleuspowered.nucleus.services.INucleusServiceCollection;
import io.github.nucleuspowered.nucleus.services.interfaces.INucleusTeleportService;
import io.github.nucleuspowered.nucleus.services.interfaces.IReloadableService;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.entity.Transform;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.util.annotation.NonnullByDefault;
import org.spongepowered.api.world.World;

import java.util.Optional;

@EssentialsEquivalent({"back", "return"})
@Command(
        aliases = {"back", "return"},
        basePermission = BackPermissions.BASE_BACK,
        commandDescriptionKey = "back",
        modifiers = {
            @CommandModifier(value = CommandModifiers.HAS_WARMUP, exemptPermission = BackPermissions.EXEMPT_WARMUP_BACK),
            @CommandModifier(value = CommandModifiers.HAS_COOLDOWN, exemptPermission = BackPermissions.EXEMPT_COOLDOWN_BACK),
            @CommandModifier(value = CommandModifiers.HAS_COST, exemptPermission = BackPermissions.EXEMPT_COST_BACK)
        },
        associatedPermissions = { BackPermissions.TPPOS_BORDER, BackPermissions.BACK_EXEMPT_SAMEDIMENSION })
@NonnullByDefault
public class BackCommand implements ICommandExecutor<Player>, IReloadableService.Reloadable {

    private boolean sameDimensionCheck = false;

    @Override
    public CommandElement[] parameters(INucleusServiceCollection serviceCollection) {
        return new CommandElement[] {
                GenericArguments.flags()
                    .permissionFlag(BackPermissions.TPPOS_BORDER,"b", "-border")
                    .flag("f", "-force")
                    .buildWith(GenericArguments.none())
        };
    }

    @Override
    public ICommandResult execute(ICommandContext<? extends Player> context) throws CommandException {
        BackHandler handler = context.getServiceCollection().getServiceUnchecked(BackHandler.class);
        Player src = context.getIfPlayer();
        Optional<Transform<World>> ol = handler.getLastLocation(src);
        if (!ol.isPresent()) {
            return context.errorResult("command.back.noloc");
        }

        boolean border = context.hasAny("b");
        Transform<World> loc = ol.get();
        if (this.sameDimensionCheck && src.getWorld().getUniqueId() != loc.getExtent().getUniqueId()) {
            if (!context.testPermission(BackPermissions.BACK_EXEMPT_SAMEDIMENSION)) {
                return context.errorResult("command.back.sameworld");
            }
        }

        INucleusTeleportService service = context.getServiceCollection().teleportService();
        try (INucleusTeleportService.BorderDisableSession ac = service.temporarilyDisableBorder(border, loc.getExtent())) {
            TeleportResult result = service.teleportPlayerSmart(
                            src,
                            loc,
                            false,
                            !context.hasAny("f"),
                            TeleportScanners.NO_SCAN.get()
                    );
            if (result.isSuccessful()) {
                context.sendMessage("command.back.success");
                return context.successResult();
            } else if (result == TeleportResult.FAIL_NO_LOCATION) {
                return context.errorResult("command.back.nosafe");
            }

            return context.errorResult("command.back.cancelled");
        }
    }

    @Override
    public void onReload(INucleusServiceCollection serviceCollection) {
        this.sameDimensionCheck = serviceCollection.moduleDataProvider().getModuleConfig(BackConfig.class).isOnlySameDimension();
    }
}
