/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.teleport.commands;

import io.github.nucleuspowered.nucleus.Nucleus;
import io.github.nucleuspowered.nucleus.internal.annotations.command.NoWarmup;
import io.github.nucleuspowered.nucleus.internal.annotations.command.NotifyIfAFK;
import io.github.nucleuspowered.nucleus.internal.annotations.command.Permissions;
import io.github.nucleuspowered.nucleus.internal.annotations.command.RegisterCommand;
import io.github.nucleuspowered.nucleus.internal.annotations.command.SetCooldownManually;
import io.github.nucleuspowered.nucleus.internal.command.AbstractCommand;
import io.github.nucleuspowered.nucleus.internal.command.ContinueMode;
import io.github.nucleuspowered.nucleus.internal.command.NucleusParameters;
import io.github.nucleuspowered.nucleus.internal.command.ReturnMessageException;
import io.github.nucleuspowered.nucleus.internal.docgen.annotations.EssentialsEquivalent;
import io.github.nucleuspowered.nucleus.internal.interfaces.Reloadable;
import io.github.nucleuspowered.nucleus.internal.permissions.PermissionInformation;
import io.github.nucleuspowered.nucleus.internal.permissions.SuggestedLevel;
import io.github.nucleuspowered.nucleus.modules.teleport.config.TeleportConfigAdapter;
import io.github.nucleuspowered.nucleus.modules.teleport.events.RequestEvent;
import io.github.nucleuspowered.nucleus.modules.teleport.services.PlayerTeleporterService;
import io.github.nucleuspowered.nucleus.util.CauseStackHelper;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.util.annotation.NonnullByDefault;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Sends a request to a subject to teleport to them, using click handlers.
 */
@Permissions(prefix = "teleport", suggestedLevel = SuggestedLevel.USER, supportsSelectors = true)
@NoWarmup(generateConfigEntry = true, generatePermissionDocs = true)
@RegisterCommand({"tpa", "teleportask", "call", "tpask"})
@NonnullByDefault
@EssentialsEquivalent({"tpa", "call", "tpask"})
@NotifyIfAFK(NucleusParameters.Keys.PLAYER)
@SetCooldownManually
public class TeleportAskCommand extends AbstractCommand<Player> implements Reloadable {

    private boolean isCooldownOnAsk = false;
    private final PlayerTeleporterService playerTeleporterService = getServiceUnchecked(PlayerTeleporterService.class);

    @Override
    public Map<String, PermissionInformation> permissionSuffixesToRegister() {
        Map<String, PermissionInformation> m = new HashMap<>();
        m.put("force", PermissionInformation.getWithTranslation("permission.teleport.force", SuggestedLevel.ADMIN));
        return m;
    }

    @Override
    public CommandElement[] getArguments() {
        return new CommandElement[] {
                GenericArguments.flags().permissionFlag(this.permissions.getPermissionWithSuffix("force"), "f").buildWith(NucleusParameters.ONE_PLAYER)
        };
    }

    @Override protected ContinueMode preProcessChecks(Player source, CommandContext args) {
        return this.playerTeleporterService
                .canTeleportTo(source, args.requireOne(NucleusParameters.Keys.PLAYER)) ? ContinueMode.CONTINUE : ContinueMode.STOP;
    }

    @Override
    public CommandResult executeCommand(Player src, CommandContext args, Cause cause) throws Exception {
        Player target = args.requireOne(NucleusParameters.Keys.PLAYER);
        if (src.equals(target)) {
            throw ReturnMessageException.fromKey(src, "command.teleport.self");
        }

        RequestEvent.CauseToPlayer event = new RequestEvent.CauseToPlayer(CauseStackHelper.createCause(src), target);
        if (Sponge.getEventManager().post(event)) {
            throw new ReturnMessageException(
                    event.getCancelMessage().orElseGet(() -> Nucleus.getNucleus().getMessageProvider().getTextMessageWithFormat("command.tpa.eventfailed")));
        }

        Consumer<Player> cooldownSetter = a -> {};
        if (this.isCooldownOnAsk) {
            setCooldown(src);
        } else {
            cooldownSetter = this::setCooldown;
        }

        this.playerTeleporterService.requestTeleport(
                src,
                target,
                getCost(src, args),
                getWarmup(src),
                src,
                target,
                !args.<Boolean>getOne("f").orElse(false),
                false,
                false,
                cooldownSetter,
                "command.tpa.question"
        );

        return CommandResult.success();
    }

    @Override
    public void onReload() throws Exception {
        this.isCooldownOnAsk = getServiceUnchecked(TeleportConfigAdapter.class).getNodeOrDefault().isCooldownOnAsk();
    }
}
