/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.misc.commands;

import com.google.common.collect.Iterables;
import io.github.nucleuspowered.nucleus.core.Util;
import io.github.nucleuspowered.nucleus.modules.misc.MiscPermissions;
import io.github.nucleuspowered.nucleus.core.scaffold.command.ICommandContext;
import io.github.nucleuspowered.nucleus.core.scaffold.command.ICommandExecutor;
import io.github.nucleuspowered.nucleus.core.scaffold.command.ICommandResult;
import io.github.nucleuspowered.nucleus.core.scaffold.command.annotation.Command;
import io.github.nucleuspowered.nucleus.core.scaffold.command.annotation.EssentialsEquivalent;
import io.github.nucleuspowered.nucleus.core.services.INucleusServiceCollection;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.exception.CommandException;
import org.spongepowered.api.command.parameter.managed.Flag;
import org.spongepowered.api.service.pagination.PaginationList;
import org.spongepowered.api.world.server.ServerWorld;

import java.lang.management.ManagementFactory;
import java.text.DecimalFormat;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Command(aliases = { "serverstat", "uptime" }, basePermission = MiscPermissions.BASE_SERVERSTAT, commandDescriptionKey = "serverstat")
@EssentialsEquivalent(value = {"gc", "lag", "mem", "memory", "uptime", "tps", "entities"})
public class ServerStatCommand implements ICommandExecutor {

    private static final DecimalFormat TPS_FORMAT = new DecimalFormat("#0.00");

    @Override
    public Flag[] flags(final INucleusServiceCollection serviceCollection) {
        return new Flag[] {
                Flag.of("c", "s", "compact", "summary")
        };
    }

    @Override
    public ICommandResult execute(final ICommandContext context) throws CommandException {
        final Duration uptime = Duration.ofMillis(ManagementFactory.getRuntimeMXBean().getUptime());

        final List<Component> messages = new ArrayList<>();

        messages.add(context.getMessage("command.serverstat.tps", this.getTPS(Sponge.server().ticksPerSecond())));

        final Optional<Instant> oi = context.getServiceCollection().platformService().gameStartedTime();
        oi.ifPresent(instant -> {
            final Duration duration = Duration.between(instant, Instant.now());
            final double averageTPS = Sponge.server().ticksPerSecond();
            messages.add(context.getMessage("command.serverstat.averagetps", this.getTPS(averageTPS)));
            messages.add(this.createText(context, "command.serverstat.uptime.main", "command.serverstat.uptime.hover",
                    context.getTimeString(duration.getSeconds())));
        });

        messages.add(this.createText(context, "command.serverstat.jvmuptime.main", "command.serverstat.jvmuptime.hover", context.getTimeString(uptime.getSeconds())));

        messages.add(Component.empty());

        final long max = Runtime.getRuntime().maxMemory() / 1024 / 1024;
        final long total = Runtime.getRuntime().totalMemory() / 1024 / 1024;
        final long free = Runtime.getRuntime().freeMemory() / 1024 / 1024;

        messages.add(this.createText(context, "command.serverstat.maxmem.main", "command.serverstat.maxmem.hover", String.valueOf(max)));
        messages.add(this.createText(context, "command.serverstat.totalmem.main", "command.serverstat.totalmem.hover", String.valueOf(total)));

        final long allocated = total - free;
        messages.add(this.createText(context, "command.serverstat.allocated.main", "command.serverstat.allocated.hover",
                String.valueOf(allocated), String.valueOf((allocated * 100)/total), String.valueOf((allocated * 100)/max)));
        messages.add(this.createText(context, "command.serverstat.freemem.main", "command.serverstat.freemem.hover", String.valueOf(free)));

        if (!context.hasFlag("c")) {
            for (final ServerWorld world : Sponge.server().worldManager().worlds()) {
                final int numOfEntities = world.entities().size();
                final int loadedChunks = Iterables.size(world.loadedChunks());
                messages.add(Component.empty());
                messages.add(context.getMessage("command.serverstat.world.title", world.key().asString()));

                // https://github.com/NucleusPowered/Nucleus/issues/888
                messages.add(context.getMessage(
                        "command.serverstat.world.info",
                        "-", // world.worldType().getKey().asString(),
                        "-",// world.getProperties().getGeneratorModifierType().getKey().asString(),
                        String.valueOf(numOfEntities),
                        String.valueOf(loadedChunks)));
            }
        }

        final PaginationList.Builder plb = Util.getPaginationBuilder(context.audience())
                .title(context.getMessage("command.serverstat.title")).padding(Component.text("="))
                .contents(messages);

        plb.sendTo(context.audience());
        return context.successResult();
    }

    private Component getTPS(final double currentTps) {
        final TextColor colour;

        if (currentTps > 18) {
            colour = NamedTextColor.GREEN;
        } else if (currentTps > 15) {
            colour = NamedTextColor.YELLOW;
        } else {
            colour = NamedTextColor.RED;
        }

        return Component.text(ServerStatCommand.TPS_FORMAT.format(currentTps), colour);
    }

    private Component createText(final ICommandContext context, final String mainKey, final String hoverKey, final String... subs) {
        final Component tb = context.getMessage(mainKey, (Object[]) subs);
        return tb.hoverEvent(HoverEvent.showText(context.getMessage(hoverKey)));
    }

}
