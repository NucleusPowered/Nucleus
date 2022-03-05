/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.warp.commands;

import io.github.nucleuspowered.nucleus.api.module.warp.data.Warp;
import io.github.nucleuspowered.nucleus.modules.warp.WarpPermissions;
import io.github.nucleuspowered.nucleus.modules.warp.event.CreateWarpEvent;
import io.github.nucleuspowered.nucleus.modules.warp.event.DeleteWarpEvent;
import io.github.nucleuspowered.nucleus.modules.warp.services.WarpService;
import io.github.nucleuspowered.nucleus.core.scaffold.command.ICommandContext;
import io.github.nucleuspowered.nucleus.core.scaffold.command.ICommandExecutor;
import io.github.nucleuspowered.nucleus.core.scaffold.command.ICommandResult;
import io.github.nucleuspowered.nucleus.core.scaffold.command.annotation.Command;
import io.github.nucleuspowered.nucleus.core.scaffold.command.annotation.CommandModifier;
import io.github.nucleuspowered.nucleus.core.scaffold.command.annotation.EssentialsEquivalent;
import io.github.nucleuspowered.nucleus.core.scaffold.command.modifier.CommandModifiers;
import io.github.nucleuspowered.nucleus.core.services.INucleusServiceCollection;
import io.github.nucleuspowered.nucleus.core.services.interfaces.IPermissionService;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.exception.CommandException;
import org.spongepowered.api.command.parameter.Parameter;
import org.spongepowered.api.command.parameter.managed.Flag;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;

import java.util.Optional;
import java.util.regex.Pattern;

@EssentialsEquivalent({"setwarp", "createwarp"})
@Command(
        aliases = {"set", "#setwarp", "#warpset"},
        parentCommand = WarpCommand.class,
        basePermission = WarpPermissions.BASE_WARP_SET,
        commandDescriptionKey = "warp.set",
        modifiers = {
                @CommandModifier(
                        value = CommandModifiers.HAS_WARMUP,
                        exemptPermission = WarpPermissions.EXEMPT_WARMUP_WARP_SET
                ),
                @CommandModifier(
                        value = CommandModifiers.HAS_COOLDOWN,
                        exemptPermission = WarpPermissions.EXEMPT_COOLDOWN_WARP_SET
                ),
                @CommandModifier(
                        value = CommandModifiers.HAS_COST,
                        exemptPermission = WarpPermissions.EXEMPT_COST_WARP_SET
                )
        }
)
public class SetWarpCommand implements ICommandExecutor {

//    private final WarpService qs = getServiceUnchecked(WarpService.class);
    private final Pattern warpRegex = Pattern.compile("^[A-Za-z][A-Za-z0-9]{0,25}$");

    private final Parameter.Value<String> warpParameter = Parameter.string().key("warp").build();

    @Override
    public Flag[] flags(final INucleusServiceCollection serviceCollection) {
        final IPermissionService permissionService = serviceCollection.permissionService();
        return new Flag[] {
                Flag.builder().setRequirement(cause -> permissionService.hasPermission(cause, WarpPermissions.BASE_WARP_DELETE))
                    .aliases("o", "overwrite")
                    .build()
        };
    }

    @Override
    public Parameter[] parameters(final INucleusServiceCollection serviceCollection) {
        return new Parameter[] {
                this.warpParameter
        };
    }

    @Override
    public ICommandResult execute(final ICommandContext context) throws CommandException {
        final String warp = context.requireOne(this.warpParameter);

        // Needs to match the name...
        if (!this.warpRegex.matcher(warp).matches()) {
            return context.errorResult("command.warps.invalidname");
        }

        final WarpService warpService = context.getServiceCollection().getServiceUnchecked(WarpService.class);

        // Get the service, does the warp exist?
        final Optional<Warp> exists = warpService.getWarp(warp);
        if (exists.isPresent()) {
            if (!context.hasFlag("o")) {
                // You have to delete to set the same name
                return context.errorResult("command.warps.nooverwrite");
            }

            final DeleteWarpEvent event = new DeleteWarpEvent(Sponge.server().causeStackManager().currentCause(), exists.get());
            if (Sponge.eventManager().post(event)) {
                return event.getCancelMessage().map(context::errorResultLiteral)
                        .orElseGet(() -> context.errorResult("nucleus.eventcancelled"));
            }

            final String toRemove = exists.get().getNamedLocation().getName();
            if (warpService.removeWarp(toRemove)) {
                // Worked. Tell them.
                context.sendMessage("command.warps.del", toRemove);
            } else {
                // Didn't work. Tell them.
                return context.errorResult("command.warps.delerror");
            }
        }

        final ServerPlayer src = context.requirePlayer();
        final CreateWarpEvent event = new CreateWarpEvent(Sponge.server().causeStackManager().currentCause(), warp, src.serverLocation());
        if (Sponge.eventManager().post(event)) {
            return event.getCancelMessage()
                    .map(context::errorResultLiteral)
                    .orElseGet(() -> context.errorResult("nucleus.eventcancelled")
            );
        }

        // OK! Set it.
        if (warpService.setWarp(warp, src.serverLocation(), src.rotation())) {
            // Worked. Tell them.
            context.sendMessage("command.warps.set", warp);
            return context.successResult();
        }

        // Didn't work. Tell them.
        return context.errorResult("command.warps.seterror");
    }
}
