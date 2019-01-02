/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.spawn.commands;

import io.github.nucleuspowered.nucleus.Nucleus;
import io.github.nucleuspowered.nucleus.configurate.datatypes.LocationNode;
import io.github.nucleuspowered.nucleus.internal.annotations.command.NoModifiers;
import io.github.nucleuspowered.nucleus.internal.annotations.command.Permissions;
import io.github.nucleuspowered.nucleus.internal.annotations.command.RegisterCommand;
import io.github.nucleuspowered.nucleus.internal.command.AbstractCommand;
import io.github.nucleuspowered.nucleus.internal.command.ReturnMessageException;
import io.github.nucleuspowered.nucleus.internal.interfaces.Reloadable;
import io.github.nucleuspowered.nucleus.internal.permissions.PermissionInformation;
import io.github.nucleuspowered.nucleus.internal.permissions.SuggestedLevel;
import io.github.nucleuspowered.nucleus.internal.teleport.NucleusTeleportHandler;
import io.github.nucleuspowered.nucleus.modules.core.CoreKeys;
import io.github.nucleuspowered.nucleus.modules.spawn.config.GlobalSpawnConfig;
import io.github.nucleuspowered.nucleus.modules.spawn.config.SpawnConfig;
import io.github.nucleuspowered.nucleus.modules.spawn.config.SpawnConfigAdapter;
import io.github.nucleuspowered.nucleus.modules.spawn.events.SendToSpawnEvent;
import io.github.nucleuspowered.nucleus.modules.spawn.helpers.SpawnHelper;
import io.github.nucleuspowered.nucleus.util.CauseStackHelper;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.entity.Transform;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.util.annotation.NonnullByDefault;
import org.spongepowered.api.world.World;
import org.spongepowered.api.world.storage.WorldProperties;

import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("ALL")
@NoModifiers
@NonnullByDefault
@Permissions(prefix = "spawn", suggestedLevel = SuggestedLevel.ADMIN)
@RegisterCommand(value = "other", subcommandOf = SpawnCommand.class)
public class SpawnOtherCommand extends AbstractCommand<CommandSource> implements Reloadable {

    private final String otherKey = "subject";
    private final String worldKey = "world";
    private GlobalSpawnConfig gsc = new GlobalSpawnConfig();
    private boolean safeTeleport = true;

    @Override public CommandElement[] getArguments() {
        return new CommandElement[] {
            GenericArguments.user(Text.of(otherKey)),
            GenericArguments.optional(GenericArguments.world(Text.of(worldKey)))
        };
    }

    @Override public void onReload() throws Exception {
        SpawnConfig sc = getServiceUnchecked(SpawnConfigAdapter.class).getNodeOrDefault();
        this.gsc = sc.getGlobalSpawn();
        this.safeTeleport = sc.isSafeTeleport();
    }

    @Override protected Map<String, PermissionInformation> permissionSuffixesToRegister() {
        return new HashMap<String, PermissionInformation>() {{
            put("offline", PermissionInformation.getWithTranslation("permission.spawnother.offline", SuggestedLevel.ADMIN));
        }};
    }

    @Override public CommandResult executeCommand(CommandSource src, CommandContext args, Cause cause) throws Exception {
        User target = args.<User>getOne(otherKey).get();
        WorldProperties world = this.getWorldProperties(src, worldKey, args)
            .orElseGet(() -> gsc.isOnSpawnCommand() ? gsc.getWorld().get() : Sponge.getServer().getDefaultWorld().get());

        Transform<World> worldTransform = SpawnHelper.getSpawn(world, target.getPlayer().orElse(null));

        SendToSpawnEvent event = new SendToSpawnEvent(worldTransform, target, CauseStackHelper.createCause(src));
        if (Sponge.getEventManager().post(event)) {
            if (event.getCancelReason().isPresent()) {
                throw new ReturnMessageException(Nucleus
                        .getNucleus().getMessageProvider().getTextMessageWithFormat("command.spawnother.other.failed.reason", target.getName(), event.getCancelReason().get()));
            }

            throw new ReturnMessageException(Nucleus.getNucleus().getMessageProvider().getTextMessageWithFormat("command.spawnother.other.failed.noreason", target.getName()));
        }

        if (!target.isOnline()) {
            return isOffline(src, target, worldTransform);
        }

        // If we don't have a rotation, then use the current rotation
        Player player = target.getPlayer().get();
        NucleusTeleportHandler.TeleportResult result = Nucleus.getNucleus().getTeleportHandler().teleportPlayer(player, worldTransform, this
                .safeTeleport, true);
        if (result.isSuccess()) {
            src.sendMessage(Nucleus.getNucleus().getMessageProvider().getTextMessageWithFormat("command.spawnother.success.source", target.getName(), world.getWorldName()));
            player.sendMessage(Nucleus.getNucleus().getMessageProvider().getTextMessageWithFormat("command.spawnother.success.target", world.getWorldName()));
            return CommandResult.success();
        }

        throw ReturnMessageException.fromKey("command.spawnother.fail", target.getName(), world.getWorldName());
    }

    private CommandResult isOffline(CommandSource source, User user, Transform<World> worldTransform) throws Exception {
        if (!permissions.testSuffix(source, "offline")) {
            throw new ReturnMessageException(Nucleus.getNucleus().getMessageProvider().getTextMessageWithFormat("command.spawnother.offline.permission"));
        }

        getOrCreateUserOnThread(user.getUniqueId()).set(CoreKeys.LOCATION_ON_LOGIN, new LocationNode(worldTransform.getLocation()));
        user.setLocation(worldTransform.getPosition(), worldTransform.getExtent().getUniqueId());
        source.sendMessage(Nucleus.getNucleus().getMessageProvider().getTextMessageWithFormat("command.spawnother.offline.sendonlogin", user.getName(), worldTransform.getExtent().getName()));
        return CommandResult.success();
    }
}
