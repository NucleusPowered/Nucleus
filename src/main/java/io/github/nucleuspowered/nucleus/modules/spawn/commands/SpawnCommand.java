/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.spawn.commands;

import io.github.nucleuspowered.nucleus.Nucleus;
import io.github.nucleuspowered.nucleus.api.teleport.TeleportResult;
import io.github.nucleuspowered.nucleus.api.teleport.TeleportResults;
import io.github.nucleuspowered.nucleus.api.teleport.TeleportScanners;
import io.github.nucleuspowered.nucleus.internal.annotations.command.Permissions;
import io.github.nucleuspowered.nucleus.internal.annotations.command.RegisterCommand;
import io.github.nucleuspowered.nucleus.internal.command.AbstractCommand;
import io.github.nucleuspowered.nucleus.internal.command.ReturnMessageException;
import io.github.nucleuspowered.nucleus.internal.docgen.annotations.EssentialsEquivalent;
import io.github.nucleuspowered.nucleus.internal.interfaces.Reloadable;
import io.github.nucleuspowered.nucleus.internal.permissions.PermissionInformation;
import io.github.nucleuspowered.nucleus.internal.permissions.SuggestedLevel;
import io.github.nucleuspowered.nucleus.modules.core.services.SafeTeleportService;
import io.github.nucleuspowered.nucleus.modules.spawn.config.GlobalSpawnConfig;
import io.github.nucleuspowered.nucleus.modules.spawn.config.SpawnConfig;
import io.github.nucleuspowered.nucleus.modules.spawn.config.SpawnConfigAdapter;
import io.github.nucleuspowered.nucleus.modules.spawn.events.SendToSpawnEvent;
import io.github.nucleuspowered.nucleus.modules.spawn.helpers.SpawnHelper;
import io.github.nucleuspowered.nucleus.util.CauseStackHelper;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.entity.Transform;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.util.annotation.NonnullByDefault;
import org.spongepowered.api.world.World;
import org.spongepowered.api.world.storage.WorldProperties;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Permissions(suggestedLevel = SuggestedLevel.USER)
@RegisterCommand("spawn")
@EssentialsEquivalent("spawn")
@NonnullByDefault
public class SpawnCommand extends AbstractCommand<Player> implements Reloadable {

    private SpawnConfig sc = new SpawnConfig();
    private final String key = "world";

    @Override public void onReload() {
        this.sc = getServiceUnchecked(SpawnConfigAdapter.class).getNodeOrDefault();
    }

    @Override
    public Map<String, PermissionInformation> permissionSuffixesToRegister() {
        Map<String, PermissionInformation> m = new HashMap<>();
        m.put("force", PermissionInformation.getWithTranslation("permission.spawn.force", SuggestedLevel.ADMIN));
        m.put("otherworlds", PermissionInformation.getWithTranslation("permission.spawn.otherworlds", SuggestedLevel.ADMIN));
        m.put("worlds", PermissionInformation.getWithTranslation("permission.spawn.worlds", SuggestedLevel.ADMIN));
        return m;
    }

    @Override
    public CommandElement[] getArguments() {
        return new CommandElement[] {
            GenericArguments.flags().permissionFlag(this.permissions.getPermissionWithSuffix("force"), "f", "-force").buildWith(
                GenericArguments.optional(requirePermissionArg(GenericArguments.onlyOne(GenericArguments.world(Text.of(this.key))),
                        this.permissions.getPermissionWithSuffix("otherworlds"))))
        };
    }

    @Override
    public CommandResult executeCommand(Player src, CommandContext args, Cause cause) throws Exception {
        boolean force = args.hasAny("f");
        GlobalSpawnConfig gsc = this.sc.getGlobalSpawn();
        WorldProperties wp = args.<WorldProperties>getOne(this.key)
            .orElseGet(() -> gsc.isOnSpawnCommand() ? gsc.getWorld().orElse(src.getWorld().getProperties()) : src.getWorld().getProperties());

        Optional<World> ow = Sponge.getServer().loadWorld(wp.getUniqueId());

        if (!ow.isPresent()) {
            throw new ReturnMessageException(Nucleus.getNucleus().getMessageProvider().getTextMessageWithFormat("command.spawn.noworld"));
        } else if (this.sc.isPerWorldPerms() && !this.permissions.testSuffix(src, "worlds." + ow.get().getName().toLowerCase())) {
            throw new ReturnMessageException(
                    Nucleus.getNucleus().getMessageProvider().getTextMessageWithFormat("command.spawn.nopermsworld", ow.get().getName()));
        }

        Transform<World> worldTransform = SpawnHelper.getSpawn(wp, src);

        SendToSpawnEvent event = new SendToSpawnEvent(worldTransform, src, CauseStackHelper.createCause(src));
        if (Sponge.getEventManager().post(event)) {
            if (event.getCancelReason().isPresent()) {
                throw new ReturnMessageException(
                        Nucleus.getNucleus().getMessageProvider().getTextMessageWithFormat("command.spawnother.self.failed.reason", event.getCancelReason().get()));
            }

            throw new ReturnMessageException(Nucleus.getNucleus().getMessageProvider().getTextMessageWithFormat("command.spawnother.self.failed.noreason"));
        }

        // If we don't have a rotation, then use the current rotation
        TeleportResult result = getServiceUnchecked(SafeTeleportService.class)
                .teleportPlayerSmart(
                        src,
                        SpawnHelper.getSpawn(ow.get().getProperties(), src),
                        true,
                        !force && this.sc.isSafeTeleport(),
                        TeleportScanners.NO_SCAN
                );

        if (result.isSuccessful()) {
            src.sendMessage(Nucleus.getNucleus().getMessageProvider().getTextMessageWithFormat("command.spawn.success", wp.getWorldName()));
            return CommandResult.success();
        }

        if (result == TeleportResults.FAIL_NO_LOCATION) {
            throw ReturnMessageException.fromKey("command.spawn.fail", wp.getWorldName());
        }

        throw ReturnMessageException.fromKey("command.spawn.cancelled", wp.getWorldName());
    }
}
