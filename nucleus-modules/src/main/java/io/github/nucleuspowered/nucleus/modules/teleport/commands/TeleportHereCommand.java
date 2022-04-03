/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.teleport.commands;

import com.google.inject.Inject;
import io.github.nucleuspowered.nucleus.api.teleport.data.TeleportResult;
import io.github.nucleuspowered.nucleus.modules.teleport.TeleportPermissions;
import io.github.nucleuspowered.nucleus.modules.teleport.config.TeleportConfig;
import io.github.nucleuspowered.nucleus.modules.teleport.services.PlayerTeleporterService;
import io.github.nucleuspowered.nucleus.core.scaffold.command.ICommandContext;
import io.github.nucleuspowered.nucleus.core.scaffold.command.ICommandExecutor;
import io.github.nucleuspowered.nucleus.core.scaffold.command.ICommandResult;
import io.github.nucleuspowered.nucleus.core.scaffold.command.annotation.Command;
import io.github.nucleuspowered.nucleus.core.scaffold.command.annotation.EssentialsEquivalent;
import io.github.nucleuspowered.nucleus.core.services.INucleusServiceCollection;
import io.github.nucleuspowered.nucleus.core.services.interfaces.IPermissionService;
import io.github.nucleuspowered.nucleus.core.services.interfaces.IReloadableService;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.exception.CommandException;
import org.spongepowered.api.command.parameter.Parameter;
import org.spongepowered.api.command.parameter.managed.Flag;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;

import java.util.UUID;

/**
 * NOTE: TeleportHere is considered an admin command, as there is a potential
 * for abuse for non-admin players trying to pull players. No cost or warmups
 * will be applied. /tpahere should be used instead in these circumstances.
 */
@EssentialsEquivalent(value = {"tphere", "s", "tpohere"}, isExact = false,
        notes = "If you have permission, this will override '/tptoggle' automatically.")
@Command(
        aliases = {"tphere", "tph"},
        basePermission = TeleportPermissions.BASE_TPHERE,
        commandDescriptionKey = "tphere",
        associatedPermissions = {
                TeleportPermissions.TPHERE_OFFLINE,
                TeleportPermissions.TPTOGGLE_EXEMPT
        }
)
public class TeleportHereCommand implements ICommandExecutor, IReloadableService.Reloadable {

    private boolean isDefaultQuiet = false;

    private final Parameter.Value<Boolean> quietOption = Parameter.bool().key("quiet").build();

    private final Parameter.Value<UUID> userToWarp;
    private final Parameter.Value<ServerPlayer> playerToWarp;

    @Inject
    public TeleportHereCommand(final INucleusServiceCollection serviceCollection) {
        final IPermissionService permissionService = serviceCollection.permissionService();
        this.userToWarp = Parameter.user()
                .key("Offline player to warp")
                .requirements(cause -> permissionService.hasPermission(cause, TeleportPermissions.TPHERE_OFFLINE))
                .build();
        this.playerToWarp = Parameter.player()
                .key("Player to warp")
                .build();
    }

    @Override
    public void onReload(final INucleusServiceCollection serviceCollection) {
        this.isDefaultQuiet =
                serviceCollection.configProvider()
                        .getModuleConfig(TeleportConfig.class)
                        .isDefaultQuiet();
    }

    @Override
    public Flag[] flags(final INucleusServiceCollection serviceCollection) {
        return new Flag[] {
                Flag.of("f"),
                Flag.builder().alias("q").setParameter(this.quietOption).build()
        };
    }

    @Override
    public Parameter[] parameters(final INucleusServiceCollection serviceCollection) {
        return new Parameter[] {
                Parameter.firstOf(
                        this.userToWarp,
                        this.playerToWarp
                )
        };
    }

    @Override public ICommandResult execute(final ICommandContext context) throws CommandException {
        final ServerPlayer source = context.requirePlayer();
        final boolean beQuiet = context.getOne(this.quietOption).orElse(this.isDefaultQuiet);
        final User target =
                context.getOne(this.userToWarp).flatMap(x -> Sponge.server().userManager().load(x).join())
                        .orElseGet(() -> context.requireOne(this.playerToWarp).user());
        final PlayerTeleporterService sts = context.getServiceCollection().getServiceUnchecked(PlayerTeleporterService.class);
        if (target.player().isPresent()) {
            final TeleportResult result = sts.teleportWithMessage(
                    source,
                    target.player().get(),
                    source,
                    false,
                    false,
                    beQuiet
            );
            return result.isSuccessful() ? context.successResult() : context.failResult();
        } else {
            // Update the offline player's next location
            target.setLocation(source.world().key(), source.position());
            target.setRotation(source.rotation());
            context.sendMessage("command.tphere.offlinesuccess", target.name());
        }

        return context.successResult();
    }
}
