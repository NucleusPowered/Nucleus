/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.admin.commands.gamemode;

import com.google.inject.Inject;
import io.github.nucleuspowered.nucleus.core.Registry;
import io.github.nucleuspowered.nucleus.modules.admin.AdminPermissions;
import io.github.nucleuspowered.nucleus.core.scaffold.command.ICommandContext;
import io.github.nucleuspowered.nucleus.core.scaffold.command.ICommandResult;
import io.github.nucleuspowered.nucleus.core.scaffold.command.annotation.Command;
import io.github.nucleuspowered.nucleus.core.scaffold.command.annotation.CommandModifier;
import io.github.nucleuspowered.nucleus.core.scaffold.command.annotation.EssentialsEquivalent;
import io.github.nucleuspowered.nucleus.core.scaffold.command.modifier.CommandModifiers;
import io.github.nucleuspowered.nucleus.core.services.INucleusServiceCollection;
import net.kyori.adventure.text.Component;
import org.spongepowered.api.command.exception.CommandException;
import org.spongepowered.api.command.parameter.Parameter;
import org.spongepowered.api.command.parameter.managed.standard.VariableValueParameters;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.gamemode.GameMode;
import org.spongepowered.api.entity.living.player.gamemode.GameModes;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.registry.RegistryTypes;

import java.util.Optional;

@Command(aliases = {"gamemode", "gm"},
        basePermission = AdminPermissions.BASE_GAMEMODE,
        commandDescriptionKey = "gamemode",
        modifiers =
                {
                        @CommandModifier(value = CommandModifiers.HAS_WARMUP, exemptPermission = AdminPermissions.EXEMPT_WARMUP_GAMEMODE),
                        @CommandModifier(value = CommandModifiers.HAS_COOLDOWN, exemptPermission = AdminPermissions.EXEMPT_COOLDOWN_GAMEMODE),
                        @CommandModifier(value = CommandModifiers.HAS_COST, exemptPermission = AdminPermissions.EXEMPT_COST_GAMEMODE)
                },
        associatedPermissions = AdminPermissions.GAMEMODE_OTHER
)
@EssentialsEquivalent(value = {"gamemode", "gm"}, isExact = false, notes = "/gm does not toggle between survival and creative, use /gmt for that")
public class GamemodeCommand extends GamemodeBase {

    private final Parameter.Value<GameMode> gameModeParameter =
            Parameter.builder(GameMode.class)
                    .key("Game Mode")
                    .addParser(VariableValueParameters.registryEntryBuilder(RegistryTypes.GAME_MODE).defaultNamespace("sponge").build())
                    .build();

    final Parameter.Value<ServerPlayer> playerValue;

    @Inject
    public GamemodeCommand(final INucleusServiceCollection serviceCollection) {
        this.playerValue = serviceCollection.commandElementSupplier().createOnlyOtherPlayerPermissionElement(AdminPermissions.GAMEMODE_OTHER);
    }

    @Override
    public Parameter[] parameters(final INucleusServiceCollection serviceCollection) {
        return new Parameter[] {
                this.playerValue,
                this.gameModeParameter
        };
    }

    @Override
    public ICommandResult execute(final ICommandContext context) throws CommandException {
        final Player user;
        final Optional<? extends GameMode> ogm;
        if (context.hasAny(this.playerValue)) {
            user = context.getPlayerFromArgs();
        } else {
            user = context.getIfPlayer();
        }
        ogm = context.getOne(this.gameModeParameter);

        if (!ogm.isPresent()) {
            final Component mode = user.get(Keys.GAME_MODE).orElseGet(GameModes.SURVIVAL::get).asComponent();
            if (context.is(user)) {
                context.sendMessage("command.gamemode.get.base", mode);
            } else {
                context.sendMessage("command.gamemode.get.other", user.name(), mode);
            }

            return context.successResult();
        }

        final GameMode gm = ogm.get();
        return this.baseCommand(context, user, gm);
    }
}
