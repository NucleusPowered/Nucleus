/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.back.commands;

import com.google.inject.Inject;
import io.github.nucleuspowered.nucleus.api.teleport.data.TeleportResult;
import io.github.nucleuspowered.nucleus.api.teleport.data.TeleportScanners;
import io.github.nucleuspowered.nucleus.api.util.WorldTransform;
import io.github.nucleuspowered.nucleus.modules.back.BackPermissions;
import io.github.nucleuspowered.nucleus.modules.back.config.BackConfig;
import io.github.nucleuspowered.nucleus.modules.back.services.BackHandler;
import io.github.nucleuspowered.nucleus.core.scaffold.command.ICommandContext;
import io.github.nucleuspowered.nucleus.core.scaffold.command.ICommandExecutor;
import io.github.nucleuspowered.nucleus.core.scaffold.command.ICommandResult;
import io.github.nucleuspowered.nucleus.core.scaffold.command.annotation.Command;
import io.github.nucleuspowered.nucleus.core.scaffold.command.annotation.CommandModifier;
import io.github.nucleuspowered.nucleus.core.scaffold.command.annotation.EssentialsEquivalent;
import io.github.nucleuspowered.nucleus.core.scaffold.command.modifier.CommandModifiers;
import io.github.nucleuspowered.nucleus.core.services.INucleusServiceCollection;
import io.github.nucleuspowered.nucleus.core.services.interfaces.INucleusLocationService;
import io.github.nucleuspowered.nucleus.core.services.interfaces.IPermissionService;
import io.github.nucleuspowered.nucleus.core.services.interfaces.IReloadableService;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.exception.CommandException;
import org.spongepowered.api.command.parameter.managed.Flag;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.world.server.ServerLocation;
import org.spongepowered.api.world.server.ServerWorld;

import java.util.Optional;

@EssentialsEquivalent({"back", "return"})
@Command(
        aliases = {"back", "return"},
        basePermission = BackPermissions.BASE_BACK,
        commandDescriptionKey = "back",
        modifiers = {
            @CommandModifier(value = CommandModifiers.HAS_WARMUP, exemptPermission = BackPermissions.EXEMPT_WARMUP_BACK),
            @CommandModifier(value = CommandModifiers.HAS_COOLDOWN, exemptPermission = BackPermissions.EXEMPT_COOLDOWN_BACK),
            @CommandModifier(value = CommandModifiers.HAS_COST, exemptPermission = BackPermissions.EXEMPT_COST_BACK)
        },
        associatedPermissions = { BackPermissions.TPPOS_BORDER, BackPermissions.BACK_EXEMPT_SAMEDIMENSION })
public class BackCommand implements ICommandExecutor, IReloadableService.Reloadable {

    private boolean sameDimensionCheck = false;

    private final Flag forceFlag = Flag.of("f", "force");
    private final Flag borderFlag;

    @Inject
    public BackCommand(final IPermissionService service) {
        this.borderFlag =
                Flag.builder().alias("b").alias("border").setRequirement(cause -> service.hasPermission(cause, BackPermissions.TPPOS_BORDER)).build();
    }

    @Override
    public Flag[] flags(final INucleusServiceCollection serviceCollection) {
        return new Flag[] {
                this.forceFlag,
                this.borderFlag
        };
    }

    @Override
    public ICommandResult execute(final ICommandContext context) throws CommandException {
        final BackHandler handler = context.getServiceCollection().getServiceUnchecked(BackHandler.class);
        final ServerPlayer src = context.getIfPlayer();
        final Optional<WorldTransform> ol = handler.getLastLocation(src.uniqueId());
        if (!ol.isPresent()) {
            return context.errorResult("command.back.noloc");
        }

        final boolean border = context.hasFlag("b");
        final WorldTransform loc = ol.get();
        if (this.sameDimensionCheck && src.world().key() != loc.getWorldResourceKey()) {
            if (!context.testPermission(BackPermissions.BACK_EXEMPT_SAMEDIMENSION)) {
                return context.errorResult("command.back.sameworld");
            }
        }

        final ServerWorld world = Sponge.server().worldManager().world(loc.getWorldResourceKey()).get();
        final INucleusLocationService service = context.getServiceCollection().teleportService();
        try (final INucleusLocationService.BorderDisableSession ac = service.temporarilyDisableBorder(border, world)) {
            final TeleportResult result = service.teleportPlayerSmart(
                            src,
                            ServerLocation.of(world, loc.getPosition()),
                            loc.getRotation(),
                            false,
                            !context.hasFlag("f"),
                            TeleportScanners.NO_SCAN.get()
                    );
            if (result.isSuccessful()) {
                context.sendMessage("command.back.success");
                return context.successResult();
            } else if (result == TeleportResult.FAIL_NO_LOCATION) {
                return context.errorResult("command.back.nosafe");
            }

            return context.errorResult("command.back.cancelled");
        }
    }

    @Override
    public void onReload(final INucleusServiceCollection serviceCollection) {
        this.sameDimensionCheck = serviceCollection.configProvider().getModuleConfig(BackConfig.class).isOnlySameDimension();
    }
}
