/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.fun.commands;

import io.github.nucleuspowered.nucleus.argumentparsers.NicknameArgument;
import io.github.nucleuspowered.nucleus.argumentparsers.SelectorWrapperArgument;
import io.github.nucleuspowered.nucleus.internal.annotations.Permissions;
import io.github.nucleuspowered.nucleus.internal.annotations.RegisterCommand;
import io.github.nucleuspowered.nucleus.internal.command.ReturnMessageException;
import io.github.nucleuspowered.nucleus.internal.permissions.PermissionInformation;
import io.github.nucleuspowered.nucleus.internal.permissions.SuggestedLevel;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.EntityTypes;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.cause.NamedCause;
import org.spongepowered.api.event.cause.entity.spawn.SpawnCause;
import org.spongepowered.api.event.cause.entity.spawn.SpawnTypes;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.util.blockray.BlockRay;
import org.spongepowered.api.util.blockray.BlockRayHit;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Permissions(supportsSelectors = true)
@RegisterCommand({"lightning", "smite", "thor"})
public class LightningCommand extends io.github.nucleuspowered.nucleus.internal.command.AbstractCommand<CommandSource> {

    private final String player = "player";

    @Override
    public Map<String, PermissionInformation> permissionSuffixesToRegister() {
        Map<String, PermissionInformation> m = new HashMap<>();
        m.put("others", new PermissionInformation(plugin.getMessageProvider().getMessageWithFormat("permission.others", this.getAliases()[0]), SuggestedLevel.ADMIN));
        return m;
    }

    @Override
    public CommandElement[] getArguments() {
        return new CommandElement[]{
                GenericArguments.optional(
                    GenericArguments.requiringPermission(
                            new SelectorWrapperArgument(
                                new NicknameArgument(Text.of(player), plugin.getUserDataManager(), NicknameArgument.UnderlyingType.PLAYER, false),
                                permissions,
                                SelectorWrapperArgument.ALL_SELECTORS)
                        , permissions.getPermissionWithSuffix("others")))
        };
    }

    @Override
    public CommandResult executeCommand(final CommandSource src, CommandContext args) throws Exception {
        Collection<Player> playerCollection = args.getAll(player);

        // No argument, let's not smite the player.
        if (playerCollection.isEmpty()) {
            if (!(src instanceof Player)) {
                src.sendMessage(plugin.getMessageProvider().getTextMessageWithFormat("command.playeronly"));
                return CommandResult.empty();
            }

            Player pl = (Player)src;

            // 100 is a good limit here.
            BlockRay<World> playerBlockRay = BlockRay.from(pl).distanceLimit(100).stopFilter(BlockRay.continueAfterFilter(BlockRay.onlyAirFilter(), 1)).build();
            Optional<BlockRayHit<World>> obh = playerBlockRay.end();
            Location<World> lightningLocation;
            if (obh.isPresent()) {
                lightningLocation = obh.get().getLocation();
            } else {
                // Smite above, but not on.
                lightningLocation = pl.getLocation().add(0, 3, 0);
            }

            return this.spawnLightning(lightningLocation, src, null, "command.lightning.error");
        }

        int successCount = 0;
        for (Player pl : playerCollection) {
            CommandResult cr = this.spawnLightning(pl.getLocation(), src, "command.lightning.success.other", "command.lightning.errorplayer", plugin.getNameUtil().getSerialisedName(pl));
            successCount += cr.getSuccessCount().orElse(0);
        }

        return CommandResult.builder().successCount(successCount).build();
    }

    private CommandResult spawnLightning(Location<World> location, CommandSource src, String successKey, String errorKey, String... replacements) throws ReturnMessageException {
        World world = location.getExtent();
        Entity bolt = world.createEntity(EntityTypes.LIGHTNING, location.getPosition());

        Cause cause = Cause.of(
                NamedCause.owner(SpawnCause.builder().type(SpawnTypes.PLUGIN).build()),
                NamedCause.source(src));

        if (world.spawnEntity(bolt, cause)) {
            if (successKey != null) {
                src.sendMessage(plugin.getMessageProvider().getTextMessageWithFormat(successKey, replacements));
            }

            return CommandResult.success();
        }

        src.sendMessage(plugin.getMessageProvider().getTextMessageWithFormat(errorKey, replacements));
        return CommandResult.empty();
    }
}
