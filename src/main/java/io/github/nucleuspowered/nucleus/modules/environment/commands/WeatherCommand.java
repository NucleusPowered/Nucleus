/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.environment.commands;

import com.google.inject.Inject;
import io.github.nucleuspowered.nucleus.Util;
import io.github.nucleuspowered.nucleus.api.data.NucleusWorld;
import io.github.nucleuspowered.nucleus.argumentparsers.TimespanArgument;
import io.github.nucleuspowered.nucleus.argumentparsers.WeatherArgument;
import io.github.nucleuspowered.nucleus.dataservices.loaders.WorldDataManager;
import io.github.nucleuspowered.nucleus.internal.annotations.Permissions;
import io.github.nucleuspowered.nucleus.internal.annotations.RegisterCommand;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.world.Locatable;
import org.spongepowered.api.world.World;
import org.spongepowered.api.world.storage.WorldProperties;
import org.spongepowered.api.world.weather.Weather;

import java.util.Optional;

@Permissions
@RegisterCommand("weather")
public class WeatherCommand extends io.github.nucleuspowered.nucleus.internal.command.AbstractCommand<CommandSource> {
    private final String world = "world";
    private final String weather = "weather";
    private final String duration = "duration";
    private final String timespan = "timespan";

    @Inject private WorldDataManager loader;

    @Override
    public CommandElement[] getArguments() {
        return new CommandElement[]{
                GenericArguments.optionalWeak(GenericArguments.onlyOne(GenericArguments.world(Text.of(world)))),
                GenericArguments.onlyOne(new WeatherArgument(Text.of(weather))), // More flexible with the arguments we can use.
                GenericArguments.firstParsing(
                        GenericArguments.onlyOne(GenericArguments.optional(GenericArguments.integer(Text.of(duration)))),
                        GenericArguments.onlyOne(GenericArguments.optional(new TimespanArgument(Text.of(timespan))))
                )
        };
    }

    @Override
    public CommandResult executeCommand(CommandSource src, CommandContext args) throws Exception {
        // We can predict the weather on multiple worlds now!
        WorldProperties wp = args.<WorldProperties>getOne(world).orElse(null);
        World w;
        if (wp != null) {
            w = Sponge.getServer().getWorld(wp.getUniqueId()).get();
        } else {
            // Actually, we just care about where we are.
            if (src instanceof Locatable) {
                w = ((Locatable) src).getWorld();
            } else {
                // As supreme overlord of the worlds... you have to specify one.
                src.sendMessage(plugin.getMessageProvider().getTextMessageWithFormat("command.specifyworld"));
                return CommandResult.empty();
            }
        }

        // Get whether we locked the weather.
        NucleusWorld ew = loader.getWorld(w).get();
        if (ew.isLockWeather()) {
            // Tell the user to unlock first.
            src.sendMessage(plugin.getMessageProvider().getTextMessageWithFormat("command.weather.locked", w.getName()));
            return CommandResult.empty();
        }

        // Houston, we have a world! Now, what was the forecast?
        Weather we = args.<Weather>getOne(weather).get();

        // Have we gotten an accurate forecast? Do we know how long this weather spell will go on for?
        Optional<Long> oi = args.getOne(timespan);
        if (!oi.isPresent()) {
            Optional<Integer> i = args.getOne(duration);
            oi =  i.isPresent() ? Optional.of((long)i.get()) : Optional.empty();
        }

        if (oi.isPresent()) {
            // YES! I should get a job at the weather service and show them how it's done!
            w.setWeather(we, oi.get());
            src.sendMessage(plugin.getMessageProvider().getTextMessageWithFormat("command.weather.time", we.getName(), w.getName(), Util.getTimeStringFromSeconds(oi.get())));
        } else {
            // No, probably because I've already gotten a job at the weather service...
            w.setWeather(we);
            src.sendMessage(plugin.getMessageProvider().getTextMessageWithFormat("command.weather.set", we.getName(), w.getName()));
        }

        // The weather control device has been activated!
        return CommandResult.success();
    }
}
