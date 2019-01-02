/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.warp.commands;

import io.github.nucleuspowered.nucleus.Nucleus;
import io.github.nucleuspowered.nucleus.internal.annotations.command.Permissions;
import io.github.nucleuspowered.nucleus.internal.annotations.command.RegisterCommand;
import io.github.nucleuspowered.nucleus.internal.command.AbstractCommand;
import io.github.nucleuspowered.nucleus.internal.command.ReturnMessageException;
import io.github.nucleuspowered.nucleus.internal.docgen.annotations.EssentialsEquivalent;
import io.github.nucleuspowered.nucleus.modules.warp.WarpParameters;
import io.github.nucleuspowered.nucleus.modules.warp.event.CreateWarpEvent;
import io.github.nucleuspowered.nucleus.modules.warp.services.WarpService;
import io.github.nucleuspowered.nucleus.util.CauseStackHelper;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.util.annotation.NonnullByDefault;

import java.util.regex.Pattern;

/**
 * Creates a warp where the player is currently standing. The warp must not
 * exist.
 */
@Permissions(prefix = "warp")
@RegisterCommand(value = {"set"}, subcommandOf = WarpCommand.class, rootAliasRegister = { "setwarp", "warpset" })
@EssentialsEquivalent({"setwarp", "createwarp"})
@NonnullByDefault
public class SetWarpCommand extends AbstractCommand<Player> {

    private final WarpService qs = getServiceUnchecked(WarpService.class);
    private final Pattern warpRegex = Pattern.compile("^[A-Za-z][A-Za-z0-9]{0,25}$");

    @Override
    public CommandElement[] getArguments() {
        return new CommandElement[] {
                GenericArguments.onlyOne(GenericArguments.string(Text.of(WarpParameters.WARP_KEY)))
        };
    }

    @Override
    public CommandResult executeCommand(Player src, CommandContext args, Cause cause) throws Exception {
        String warp = args.<String>getOne(WarpParameters.WARP_KEY).get();

        // Needs to match the name...
        if (!this.warpRegex.matcher(warp).matches()) {
            src.sendMessage(Nucleus.getNucleus().getMessageProvider().getTextMessageWithFormat("command.warps.invalidname"));
            return CommandResult.empty();
        }

        // Get the service, does the warp exist?
        if (this.qs.getWarp(warp).isPresent()) {
            // You have to delete to set the same name
            src.sendMessage(Nucleus.getNucleus().getMessageProvider().getTextMessageWithFormat("command.warps.nooverwrite"));
            return CommandResult.empty();
        }

        CreateWarpEvent event = CauseStackHelper.createFrameWithCausesWithReturn(c -> new CreateWarpEvent(c, warp, src.getLocation()), src);
        if (Sponge.getEventManager().post(event)) {
            throw new ReturnMessageException(event.getCancelMessage().orElseGet(() ->
                    Nucleus.getNucleus().getMessageProvider().getTextMessageWithFormat("nucleus.eventcancelled")
            ));
        }

        // OK! Set it.
        if (this.qs.setWarp(warp, src.getLocation(), src.getRotation())) {
            // Worked. Tell them.
            src.sendMessage(Nucleus.getNucleus().getMessageProvider().getTextMessageWithFormat("command.warps.set", warp));
            return CommandResult.success();
        }

        // Didn't work. Tell them.
        throw new ReturnMessageException(Nucleus.getNucleus().getMessageProvider().getTextMessageWithFormat("command.warps.seterror"));
    }
}
