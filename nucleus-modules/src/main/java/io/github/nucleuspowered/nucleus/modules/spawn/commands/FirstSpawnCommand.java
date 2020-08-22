/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.spawn.commands;

import io.github.nucleuspowered.nucleus.api.teleport.data.TeleportResult;
import io.github.nucleuspowered.nucleus.api.teleport.data.TeleportScanners;
import io.github.nucleuspowered.nucleus.configurate.datatypes.LocationNode;
import io.github.nucleuspowered.nucleus.modules.spawn.SpawnKeys;
import io.github.nucleuspowered.nucleus.modules.spawn.SpawnPermissions;
import io.github.nucleuspowered.nucleus.modules.spawn.config.SpawnConfig;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandContext;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandExecutor;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandResult;
import io.github.nucleuspowered.nucleus.scaffold.command.annotation.Command;
import io.github.nucleuspowered.nucleus.scaffold.command.annotation.CommandModifier;
import io.github.nucleuspowered.nucleus.scaffold.command.modifier.CommandModifiers;
import io.github.nucleuspowered.nucleus.services.INucleusServiceCollection;
import io.github.nucleuspowered.nucleus.services.interfaces.IReloadableService;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.entity.Transform;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.util.annotation.NonnullByDefault;
import org.spongepowered.api.world.World;

import java.util.Optional;

@NonnullByDefault
@Command(
        aliases = "firstspawn",
        basePermission = SpawnPermissions.BASE_FIRSTSPAWN,
        commandDescriptionKey = "firstspawn",
        modifiers = {
                @CommandModifier(value = CommandModifiers.HAS_COOLDOWN, exemptPermission = SpawnPermissions.EXEMPT_COOLDOWN_FIRSTSPAWN),
                @CommandModifier(value = CommandModifiers.HAS_WARMUP, exemptPermission = SpawnPermissions.EXEMPT_WARMUP_FIRSTSPAWN),
                @CommandModifier(value = CommandModifiers.HAS_COST, exemptPermission = SpawnPermissions.EXEMPT_COST_FIRSTSPAWN)
        }
)
public class FirstSpawnCommand implements ICommandExecutor<Player>, IReloadableService.Reloadable {

    private boolean isSafeTeleport = true;

    @Override public ICommandResult execute(ICommandContext<? extends Player> context) throws CommandException {

        Optional<Transform<World>> olwr =
                context.getServiceCollection().storageManager()
                        .getGeneralService()
                        .getOrNewOnThread()
                        .get(SpawnKeys.FIRST_SPAWN_LOCATION)
                        .flatMap(LocationNode::getTransformIfExists);
        if (!olwr.isPresent()) {
            return context.errorResult("command.firstspawn.notset");
        }

        TeleportResult result = context.getServiceCollection()
                .teleportService()
                .teleportPlayerSmart(
                        context.getIfPlayer(),
                        olwr.get(),
                        true,
                        this.isSafeTeleport,
                        TeleportScanners.NO_SCAN.get()
                );
        if (result.isSuccessful()) {
            context.sendMessage("command.firstspawn.success");
            return context.successResult();
        }

        return context.errorResult("command.firstspawn.fail");
    }

    @Override public void onReload(INucleusServiceCollection serviceCollection) {
        this.isSafeTeleport = serviceCollection.moduleDataProvider().getModuleConfig(SpawnConfig.class).isSafeTeleport();
    }
}
