/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.vanish.commands;

import com.google.common.collect.Maps;
import io.github.nucleuspowered.nucleus.Nucleus;
import io.github.nucleuspowered.nucleus.internal.annotations.command.NoModifiers;
import io.github.nucleuspowered.nucleus.internal.annotations.command.Permissions;
import io.github.nucleuspowered.nucleus.internal.annotations.command.RegisterCommand;
import io.github.nucleuspowered.nucleus.internal.command.AbstractCommand;
import io.github.nucleuspowered.nucleus.internal.command.ReturnMessageException;
import io.github.nucleuspowered.nucleus.internal.docgen.annotations.EssentialsEquivalent;
import io.github.nucleuspowered.nucleus.internal.permissions.PermissionInformation;
import io.github.nucleuspowered.nucleus.internal.permissions.SuggestedLevel;
import io.github.nucleuspowered.nucleus.modules.vanish.VanishKeys;
import io.github.nucleuspowered.nucleus.modules.vanish.services.VanishService;
import io.github.nucleuspowered.storage.dataobjects.keyed.IKeyedDataObject;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.entity.living.player.gamemode.GameModes;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.util.annotation.NonnullByDefault;

import java.util.Map;

@Permissions(supportsOthers = true)
@NoModifiers
@NonnullByDefault
@RegisterCommand({"vanish", "v"})
@EssentialsEquivalent({"vanish", "v"})
public class VanishCommand extends AbstractCommand<CommandSource> {

    private final String player = "player";
    private final String b = "toggle";

    @Override
    public CommandElement[] getArguments() {
        return new CommandElement[] {
                GenericArguments.optionalWeak(requirePermissionArg(GenericArguments.user(Text.of(this.player)), this.permissions.getOthers())),
                GenericArguments.optional(GenericArguments.onlyOne(GenericArguments.bool(Text.of(this.b))))
        };
    }

    @Override
    protected Map<String, PermissionInformation> permissionSuffixesToRegister() {
        Map<String, PermissionInformation> mspi = Maps.newHashMap();
        mspi.put("see", PermissionInformation.getWithTranslation("permission.vanish.see", SuggestedLevel.ADMIN));
        mspi.put("persist", PermissionInformation.getWithTranslation("permission.vanish.persist", SuggestedLevel.ADMIN));
        mspi.put("onlogin", PermissionInformation.getWithTranslation("permission.vanish.onlogin", SuggestedLevel.NONE));
        return mspi;
    }

    @Override
    public CommandResult executeCommand(CommandSource src, CommandContext args, Cause cause) throws Exception {
        User ou = getUserFromArgs(User.class, src, this.player, args);
        if (ou.getPlayer().isPresent()) {
            return onPlayer(src, args, ou.getPlayer().get());
        }

        if (!this.permissions.testSuffix(ou, "persist")) {
            throw new ReturnMessageException(Nucleus.getNucleus().getMessageProvider().getTextMessageWithFormat("command.vanish.noperm", ou.getName()));
        }

        boolean result;
        try (IKeyedDataObject.Value<Boolean> value = Nucleus.getNucleus()
                .getStorageManager()
                .getUserService()
                .getOrNewOnThread(ou.getUniqueId())
                .getAndSet(VanishKeys.VANISH_STATUS)) {
            result = args.<Boolean>getOne(this.b).orElse(!value.getValue().orElse(false));
            value.setValue(result);
        }

        sendMessageTo(
                src,
                "command.vanish.successuser",
                ou.getName(),
                result ?
                        getMessageFor(src, "command.vanish.vanished") :
                        getMessageFor(src, "command.vanish.visible")
        );

        return CommandResult.success();
    }

    private CommandResult onPlayer(CommandSource src, CommandContext args, Player playerToVanish) throws Exception {
        if (playerToVanish.get(Keys.GAME_MODE).orElse(GameModes.NOT_SET).equals(GameModes.SPECTATOR)) {
            throw ReturnMessageException.fromKey("command.vanish.fail");
        }

        // If we don't specify whether to vanish, toggle
        boolean toVanish = args.<Boolean>getOne(this.b).orElse(!playerToVanish.get(Keys.VANISH).orElse(false));
        if (toVanish) {
            getServiceUnchecked(VanishService.class).vanishPlayer(playerToVanish);
        } else {
            getServiceUnchecked(VanishService.class).unvanishPlayer(playerToVanish);
        }

        sendMessageTo(
                playerToVanish,
                "command.vanish.success",
                toVanish ?
                        getMessageFor(playerToVanish, "command.vanish.vanished") :
                        getMessageFor(playerToVanish, "command.vanish.visible")
        );

        if (!(src instanceof Player) || !(((Player) src).getUniqueId().equals(playerToVanish.getUniqueId()))) {
            sendMessageTo(
                    playerToVanish,
                    "command.vanish.successplayer",
                    Nucleus.getNucleus().getNameUtil().getName(playerToVanish),
                    toVanish ?
                            getMessageFor(playerToVanish, "command.vanish.vanished") :
                            getMessageFor(playerToVanish, "command.vanish.visible")
            );
        }

        return CommandResult.success();
    }
}
