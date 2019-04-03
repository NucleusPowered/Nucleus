/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.world.commands;

import com.flowpowered.math.vector.Vector3d;
import io.github.nucleuspowered.nucleus.Nucleus;
import io.github.nucleuspowered.nucleus.internal.annotations.command.Permissions;
import io.github.nucleuspowered.nucleus.internal.annotations.command.RegisterCommand;
import io.github.nucleuspowered.nucleus.internal.command.AbstractCommand;
import io.github.nucleuspowered.nucleus.internal.command.NucleusParameters;
import io.github.nucleuspowered.nucleus.internal.command.ReturnMessageException;
import io.github.nucleuspowered.nucleus.internal.docgen.annotations.EssentialsEquivalent;
import io.github.nucleuspowered.nucleus.internal.permissions.PermissionInformation;
import io.github.nucleuspowered.nucleus.internal.permissions.SuggestedLevel;
import io.github.nucleuspowered.nucleus.modules.spawn.SpawnKeys;
import io.github.nucleuspowered.nucleus.modules.spawn.SpawnModule;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.entity.Transform;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.world.World;
import org.spongepowered.api.world.storage.WorldProperties;

import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("ALL")
@Permissions(prefix = "world", suggestedLevel = SuggestedLevel.ADMIN)
@RegisterCommand(value = {"teleport", "tp"}, subcommandOf = WorldCommand.class)
@EssentialsEquivalent(value = "world", notes = "The world command in Essentials was just a warp command.")
public class TeleportWorldCommand extends AbstractCommand<CommandSource> {

    @Override
    protected Map<String, PermissionInformation> permissionSuffixesToRegister() {
        return new HashMap<String, PermissionInformation>() {{
            put("others", PermissionInformation.getWithTranslation("permission.world.teleport.other", SuggestedLevel.ADMIN));
        }};
    }

    @Override
    public CommandElement[] getArguments() {
        return new CommandElement[] {
            NucleusParameters.WORLD_PROPERTIES_ENABLED_ONLY,
            GenericArguments.optional(
                    requirePermissionArg(NucleusParameters.ONE_PLAYER, this.permissions.getPermissionWithSuffix("others")
            ))
        };
    }

    @Override
    public CommandResult executeCommand(final CommandSource src, CommandContext args, Cause cause) throws Exception {
        Player player = getUserFromArgs(Player.class, src, NucleusParameters.Keys.PLAYER, args, "command.world.player");
        WorldProperties worldProperties = args.<WorldProperties>getOne(NucleusParameters.Keys.WORLD).get();
        if (!worldProperties.isEnabled()) {
            throw new ReturnMessageException(Nucleus.getNucleus().getMessageProvider().getTextMessageWithFormat("command.world.teleport.notenabled",
                    worldProperties.getWorldName()));
        }

        World world = Sponge.getServer().loadWorld(worldProperties.getUniqueId())
            .orElseThrow(() -> ReturnMessageException.fromKey(
                    "command.world.teleport.failed", worldProperties.getWorldName()
            ));

        Vector3d pos = worldProperties.getSpawnPosition().toDouble();
        if (!player.transferToWorld(world, pos)) {
            throw ReturnMessageException.fromKey(
                    "command.world.teleport.failed", worldProperties.getWorldName());
        }

        // Rotate.
        if (Nucleus.getNucleus().isModuleLoaded(SpawnModule.ID)) {
            Nucleus.getNucleus().getStorageManager().getWorldService()
                    .getOnThread(worldProperties.getUniqueId())
                    .flatMap(x -> x.get(SpawnKeys.WORLD_SPAWN_ROTATION))
                    .ifPresent(y -> new Transform<World>(world, pos, y));
        }
        if (src instanceof Player && ((Player) src).getUniqueId().equals(player.getUniqueId())) {
            src.sendMessage(Nucleus.getNucleus().getMessageProvider().getTextMessageWithFormat("command.world.teleport.success", worldProperties.getWorldName()));
        } else {
            src.sendMessage(Nucleus.getNucleus().getMessageProvider().getTextMessageWithFormat("command.world.teleport.successplayer",
                    Nucleus.getNucleus().getNameUtil().getSerialisedName(player), worldProperties.getWorldName()));
            player.sendMessage(Nucleus.getNucleus().getMessageProvider().getTextMessageWithFormat("command.world.teleport.success", worldProperties.getWorldName()));
        }

        return CommandResult.success();
    }
}
