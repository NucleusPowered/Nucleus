/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.misc.commands;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import io.github.nucleuspowered.nucleus.Util;
import io.github.nucleuspowered.nucleus.modules.misc.MiscPermissions;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandContext;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandExecutor;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandResult;
import io.github.nucleuspowered.nucleus.scaffold.command.annotation.Command;
import io.github.nucleuspowered.nucleus.scaffold.command.annotation.EssentialsEquivalent;
import io.github.nucleuspowered.nucleus.services.INucleusServiceCollection;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.service.pagination.PaginationList;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.action.TextActions;
import org.spongepowered.api.text.format.TextColor;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.util.annotation.NonnullByDefault;
import org.spongepowered.api.world.GeneratorType;
import org.spongepowered.api.world.World;

import java.lang.management.ManagementFactory;
import java.text.DecimalFormat;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@NonnullByDefault
@Command(aliases = { "serverstat", "uptime" }, basePermission = MiscPermissions.BASE_SERVERSTAT, commandDescriptionKey = "serverstat")
@EssentialsEquivalent(value = {"gc", "lag", "mem", "memory", "uptime", "tps", "entities"})
public class ServerStatCommand implements ICommandExecutor<CommandSource> {

    private static final DecimalFormat TPS_FORMAT = new DecimalFormat("#0.00");

    @Override
    public CommandElement[] parameters(INucleusServiceCollection serviceCollection) {
        return new CommandElement[] {
                GenericArguments.flags().flag("c", "s", "-compact", "-summary").buildWith(GenericArguments.none())
        };
    }

    @Override public ICommandResult execute(ICommandContext<? extends CommandSource> context) throws CommandException {
        Duration uptime = Duration.ofMillis(ManagementFactory.getRuntimeMXBean().getUptime());

        List<Text> messages = Lists.newArrayList();

        messages.add(context.getMessage("command.serverstat.tps", getTPS(Sponge.getServer().getTicksPerSecond())));

        Optional<Instant> oi = context.getServiceCollection().platformService().gameStartedTime();
        oi.ifPresent(instant -> {
            Duration duration = Duration.between(instant, Instant.now());
            double averageTPS = Math.min(20, ((double) Sponge.getServer().getRunningTimeTicks() / ((double) (duration.toMillis() + 50) / 1000.0d)));
            messages.add(context.getMessage("command.serverstat.averagetps", getTPS(averageTPS)));
            messages.add(createText(context, "command.serverstat.uptime.main", "command.serverstat.uptime.hover",
                    context.getTimeString(duration.getSeconds())));
        });

        messages.add(createText(context, "command.serverstat.jvmuptime.main", "command.serverstat.jvmuptime.hover", context.getTimeString(uptime.getSeconds())));

        messages.add(Util.SPACE);

        long max = Runtime.getRuntime().maxMemory() / 1024 / 1024;
        long total = Runtime.getRuntime().totalMemory() / 1024 / 1024;
        long free = Runtime.getRuntime().freeMemory() / 1024 / 1024;

        messages.add(createText(context, "command.serverstat.maxmem.main", "command.serverstat.maxmem.hover", String.valueOf(max)));
        messages.add(createText(context, "command.serverstat.totalmem.main", "command.serverstat.totalmem.hover", String.valueOf(total)));

        long allocated = total - free;
        messages.add(createText(context, "command.serverstat.allocated.main", "command.serverstat.allocated.hover",
                String.valueOf(allocated), String.valueOf((allocated * 100)/total), String.valueOf((allocated * 100)/max)));
        messages.add(createText(context, "command.serverstat.freemem.main", "command.serverstat.freemem.hover", String.valueOf(free)));

        if (!context.hasAny("c")) {
            for (World world : Sponge.getServer().getWorlds()) {
                int numOfEntities = world.getEntities().size();
                int loadedChunks = Iterables.size(world.getLoadedChunks());
                messages.add(Util.SPACE);
                messages.add(context.getMessage("command.serverstat.world.title", world.getName()));

                // https://github.com/NucleusPowered/Nucleus/issues/888
                GeneratorType genType = world.getDimension().getGeneratorType();
                messages.add(context.getMessage(
                        "command.serverstat.world.info",
                        world.getDimension().getType().getName(),
                        genType == null ? context.getMessage("standard.unknown") : genType.getName(),
                        String.valueOf(numOfEntities),
                        String.valueOf(loadedChunks)));
            }
        }

        PaginationList.Builder plb = Util.getPaginationBuilder(context.getCommandSource())
                .title(context.getMessage("command.serverstat.title")).padding(Text.of("="))
                .contents(messages);

        plb.sendTo(context.getCommandSource());
        return context.successResult();
    }

    private Text getTPS(double currentTps) {
        TextColor colour;

        if (currentTps > 18) {
            colour = TextColors.GREEN;
        } else if (currentTps > 15) {
            colour = TextColors.YELLOW;
        } else {
            colour = TextColors.RED;
        }

        return Text.of(colour, TPS_FORMAT.format(currentTps));
    }

    @SuppressWarnings("RedundantCast")
    private Text createText(ICommandContext<? extends CommandSource> context, String mainKey, String hoverKey, String... subs) {
        Text.Builder tb = context.getMessage(mainKey, (Object[]) subs).toBuilder();
        return tb.onHover(TextActions.showText(context.getMessage(hoverKey))).build();
    }

}
