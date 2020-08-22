/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.playerinfo.commands;

import com.flowpowered.math.vector.Vector3i;
import io.github.nucleuspowered.nucleus.modules.playerinfo.PlayerInfoPermissions;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandContext;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandExecutor;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandResult;
import io.github.nucleuspowered.nucleus.scaffold.command.annotation.Command;
import io.github.nucleuspowered.nucleus.scaffold.command.annotation.CommandModifier;
import io.github.nucleuspowered.nucleus.scaffold.command.modifier.CommandModifiers;
import io.github.nucleuspowered.nucleus.services.INucleusServiceCollection;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.text.action.TextActions;
import org.spongepowered.api.util.annotation.NonnullByDefault;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

@NonnullByDefault
@Command(
        aliases = {"getpos", "coords", "position", "whereami", "getlocation", "getloc"},
        basePermission = PlayerInfoPermissions.BASE_GETPOS,
        commandDescriptionKey = "getpos",
        modifiers = {
                @CommandModifier(value = CommandModifiers.HAS_COOLDOWN, exemptPermission = PlayerInfoPermissions.EXEMPT_COOLDOWN_GETPOS),
                @CommandModifier(value = CommandModifiers.HAS_WARMUP, exemptPermission = PlayerInfoPermissions.EXEMPT_WARMUP_GETPOS),
                @CommandModifier(value = CommandModifiers.HAS_COST, exemptPermission = PlayerInfoPermissions.EXEMPT_COST_GETPOS)
        },
        associatedPermissions = PlayerInfoPermissions.GETPOS_OTHERS
)
public class GetPosCommand implements ICommandExecutor<CommandSource> {

    @Override public CommandElement[] parameters(INucleusServiceCollection serviceCollection) {
        return new CommandElement[] {
                serviceCollection.commandElementSupplier().createOnlyOtherUserPermissionElement(false, PlayerInfoPermissions.GETPOS_OTHERS)
        };
    }

    @Override public ICommandResult execute(ICommandContext<? extends CommandSource> context) throws CommandException {
        User user = context.getUserFromArgs();
        Location<World> location;
        if (user.isOnline()) {
            location = user.getPlayer().get().getLocation();
        } else {
            World w =
                    user.getWorldUniqueId().flatMap(x -> Sponge.getServer().getWorld(x))
                            .orElseThrow(() -> context.createException("command.getpos.location.nolocation", user.getName()));
            location = new Location<>(
                    w,
                    user.getPosition()
            );
        }

        boolean isSelf = context.is(user);
        Vector3i blockPos = location.getBlockPosition();
        if (isSelf) {
            context.sendMessage(
                            "command.getpos.location.self",
                            location.getExtent().getName(),
                            String.valueOf(blockPos.getX()),
                            String.valueOf(blockPos.getY()),
                            String.valueOf(blockPos.getZ())
            );
        } else {
            context.getMessage(
                            "command.getpos.location.other",
                            context.getDisplayName(user.getUniqueId()),
                            location.getExtent().getName(),
                            String.valueOf(blockPos.getX()),
                            String.valueOf(blockPos.getY()),
                            String.valueOf(blockPos.getZ())
                    ).toBuilder().onClick(TextActions.runCommand(String.join(" ",
                        "/nucleus:tppos",
                        location.getExtent().getName(),
                        String.valueOf(blockPos.getX()),
                        String.valueOf(blockPos.getY()),
                        String.valueOf(blockPos.getZ()))))
                        .onHover(TextActions.showText(
                                context.getMessage("command.getpos.hover")))
                        .build();
        }

        return context.successResult();
    }
}
