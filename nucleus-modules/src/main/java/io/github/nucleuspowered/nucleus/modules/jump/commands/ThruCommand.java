/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.jump.commands;

import io.github.nucleuspowered.nucleus.Util;
import io.github.nucleuspowered.nucleus.api.teleport.data.TeleportScanners;
import io.github.nucleuspowered.nucleus.modules.jump.JumpPermissions;
import io.github.nucleuspowered.nucleus.modules.jump.config.JumpConfig;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandContext;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandExecutor;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandResult;
import io.github.nucleuspowered.nucleus.scaffold.command.annotation.Command;
import io.github.nucleuspowered.nucleus.scaffold.command.annotation.CommandModifier;
import io.github.nucleuspowered.nucleus.scaffold.command.modifier.CommandModifiers;
import io.github.nucleuspowered.nucleus.services.INucleusServiceCollection;
import io.github.nucleuspowered.nucleus.services.interfaces.IReloadableService;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.util.annotation.NonnullByDefault;
import org.spongepowered.api.util.blockray.BlockRay;
import org.spongepowered.api.util.blockray.BlockRayHit;
import org.spongepowered.api.world.World;

@NonnullByDefault
@Command(
        aliases = {"thru", "through"},
        basePermission = JumpPermissions.BASE_THRU,
        commandDescriptionKey = "thru",
        modifiers = {
                @CommandModifier(value = CommandModifiers.HAS_COOLDOWN, exemptPermission = JumpPermissions.EXEMPT_COOLDOWN_THRU),
                @CommandModifier(value = CommandModifiers.HAS_WARMUP, exemptPermission = JumpPermissions.EXEMPT_WARMUP_THRU),
                @CommandModifier(value = CommandModifiers.HAS_COST, exemptPermission = JumpPermissions.EXEMPT_COST_THRU)
        }
)
public class ThruCommand implements ICommandExecutor<Player>, IReloadableService.Reloadable {

    private int maxThru = 20;

    @Override public ICommandResult execute(ICommandContext<? extends Player> context) throws CommandException {
        Player player = context.getIfPlayer();
        BlockRay<World> playerBlockRay = BlockRay.from(player).distanceLimit(this.maxThru).build();
        World world = player.getWorld();

        // First, see if we get a wall.
        while (playerBlockRay.hasNext()) {
            // Once we have a wall, we'll break out.
            if (!world.getBlockType(playerBlockRay.next().getBlockPosition()).equals(BlockTypes.AIR)) {
                break;
            }
        }

        // Even if we did find a wall, no good if we are at the end of the ray.
        if (!playerBlockRay.hasNext()) {
            return context.errorResult("command.thru.nowall");
        }

        do {
            BlockRayHit<World> b = playerBlockRay.next();
            if (player.getWorld().getBlockType(b.getBlockPosition()).equals(BlockTypes.AIR)) {
                if (!Util.isLocationInWorldBorder(b.getLocation())) {
                    return context.errorResult("command.jump.outsideborder");
                }

                // If we can go, do so.
                boolean result =
                        context.getServiceCollection().teleportService().teleportPlayerSmart(
                                player,
                                b.getLocation(),
                                false,
                                true,
                                TeleportScanners.NO_SCAN.get()
                        ).isSuccessful();
                if (result) {
                    context.sendMessage("command.thru.success");
                    return context.successResult();
                } else {
                    return context.errorResult("command.thru.notsafe");
                }
            }
        } while (playerBlockRay.hasNext());

        return context.errorResult("command.thru.nospot");
    }

    @Override
    public void onReload(INucleusServiceCollection serviceCollection) {
        this.maxThru = serviceCollection.moduleDataProvider().getModuleConfig(JumpConfig.class).getMaxThru();
    }

}
