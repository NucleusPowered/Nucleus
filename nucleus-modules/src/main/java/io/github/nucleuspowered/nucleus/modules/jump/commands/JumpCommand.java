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
import io.github.nucleuspowered.nucleus.scaffold.command.annotation.EssentialsEquivalent;
import io.github.nucleuspowered.nucleus.scaffold.command.modifier.CommandModifiers;
import io.github.nucleuspowered.nucleus.services.INucleusServiceCollection;
import io.github.nucleuspowered.nucleus.services.interfaces.IReloadableService;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.data.property.block.PassableProperty;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.util.Direction;
import org.spongepowered.api.util.annotation.NonnullByDefault;
import org.spongepowered.api.util.blockray.BlockRay;
import org.spongepowered.api.util.blockray.BlockRayHit;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.util.Optional;

import javax.annotation.Nullable;

@EssentialsEquivalent({"jump", "j", "jumpto"})
@NonnullByDefault
@Command(
        aliases = {"jump", "j", "jmp"},
        basePermission = JumpPermissions.BASE_JUMP,
        commandDescriptionKey = "jump",
        modifiers = {
            @CommandModifier(value = CommandModifiers.HAS_COOLDOWN, exemptPermission = JumpPermissions.EXEMPT_COOLDOWN_JUMP),
            @CommandModifier(value = CommandModifiers.HAS_WARMUP, exemptPermission = JumpPermissions.EXEMPT_WARMUP_JUMP),
            @CommandModifier(value = CommandModifiers.HAS_COST, exemptPermission = JumpPermissions.EXEMPT_COST_JUMP)
        }
)
public class JumpCommand implements ICommandExecutor<Player>, IReloadableService.Reloadable {

    private int maxJump = 20;

    // Original code taken from EssentialCmds. With thanks to 12AwsomeMan34 for
    // the initial contribution.
    @Override
    public ICommandResult execute(ICommandContext<? extends Player> context) throws CommandException {
        Player player = context.getIfPlayer();
        BlockRay<World> playerBlockRay = BlockRay.from(player).distanceLimit(this.maxJump).build();

        BlockRayHit<World> finalHitRay = null;

        // Iterate over the blocks until we get a solid block.
        while (finalHitRay == null && playerBlockRay.hasNext()) {
            BlockRayHit<World> currentHitRay = playerBlockRay.next();
            if (!player.getWorld().getBlockType(currentHitRay.getBlockPosition()).equals(BlockTypes.AIR)) {
                finalHitRay = currentHitRay;
            }
        }

        if (finalHitRay == null) {
            // We didn't find anywhere to jump to.
            return context.errorResult("command.jump.noblock");
        }

        // If the block not passable, then it is a solid block
        Location<World> finalLocation = finalHitRay.getLocation();
        Optional<PassableProperty> pp = finalHitRay.getLocation().getProperty(PassableProperty.class);
        if (pp.isPresent() && !getFromBoxed(pp.get().getValue())) {
            finalLocation = finalLocation.add(0, 1, 0);
        } else {
            Optional<PassableProperty> ppbelow = finalHitRay.getLocation().getRelative(Direction.DOWN).getProperty(PassableProperty.class);
            if (ppbelow.isPresent() && !getFromBoxed(ppbelow.get().getValue())) {
                finalLocation = finalLocation.sub(0, 1, 0);
            }
        }

        if (!Util.isLocationInWorldBorder(finalLocation)) {
            return context.errorResult("command.jump.outsideborder");
        }

        boolean result = context.getServiceCollection()
                .teleportService()
                .teleportPlayerSmart(
                        player,
                        finalLocation,
                        false,
                        true,
                        TeleportScanners.NO_SCAN.get()
                ).isSuccessful();
        if (result) {
            context.sendMessage("command.jump.success");
            return context.successResult();
        }

        return context.errorResult("command.jump.notsafe");
    }

    private boolean getFromBoxed(@Nullable Boolean bool) {
        return bool != null ? bool : false;
    }

    @Override
    public void onReload(INucleusServiceCollection serviceCollection) {
        this.maxJump = serviceCollection.moduleDataProvider().getModuleConfig(JumpConfig.class).getMaxJump();
    }
}
