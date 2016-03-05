/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.commands.environment;

import com.google.inject.Inject;
import io.github.nucleuspowered.nucleus.Util;
import io.github.nucleuspowered.nucleus.api.PluginModule;
import io.github.nucleuspowered.nucleus.api.data.NucleusWorld;
import io.github.nucleuspowered.nucleus.internal.CommandBase;
import io.github.nucleuspowered.nucleus.internal.annotations.*;
import io.github.nucleuspowered.nucleus.internal.services.datastore.WorldConfigLoader;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.command.source.LocatedSource;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.world.storage.WorldProperties;

import java.util.Optional;

/**
 * Locks (or unlocks) the weather.
 *
 * Permission: nucleus.lockweather.base
 */
@Permissions
@RunAsync
@Modules(PluginModule.ENVIRONMENT)
@RegisterCommand({ "lockweather", "killweather" })
@NoWarmup
@NoCooldown
@NoCost
public class LockWeatherCommand extends CommandBase<CommandSource> {

    @Inject private WorldConfigLoader loader;

    private final String worldKey = "world";
    private final String toggleKey = "toggle";

    @Override
    public CommandSpec createSpec() {
        return CommandSpec.builder().arguments(
                GenericArguments.onlyOne(GenericArguments.optionalWeak(GenericArguments.world(Text.of(worldKey)))),
                GenericArguments.onlyOne(GenericArguments.optional(GenericArguments.bool(Text.of(toggleKey))))
        ).executor(this).build();
    }

    @Override
    public CommandResult executeCommand(CommandSource src, CommandContext args) throws Exception {
        Optional<WorldProperties> world = args.<WorldProperties>getOne(worldKey);
        WorldProperties wp;
        if (world.isPresent()) {
            // World was specified
            wp = world.get();
        } else if (src instanceof LocatedSource) {
            // Player/CommandBlock world
            wp = ((LocatedSource) src).getWorld().getProperties();
        } else {
            // Default world - console!
            src.sendMessage(Text.of(TextColors.RED, Util.getMessageWithFormat("command.specifyworld")));
            return CommandResult.empty();
        }

        NucleusWorld ws = loader.getWorld(wp.getUniqueId());
        boolean toggle = args.<Boolean>getOne(toggleKey).orElse(!ws.isLockWeather());

        ws.setLockWeather(toggle);
        if (toggle) {
            src.sendMessage(Text.of(TextColors.GREEN, Util.getMessageWithFormat("command.lockweather.locked", wp.getWorldName())));
        } else {
            src.sendMessage(Text.of(TextColors.GREEN, Util.getMessageWithFormat("command.lockweather.unlocked", wp.getWorldName())));
        }

        return CommandResult.success();
    }
}
