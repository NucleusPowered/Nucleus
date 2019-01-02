/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.environment.commands;

import io.github.nucleuspowered.nucleus.Nucleus;
import io.github.nucleuspowered.nucleus.Util;
import io.github.nucleuspowered.nucleus.argumentparsers.WeatherArgument;
import io.github.nucleuspowered.nucleus.internal.annotations.RunAsync;
import io.github.nucleuspowered.nucleus.internal.annotations.command.Permissions;
import io.github.nucleuspowered.nucleus.internal.annotations.command.RegisterCommand;
import io.github.nucleuspowered.nucleus.internal.command.AbstractCommand;
import io.github.nucleuspowered.nucleus.internal.command.NucleusParameters;
import io.github.nucleuspowered.nucleus.internal.command.ReturnMessageException;
import io.github.nucleuspowered.nucleus.internal.docgen.annotations.EssentialsEquivalent;
import io.github.nucleuspowered.nucleus.internal.interfaces.Reloadable;
import io.github.nucleuspowered.nucleus.internal.permissions.PermissionInformation;
import io.github.nucleuspowered.nucleus.internal.permissions.SuggestedLevel;
import io.github.nucleuspowered.nucleus.modules.environment.EnvironmentKeys;
import io.github.nucleuspowered.nucleus.modules.environment.config.EnvironmentConfigAdapter;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.util.annotation.NonnullByDefault;
import org.spongepowered.api.world.World;
import org.spongepowered.api.world.storage.WorldProperties;
import org.spongepowered.api.world.weather.Weather;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Permissions
@RunAsync
@RegisterCommand("weather")
@NonnullByDefault
@EssentialsEquivalent({"thunder", "sun", "weather", "sky", "storm", "rain"})
public class WeatherCommand extends AbstractCommand<CommandSource> implements Reloadable {

    private final String weather = "weather";

    private long max = Long.MAX_VALUE;

    @Override public void onReload() {
        this.max = Nucleus.getNucleus().getInternalServiceManager().getServiceUnchecked(EnvironmentConfigAdapter.class).getNodeOrDefault()
                .getMaximumWeatherTimespan();
    }

    @Override
    public Map<String, PermissionInformation> permissionSuffixesToRegister() {
        Map<String, PermissionInformation> m = new HashMap<>();
        m.put("exempt.length", PermissionInformation.getWithTranslation("permission.weather.exempt.length", SuggestedLevel.ADMIN));
        return m;
    }

    @Override
    public CommandElement[] getArguments() {
        return new CommandElement[]{
                NucleusParameters.OPTIONAL_WEAK_WORLD_PROPERTIES_ENABLED_ONLY,
                GenericArguments.onlyOne(new WeatherArgument(Text.of(this.weather))), // More flexible with the arguments we can use.
                NucleusParameters.OPTIONAL_DURATION
        };
    }

    @Override
    public CommandResult executeCommand(CommandSource src, CommandContext args, Cause cause) throws Exception {
        // We can predict the weather on multiple worlds now!
        WorldProperties wp = this.getWorldFromUserOrArgs(src, NucleusParameters.Keys.WORLD, args);
        World w = Sponge.getServer().getWorld(wp.getUniqueId())
            .orElseThrow(() -> ReturnMessageException.fromKey("args.worldproperties.notloaded", wp.getWorldName()));

        // Get whether we locked the weather.
        if (getWorldOnThread(w.getUniqueId()).map(x -> x.get(EnvironmentKeys.LOCKED_WEATHER).orElse(false)).orElse(false)) {
            // Tell the user to unlock first.
            throw ReturnMessageException.fromKey("command.weather.locked", w.getName());
        }

        // Houston, we have a world! Now, what was the forecast?
        Weather we = args.<Weather>getOne(this.weather).get();

        // Have we gotten an accurate forecast? Do we know how long this weather spell will go on for?
        Optional<Long> oi = args.getOne(NucleusParameters.Keys.DURATION);

        // Even weather masters have their limits. Sometimes.
        if (this.max > 0 && oi.orElse(Long.MAX_VALUE) > this.max && !this.permissions.testSuffix(src, "exempt.length")) {
            throw ReturnMessageException.fromKey("command.weather.toolong", Util.getTimeStringFromSeconds(this.max));
        }

        if (oi.isPresent()) {
            // YES! I should get a job at the weather service and show them how it's done!
            Task.builder().execute(() -> w.setWeather(we, oi.get() * 20L)).submit(Nucleus.getNucleus());
            src.sendMessage(Nucleus.getNucleus().getMessageProvider().getTextMessageWithFormat("command.weather.time", we.getName(), w.getName(), Util.getTimeStringFromSeconds(oi.get())));
        } else {
            // No, probably because I've already gotten a job at the weather service...
            Task.builder().execute(() -> w.setWeather(we)).submit(Nucleus.getNucleus());
            src.sendMessage(Nucleus.getNucleus().getMessageProvider().getTextMessageWithFormat("command.weather.set", we.getName(), w.getName()));
        }

        // The weather control device has been activated!
        return CommandResult.success();
    }


}
