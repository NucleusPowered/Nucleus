/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.vanish.commands;

import io.github.nucleuspowered.nucleus.modules.vanish.VanishKeys;
import io.github.nucleuspowered.nucleus.modules.vanish.VanishPermissions;
import io.github.nucleuspowered.nucleus.modules.vanish.services.VanishService;
import io.github.nucleuspowered.nucleus.core.scaffold.command.ICommandContext;
import io.github.nucleuspowered.nucleus.core.scaffold.command.ICommandExecutor;
import io.github.nucleuspowered.nucleus.core.scaffold.command.ICommandResult;
import io.github.nucleuspowered.nucleus.core.scaffold.command.NucleusParameters;
import io.github.nucleuspowered.nucleus.core.scaffold.command.annotation.Command;
import io.github.nucleuspowered.nucleus.core.scaffold.command.annotation.EssentialsEquivalent;
import io.github.nucleuspowered.nucleus.core.services.INucleusServiceCollection;
import io.github.nucleuspowered.nucleus.core.services.impl.storage.dataobjects.keyed.IKeyedDataObject;
import org.spongepowered.api.command.exception.CommandException;
import org.spongepowered.api.command.parameter.Parameter;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.entity.living.player.gamemode.GameModes;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;

@EssentialsEquivalent({"vanish", "v"})
@Command(
        aliases = {"vanish", "v"},
        basePermission = VanishPermissions.BASE_VANISH,
        commandDescriptionKey = "vanish",
        associatedPermissions = {
                VanishPermissions.VANISH_SEE,
                VanishPermissions.VANISH_PERSIST
        }
)
public class VanishCommand implements ICommandExecutor {

    @Override
    public Parameter[] parameters(final INucleusServiceCollection serviceCollection) {
        return new Parameter[] {
                serviceCollection.commandElementSupplier().createOnlyOtherUserPermissionElement(VanishPermissions.OTHERS_VANISH),
                NucleusParameters.OPTIONAL_ONE_TRUE_FALSE
        };
    }

    @Override public ICommandResult execute(final ICommandContext context) throws CommandException {
        final User ou = context.getUserFromArgs();
        if (ou.player().isPresent()) {
            return this.onPlayer(context, ou.player().get());
        }

        if (!context.testPermissionFor(ou, "persist")) {
            return context.errorResult("command.vanish.noperm", ou.name());
        }

        final boolean result;
        try (final IKeyedDataObject.Value<Boolean> value = context
                .getServiceCollection()
                .storageManager()
                .getUserService()
                .getOrNewOnThread(ou.uniqueId())
                .getAndSet(VanishKeys.VANISH_STATUS)) {
            result = context.getOne(NucleusParameters.OPTIONAL_ONE_TRUE_FALSE).orElseGet(() -> !value.getValue().orElse(false));
            value.setValue(result);
            final VanishService service = context.getServiceCollection().getServiceUnchecked(VanishService.class);
            if (result) {
                service.vanishPlayer(ou);
            } else {
                service.unvanishPlayer(ou);
            }
        }

        context.sendMessage(
                "command.vanish.successuser",
                ou.name(),
                result ? "loc:command.vanish.vanished" : "loc:command.vanish.visible");

        return context.successResult();
    }

    private ICommandResult onPlayer(final ICommandContext context, final ServerPlayer playerToVanish) throws CommandException {
        if (playerToVanish.get(Keys.GAME_MODE).orElseGet(GameModes.NOT_SET).equals(GameModes.SPECTATOR.get())) {
            return context.errorResult("command.vanish.fail");
        }

        // If we don't specify whether to vanish, toggle
        final boolean toVanish = context.getOne(NucleusParameters.OPTIONAL_ONE_TRUE_FALSE).orElse(!playerToVanish.get(Keys.VANISH).orElse(false));
        final VanishService service = context.getServiceCollection().getServiceUnchecked(VanishService.class);
        if (toVanish) {
            service.vanishPlayer(playerToVanish.user());
        } else {
            service.unvanishPlayer(playerToVanish.user());
        }

        context.sendMessageTo(
                playerToVanish,
                "command.vanish.success",
                toVanish ? "loc:command.vanish.vanished" : "loc:command.vanish.visible");

        if (!context.is(playerToVanish)) {
            context.sendMessage(
                    "command.vanish.successplayer",
                    context.getDisplayName(playerToVanish.uniqueId()),
                    toVanish ? "loc:command.vanish.vanished" : "loc:command.vanish.visible");
        }

        return context.successResult();
    }
}
