/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.misc.commands;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import io.github.nucleuspowered.nucleus.Util;
import io.github.nucleuspowered.nucleus.internal.annotations.*;
import io.github.nucleuspowered.nucleus.internal.command.AbstractCommand;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.service.pagination.PaginationList;
import org.spongepowered.api.service.pagination.PaginationService;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.action.TextActions;
import org.spongepowered.api.text.format.TextColor;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.world.World;

import java.lang.management.ManagementFactory;
import java.text.DecimalFormat;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Permissions
@RunAsync
@NoCooldown
@NoCost
@NoWarmup
@RegisterCommand({"serverstat", "gc"})
public class ServerStatCommand extends AbstractCommand<CommandSource> {

    private static final DecimalFormat tpsFormat = new DecimalFormat("#0.00");
    private static final Text notQuiteEmpty = Text.of(" ");

    @Override
    public CommandResult executeCommand(CommandSource src, CommandContext args) throws Exception {

        Duration uptime = Duration.ofMillis(ManagementFactory.getRuntimeMXBean().getUptime());

        List<Text> messages = Lists.newArrayList();

        messages.add(plugin.getMessageProvider().getTextMessageWithTextFormat("command.serverstat.tps", getTPS(Sponge.getServer().getTicksPerSecond())));

        Optional<Instant> oi = plugin.getGameStartedTime();
        if (oi.isPresent()) {
            Duration duration = Duration.between(oi.get(), Instant.now());
            double averageTPS = Math.min(20, ((double)Sponge.getServer().getRunningTimeTicks() / ((double)(duration.toMillis() + 50)/1000.0d)));
            messages.add(plugin.getMessageProvider().getTextMessageWithTextFormat("command.serverstat.averagetps", getTPS(averageTPS)));
            messages.add(createText("command.serverstat.uptime.main", "command.serverstat.uptime.hover", Util.getTimeStringFromSeconds(duration.getSeconds())));
        }

        messages.add(createText("command.serverstat.jvmuptime.main", "command.serverstat.jvmuptime.hover", Util.getTimeStringFromSeconds(uptime.getSeconds())));

        messages.add(notQuiteEmpty);

        long max = Runtime.getRuntime().maxMemory() / 1024 / 1024;
        long total = Runtime.getRuntime().totalMemory() / 1024 / 1024;
        long free = Runtime.getRuntime().freeMemory() / 1024 / 1024;

        messages.add(createText("command.serverstat.maxmem.main", "command.serverstat.maxmem.hover", String.valueOf(max)));
        messages.add(createText("command.serverstat.totalmem.main", "command.serverstat.totalmem.hover", String.valueOf(total)));

        long allocated = total - free;
        messages.add(createText("command.serverstat.allocated.main", "command.serverstat.allocated.hover",
                String.valueOf(allocated), String.valueOf((allocated * 100)/total), String.valueOf((allocated * 100)/max)));
        messages.add(createText("command.serverstat.freemem.main", "command.serverstat.freemem.hover", String.valueOf(free)));

        for (World world : Sponge.getServer().getWorlds())
        {
            int numOfEntities = world.getEntities().size();
            int loadedChunks = Iterables.size(world.getLoadedChunks());
            messages.add(notQuiteEmpty);
            messages.add(plugin.getMessageProvider().getTextMessageWithFormat("command.serverstat.world.title", world.getName()));
            messages.add(plugin.getMessageProvider().getTextMessageWithFormat(
                    "command.serverstat.world.info",
                    world.getDimension().getType().getName(),
                    world.getDimension().getGeneratorType().getName(),
                    String.valueOf(numOfEntities),
                    String.valueOf(loadedChunks)));
        }

        PaginationList.Builder plb = Sponge.getServiceManager().provideUnchecked(PaginationService.class).builder()
                .title(plugin.getMessageProvider().getTextMessageWithFormat("command.serverstat.title")).padding(Text.of("="))
                .contents(messages);
        if (!(src instanceof Player)) {
            plb.linesPerPage(-1);
        }

        plb.sendTo(src);
        return CommandResult.success();
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

        return Text.of(colour, tpsFormat.format(currentTps));
    }

    private Text createText(String mainKey, String hoverKey, String... subs) {
        Text.Builder tb = plugin.getMessageProvider().getTextMessageWithFormat(mainKey, subs).toBuilder();
        return tb.onHover(TextActions.showText(plugin.getMessageProvider().getTextMessageWithFormat(hoverKey))).build();
    }

    private Text createText(String mainKey, String hoverKey, Text... subs) {
        Text.Builder tb = plugin.getMessageProvider().getTextMessageWithTextFormat(mainKey, subs).toBuilder();
        return tb.onHover(TextActions.showText(plugin.getMessageProvider().getTextMessageWithFormat(hoverKey))).build();
    }
}
