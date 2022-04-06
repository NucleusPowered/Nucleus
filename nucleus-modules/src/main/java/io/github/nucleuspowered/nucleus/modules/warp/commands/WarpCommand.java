/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.warp.commands;

import io.github.nucleuspowered.nucleus.api.module.warp.data.Warp;
import io.github.nucleuspowered.nucleus.api.teleport.data.TeleportResult;
import io.github.nucleuspowered.nucleus.api.teleport.data.TeleportScanners;
import io.github.nucleuspowered.nucleus.core.scaffold.command.ICommandContext;
import io.github.nucleuspowered.nucleus.core.scaffold.command.ICommandExecutor;
import io.github.nucleuspowered.nucleus.core.scaffold.command.ICommandResult;
import io.github.nucleuspowered.nucleus.core.scaffold.command.annotation.Command;
import io.github.nucleuspowered.nucleus.core.scaffold.command.annotation.CommandModifier;
import io.github.nucleuspowered.nucleus.core.scaffold.command.annotation.EssentialsEquivalent;
import io.github.nucleuspowered.nucleus.core.scaffold.command.modifier.CommandModifiers;
import io.github.nucleuspowered.nucleus.core.services.INucleusServiceCollection;
import io.github.nucleuspowered.nucleus.core.services.interfaces.IEconomyServiceProvider;
import io.github.nucleuspowered.nucleus.core.services.interfaces.INucleusLocationService;
import io.github.nucleuspowered.nucleus.core.services.interfaces.IReloadableService;
import io.github.nucleuspowered.nucleus.modules.warp.WarpPermissions;
import io.github.nucleuspowered.nucleus.modules.warp.config.WarpConfig;
import io.github.nucleuspowered.nucleus.modules.warp.event.UseWarpEvent;
import io.github.nucleuspowered.nucleus.modules.warp.services.WarpService;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.exception.CommandException;
import org.spongepowered.api.command.parameter.Parameter;
import org.spongepowered.api.command.parameter.managed.Flag;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.event.CauseStackManager;
import org.spongepowered.api.world.teleport.TeleportHelperFilter;

import java.util.Optional;

@EssentialsEquivalent(value = {"warp", "warps"}, isExact = false, notes = "Use '/warp' for warping, '/warps' to list warps.")
@Command(
        aliases = {"warp"},
        basePermission = WarpPermissions.BASE_WARP,
        commandDescriptionKey = "warp",
        modifiers = {
                @CommandModifier(
                        value = CommandModifiers.HAS_WARMUP,
                        exemptPermission = WarpPermissions.EXEMPT_WARMUP_WARP
                ),
                @CommandModifier(
                        value = CommandModifiers.HAS_COOLDOWN,
                        exemptPermission = WarpPermissions.EXEMPT_COOLDOWN_WARP
                )
        },
        associatedPermissions = {
                WarpPermissions.PERMISSIONS_WARPS,
                WarpPermissions.OTHERS_WARP
        }
)
public class WarpCommand implements ICommandExecutor, IReloadableService.Reloadable {

    private boolean isSafeTeleport = true;
    private double defaultCost = 0;

    @Override public void onReload(final INucleusServiceCollection serviceCollection) {
        final WarpConfig wc = serviceCollection.configProvider().getModuleConfig(WarpConfig.class);
        this.defaultCost = wc.getDefaultWarpCost();
        this.isSafeTeleport = wc.isSafeTeleport();
    }

    @Override
    public Flag[] flags(final INucleusServiceCollection serviceCollection) {
        return new Flag[] {
                Flag.of("y", "a", "accept"),
                Flag.of("f", "force")
        };
    }

    // flag,
    @Override
    public Parameter[] parameters(final INucleusServiceCollection serviceCollection) {
        return new Parameter[] {
                serviceCollection.commandElementSupplier().createOnlyOtherPlayerPermissionElement(WarpPermissions.OTHERS_WARP),
                serviceCollection.getServiceUnchecked(WarpService.class).warpElement(true)
        };
    }

    @Override public Optional<ICommandResult> preExecute(final ICommandContext context) throws CommandException {
        final ServerPlayer target = context.getPlayerFromArgs();
        final IEconomyServiceProvider economyServiceProvider = context.getServiceCollection().economyServiceProvider();
        if (!context.is(target)) {
            // Don't cooldown
            context.removeModifier(CommandModifiers.HAS_COOLDOWN);
            return Optional.empty();
        }

        if (!economyServiceProvider.serviceExists() ||
                context.testPermission(WarpPermissions.EXEMPT_COST_WARP) ||
                context.hasFlag("y")) {
            return Optional.empty();
        }

        final WarpService service = context.getServiceCollection().getServiceUnchecked(WarpService.class);
        final Warp wd = context.requireOne(service.warpElement(true));
        final Optional<Double> i = wd.getCost();
        final double cost = i.orElse(this.defaultCost);

        if (cost <= 0) {
            return Optional.empty();
        }

        final String costWithUnit = economyServiceProvider.getCurrencySymbol(cost);
        if (economyServiceProvider.hasBalance(target.uniqueId(), cost)) {
            final String command = String.format("/nucleus:warp -y %s", wd.getNamedLocation().getName());
            context.sendMessage("command.warp.cost.details", wd.getNamedLocation().getName(), costWithUnit);
            context.sendMessageText(
                    context.getMessage("command.warp.cost.clickaccept")
                            .clickEvent(ClickEvent.runCommand(command)).hoverEvent(
                                    HoverEvent.showText(context.getMessage("command.warp.cost.clickhover", command)))
                            .append(context.getMessage("command.warp.cost.alt")));
        } else {
            context.sendMessage("command.warp.cost.nomoney", wd.getNamedLocation().getName(), costWithUnit);
        }

        return Optional.of(context.failResult());
    }

    @Override public ICommandResult execute(final ICommandContext context) throws CommandException {
        final ServerPlayer player = context.getPlayerFromArgs();
        final boolean isOther = !context.is(player);

        // Permission checks are done by the parser.
        final WarpService service = context.getServiceCollection().getServiceUnchecked(WarpService.class);
        final Warp wd = context.requireOne(service.warpElement(true));

        // Load the world in question
        if (!wd.getNamedLocation().getLocation().isPresent()) {
            return context.errorResult("command.warp.worldnotloaded");
        }

        try (final CauseStackManager.StackFrame frame = Sponge.server().causeStackManager().pushCauseFrame()) {
            frame.pushCause(context.getCommandSourceRoot());
            final UseWarpEvent event = new UseWarpEvent(frame.currentCause(), player.uniqueId(), wd);
            if (Sponge.eventManager().post(event)) {
                return event.getCancelMessage().map(context::errorResultLiteral)
                        .orElseGet(() -> context.errorResult("nucleus.eventcancelled"));
            }

            final Optional<Double> i = wd.getCost();
            final double cost = i.orElse(this.defaultCost);

            boolean charge = false;
            final IEconomyServiceProvider economyServiceProvider = context.getServiceCollection().economyServiceProvider();
            if (!isOther && economyServiceProvider.serviceExists() && cost > 0 &&
                    !context.testPermission(WarpPermissions.EXEMPT_COST_WARP)) {
                if (economyServiceProvider.withdrawFromPlayer(player.uniqueId(), cost, false)) {
                    charge = true; // only true for a warp by the current subject.
                } else {
                    return context.errorResult("command.warp.cost.nomoney", wd.getNamedLocation().getName(),
                            economyServiceProvider.getCurrencySymbol(cost));
                }
            }

            // We have a warp data, warp them.
            if (isOther) {
                context.sendMessage("command.warps.namedstart",
                        context.getDisplayName(player.uniqueId()),
                        wd.getNamedLocation().getName());
            } else {
                context.sendMessage("command.warps.start", wd.getNamedLocation().getName());
            }

            // Warp them.
            final boolean isSafe = !context.hasFlag("f") && this.isSafeTeleport;

            final INucleusLocationService safeLocationService = context.getServiceCollection().teleportService();
            final TeleportHelperFilter filter = safeLocationService.getAppropriateFilter(player, isSafe);

            final TeleportResult result = safeLocationService.teleportPlayer(
                    player,
                    wd.getNamedLocation().getLocation().get(),
                    wd.getNamedLocation().getRotation(),
                    false,
                    TeleportScanners.NO_SCAN.get(),
                    filter
            );

            if (!result.isSuccessful()) {
                if (charge) {
                    economyServiceProvider.depositInPlayer(player.uniqueId(), cost, false);
                }

                // Don't add the cooldown if enabled.
                return context.errorResult(result == TeleportResult.FAIL_NO_LOCATION ? "command.warps.nosafe" :
                        "command.warps.cancelled");
            }

            if (isOther) {
                context.sendMessageTo(player, "command.warps.warped", wd.getNamedLocation().getName());
            } else if (charge) {
                context.sendMessage("command.warp.cost.charged", economyServiceProvider.getCurrencySymbol(cost));
            }

            return context.successResult();
        }
    }
}
