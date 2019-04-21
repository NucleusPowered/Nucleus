/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.warp.commands;

import com.google.common.collect.Lists;
import io.github.nucleuspowered.nucleus.Nucleus;
import io.github.nucleuspowered.nucleus.api.nucleusdata.Warp;
import io.github.nucleuspowered.nucleus.api.teleport.TeleportResult;
import io.github.nucleuspowered.nucleus.api.teleport.TeleportResults;
import io.github.nucleuspowered.nucleus.api.teleport.TeleportScanners;
import io.github.nucleuspowered.nucleus.argumentparsers.AdditionalCompletionsArgument;
import io.github.nucleuspowered.nucleus.argumentparsers.NoModifiersArgument;
import io.github.nucleuspowered.nucleus.argumentparsers.RequiredArgumentsArgument;
import io.github.nucleuspowered.nucleus.internal.PermissionRegistry;
import io.github.nucleuspowered.nucleus.internal.annotations.command.NoCost;
import io.github.nucleuspowered.nucleus.internal.annotations.command.Permissions;
import io.github.nucleuspowered.nucleus.internal.annotations.command.RegisterCommand;
import io.github.nucleuspowered.nucleus.internal.command.AbstractCommand;
import io.github.nucleuspowered.nucleus.internal.command.ContinueMode;
import io.github.nucleuspowered.nucleus.internal.command.NucleusParameters;
import io.github.nucleuspowered.nucleus.internal.command.ReturnMessageException;
import io.github.nucleuspowered.nucleus.internal.docgen.annotations.EssentialsEquivalent;
import io.github.nucleuspowered.nucleus.internal.interfaces.Reloadable;
import io.github.nucleuspowered.nucleus.internal.permissions.PermissionInformation;
import io.github.nucleuspowered.nucleus.internal.permissions.SuggestedLevel;
import io.github.nucleuspowered.nucleus.modules.core.services.SafeTeleportService;
import io.github.nucleuspowered.nucleus.modules.warp.WarpParameters;
import io.github.nucleuspowered.nucleus.modules.warp.config.WarpConfig;
import io.github.nucleuspowered.nucleus.modules.warp.config.WarpConfigAdapter;
import io.github.nucleuspowered.nucleus.modules.warp.event.UseWarpEvent;
import io.github.nucleuspowered.nucleus.util.CauseStackHelper;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.text.action.TextActions;
import org.spongepowered.api.util.annotation.NonnullByDefault;
import org.spongepowered.api.world.teleport.TeleportHelperFilter;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Allows a user to warp to the specified warp.
 *
 * Command Usage: /warp [-f] [subject] [warp]
 *
 * <p>
 * If <code>warp.separate-permissions</code> = <code>true</code> in the commands
 * config, also requires <code>plugin.warps.[warpname]</code> permission, or
 * the NucleusPlugin admin permission.
 * </p>
 *
 * <p>
 *     NoCost is applied, as this is handled via the main config file.
 * </p>
 */
@NonnullByDefault
@Permissions(suggestedLevel = SuggestedLevel.USER, supportsOthers = true)
@RegisterCommand(value = "warp")
@NoCost
@EssentialsEquivalent(value = {"warp", "warps"}, isExact = false, notes = "Use '/warp' for warping, '/warps' to list warps.")
public class WarpCommand extends AbstractCommand<CommandSource> implements Reloadable {

    private boolean isSafeTeleport = true;
    private double defaultCost = 0;

    @Override public void onReload() {
        WarpConfig wc = getServiceUnchecked(WarpConfigAdapter.class).getNodeOrDefault();
        this.defaultCost = wc.getDefaultWarpCost();
        this.isSafeTeleport = wc.isSafeTeleport();
    }

    @Override
    protected Map<String, PermissionInformation> permissionsToRegister() {
        Map<String, PermissionInformation> m = new HashMap<>();
        m.put(PermissionRegistry.PERMISSIONS_PREFIX + "warps",
                PermissionInformation.getWithTranslation("permissions.warps", SuggestedLevel.ADMIN));
        return m;
    }

    @Override
    public CommandElement[] getArguments() {
        return new CommandElement[] {
                GenericArguments.onlyOne(GenericArguments
                        .optionalWeak(GenericArguments.flags()
                                .flag("y", "a", "-accept")
                                .flag("f", "-force").setAnchorFlags(false).buildWith(GenericArguments.none()))),
            GenericArguments.optionalWeak(RequiredArgumentsArgument.r2(requirePermissionArg(
                new NoModifiersArgument<>(
                        NucleusParameters.ONE_PLAYER,
                        NoModifiersArgument.PLAYER_NOT_CALLER_PREDICATE), this.permissions.getOthers()))),

                GenericArguments.onlyOne(
                    new AdditionalCompletionsArgument(
                            WarpParameters.WARP_PERM, 0, 1,
                            (c, s) -> this.permissions.testOthers(c) ?
                                Sponge.getServer().getOnlinePlayers().stream().map(User::getName).collect(Collectors.toList()) : Lists.newArrayList()
                ))
        };
    }

    @Override
    protected ContinueMode preProcessChecks(final CommandSource source, CommandContext args) {
        if (args.<Player>getOne(NucleusParameters.Keys.PLAYER).map(x -> !(source instanceof Player) || x.getUniqueId().equals(((Player) source)
                .getUniqueId()))
                .orElse(false)) {
            // Bypass cooldowns
            args.putArg(NoModifiersArgument.NO_COOLDOWN_ARGUMENT, true);
            return ContinueMode.CONTINUE;
        }

        if (!Nucleus.getNucleus().getEconHelper().economyServiceExists() || this.permissions.testCostExempt(source) || args.hasAny("y")) {
            return ContinueMode.CONTINUE;
        }

        Warp wd = args.<Warp>getOne(WarpParameters.WARP_KEY).get();
        Optional<Double> i = wd.getCost();
        double cost = i.orElse(this.defaultCost);

        if (cost <= 0) {
            return ContinueMode.CONTINUE;
        }

        String costWithUnit = Nucleus.getNucleus().getEconHelper().getCurrencySymbol(cost);
        if (Nucleus.getNucleus().getEconHelper().hasBalance((Player)source, cost)) {
            String command = String.format("/warp -y %s", wd.getName());
            source.sendMessage(Nucleus.getNucleus().getMessageProvider().getTextMessageWithFormat("command.warp.cost.details", wd.getName(), costWithUnit));
            source.sendMessage(
                    Nucleus.getNucleus().getMessageProvider().getTextMessageWithFormat("command.warp.cost.clickaccept").toBuilder()
                    .onClick(TextActions.runCommand(command)).onHover(TextActions.showText(
                            Nucleus.getNucleus().getMessageProvider().getTextMessageWithFormat("command.warp.cost.clickhover", command)))
                    .append(Nucleus.getNucleus().getMessageProvider().getTextMessageWithFormat("command.warp.cost.alt")).build());
        } else {
            source.sendMessage(Nucleus.getNucleus().getMessageProvider().getTextMessageWithFormat("command.warp.cost.nomoney", wd.getName(), costWithUnit));
        }

        return ContinueMode.STOP;
    }

    @Override
    public CommandResult executeCommand(CommandSource source, CommandContext args, Cause cause) throws Exception {
        Player player = this.getUserFromArgs(Player.class, source, NucleusParameters.Keys.PLAYER, args);
        boolean isOther = !(source instanceof Player) || !((Player) source).getUniqueId().equals(player.getUniqueId());

        // Permission checks are done by the parser.
        Warp wd = args.<Warp>getOne(WarpParameters.WARP_KEY).get();

        // Load the world in question
        if (!wd.getTransform().isPresent()) {
            Sponge.getServer().loadWorld(wd.getWorldProperties().get().getUniqueId())
                .orElseThrow(() -> ReturnMessageException.fromKey(
                    "command.warp.worldnotloaded"
                ));
        }

        UseWarpEvent event = CauseStackHelper.createFrameWithCausesWithReturn(c -> new UseWarpEvent(c, player, wd), source);
        if (Sponge.getEventManager().post(event)) {
            throw new ReturnMessageException(event.getCancelMessage().orElseGet(() ->
                    Nucleus.getNucleus().getMessageProvider().getTextMessageWithFormat("nucleus.eventcancelled")
            ));
        }

        Optional<Double> i = wd.getCost();
        double cost = i.orElse(this.defaultCost);

        boolean charge = false;
        if (!isOther && Nucleus.getNucleus().getEconHelper().economyServiceExists() && !this.permissions.testCostExempt(source) && cost > 0) {
            if (Nucleus.getNucleus().getEconHelper().withdrawFromPlayer(player, cost, false)) {
                charge = true; // only true for a warp by the current subject.
            } else {
                source.sendMessage(Nucleus.getNucleus().getMessageProvider().getTextMessageWithFormat("command.warp.cost.nomoney", wd.getName(),
                        Nucleus.getNucleus().getEconHelper().getCurrencySymbol(cost)));
                return CommandResult.empty();
            }
        }

        // We have a warp data, warp them.
        if (isOther) {
            source.sendMessage(Nucleus.getNucleus().getMessageProvider().getTextMessageWithFormat("command.warps.namedstart",
                    Nucleus.getNucleus().getNameUtil().getSerialisedName(player), wd.getName()));
        } else {
            source.sendMessage(Nucleus.getNucleus().getMessageProvider().getTextMessageWithFormat("command.warps.start", wd.getName()));
        }

        // Warp them.
        boolean isSafe = !args.getOne("f").isPresent() && this.isSafeTeleport;

        SafeTeleportService safeLocationService = getServiceUnchecked(SafeTeleportService.class);
        TeleportHelperFilter filter = safeLocationService.getAppropriateFilter(player, isSafe);

        TeleportResult result = safeLocationService.teleportPlayer(
                player,
                wd.getLocation().get(),
                wd.getRotation(),
                false,
                TeleportScanners.NO_SCAN,
                filter
        );

        if (!result.isSuccessful()) {
            if (charge) {
                Nucleus.getNucleus().getEconHelper().depositInPlayer(player, cost, false);
            }

            // Don't add the cooldown if enabled.
            throw ReturnMessageException.fromKey(result == TeleportResults.FAIL_NO_LOCATION ? "command.warps.nosafe" :
                    "command.warps.cancelled");
        }

        if (isOther) {
            player.sendMessage(Nucleus.getNucleus().getMessageProvider().getTextMessageWithFormat("command.warps.warped", wd.getName()));
        } else if (charge) {
            source.sendMessage(Nucleus.getNucleus().getMessageProvider().getTextMessageWithFormat("command.warp.cost.charged",
                    Nucleus.getNucleus().getEconHelper().getCurrencySymbol(cost)));
        }

        return CommandResult.success();
    }
}
