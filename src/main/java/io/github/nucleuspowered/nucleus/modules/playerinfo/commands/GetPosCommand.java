/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.playerinfo.commands;

import com.flowpowered.math.vector.Vector3i;
import io.github.nucleuspowered.nucleus.Nucleus;
import io.github.nucleuspowered.nucleus.configurate.datatypes.LocationNode;
import io.github.nucleuspowered.nucleus.internal.annotations.command.Permissions;
import io.github.nucleuspowered.nucleus.internal.annotations.command.RegisterCommand;
import io.github.nucleuspowered.nucleus.internal.command.AbstractCommand;
import io.github.nucleuspowered.nucleus.internal.command.ReturnMessageException;
import io.github.nucleuspowered.nucleus.internal.docgen.annotations.EssentialsEquivalent;
import io.github.nucleuspowered.nucleus.internal.permissions.PermissionInformation;
import io.github.nucleuspowered.nucleus.internal.permissions.SuggestedLevel;
import io.github.nucleuspowered.nucleus.modules.core.CoreKeys;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.action.TextActions;
import org.spongepowered.api.util.annotation.NonnullByDefault;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.util.Map;

@Permissions(suggestedLevel = SuggestedLevel.USER, supportsOthers = true)
@RegisterCommand({"getpos", "coords", "position", "whereami", "getlocation", "getloc"})
@EssentialsEquivalent({"getpos", "coords", "position", "whereami", "getlocation", "getloc"})
@NonnullByDefault
public class GetPosCommand extends AbstractCommand<CommandSource> {

    private final String playerKey = "subject";

    @Override
    protected Map<String, PermissionInformation> permissionSuffixesToRegister() {
        Map<String, PermissionInformation> mspi = super.permissionSuffixesToRegister();
        mspi.put("others", new PermissionInformation(
                Nucleus.getNucleus().getMessageProvider().getMessageWithFormat("permission.getpos.others"),
                SuggestedLevel.MOD
        ));
        return mspi;
    }

    @Override public CommandElement[] getArguments() {
        return new CommandElement[] {
            GenericArguments.optionalWeak(requirePermissionArg(GenericArguments.user(Text.of(this.playerKey)), this.permissions.getOthers()))
        };
    }

    @Override protected CommandResult executeCommand(CommandSource src, CommandContext args, Cause cause) throws Exception {
        User user = this.getUserFromArgs(User.class, src, this.playerKey, args);
        Location<World> location;
        if (user.isOnline()) {
            location = user.getPlayer().get().getLocation();
        } else {
            location = getUser(user.getUniqueId())
                    .join()
                    .flatMap(x -> x.get(CoreKeys.LAST_LOCATION).flatMap(LocationNode::getLocationIfExists))
                    .orElseThrow(() -> ReturnMessageException.fromKey("command.getpos.location.nolocation", user.getName()));
        }

        boolean isSelf = src instanceof Player && ((Player) src).getUniqueId().equals(user.getUniqueId());
        Vector3i blockPos = location.getBlockPosition();
        if (isSelf) {
            src.sendMessage(
                    Nucleus.getNucleus().getMessageProvider()
                    .getTextMessageWithFormat(
                            "command.getpos.location.self",
                            location.getExtent().getName(),
                            String.valueOf(blockPos.getX()),
                            String.valueOf(blockPos.getY()),
                            String.valueOf(blockPos.getZ())
                    )
            );
        } else {
            src.sendMessage(
                    Nucleus.getNucleus().getMessageProvider()
                    .getTextMessageWithFormat(
                            "command.getpos.location.other",
                            Nucleus.getNucleus().getNameUtil().getSerialisedName(user),
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
                        .onHover(TextActions.showText(Nucleus.getNucleus().getMessageProvider().getTextMessageWithFormat("command.getpos.hover")))
                        .build()
            );
        }

        return CommandResult.success();
    }
}
