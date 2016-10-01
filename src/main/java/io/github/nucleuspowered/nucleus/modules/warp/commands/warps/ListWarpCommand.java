/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.warp.commands.warps;

import com.google.inject.Inject;
import io.github.nucleuspowered.nucleus.api.data.WarpData;
import io.github.nucleuspowered.nucleus.api.service.NucleusWarpService;
import io.github.nucleuspowered.nucleus.internal.PermissionRegistry;
import io.github.nucleuspowered.nucleus.internal.annotations.Permissions;
import io.github.nucleuspowered.nucleus.internal.annotations.RegisterCommand;
import io.github.nucleuspowered.nucleus.internal.annotations.RunAsync;
import io.github.nucleuspowered.nucleus.internal.permissions.SuggestedLevel;
import io.github.nucleuspowered.nucleus.modules.warp.config.WarpConfigAdapter;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.service.pagination.PaginationList;
import org.spongepowered.api.service.pagination.PaginationService;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.action.TextActions;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.text.format.TextStyles;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Lists all the warps that a player can access.
 *
 * Command Usage: /warp list Permission: quickstart.warp.list.base
 */
@Permissions(root = "warp", suggestedLevel = SuggestedLevel.USER)
@RunAsync
@RegisterCommand(value = {"list"}, subcommandOf = WarpCommand.class)
public class ListWarpCommand extends io.github.nucleuspowered.nucleus.internal.command.AbstractCommand<CommandSource> {

    private NucleusWarpService service;
    @Inject private WarpConfigAdapter adapter;

    @Override
    public CommandResult executeCommand(final CommandSource src, CommandContext args) throws Exception {
        if (service == null) {
            service = Sponge.getServiceManager().provideUnchecked(NucleusWarpService.class);
        }

        PaginationService ps = Sponge.getServiceManager().provideUnchecked(PaginationService.class);

        // Get the warp list.
        Set<String> ws = service.getWarpNames();
        if (ws.isEmpty()) {
            src.sendMessage(plugin.getMessageProvider().getTextMessageWithFormat("command.warps.list.nowarps"));
            return CommandResult.empty();
        }

        final boolean econExists = plugin.getEconHelper().economyServiceExists();
        final int defaultCost = adapter.getNodeOrDefault().getDefaultWarpCost();
        List<Text> lt = ws.stream().filter(s -> canView(src, s.toLowerCase())).sorted(String::compareTo).map(s -> {
            Optional<WarpData> wd = service.getWarp(s);
            if (wd.isPresent()) {
                Text.Builder tb =
                        Text.builder().append(Text.builder(s).color(TextColors.GREEN).style(TextStyles.UNDERLINE).onClick(TextActions.runCommand("/warp " + s))
                        .onHover(TextActions.showText(plugin.getMessageProvider().getTextMessageWithFormat("command.warps.warpprompt", s))).build());

                if (econExists) {
                    int cost = wd.get().getCost().orElse(defaultCost);
                    if (cost > 0) {
                        tb.append(plugin.getMessageProvider().getTextMessageWithFormat("command.warps.list.cost", plugin.getEconHelper().getCurrencySymbol(cost)));
                    }
                }

                return tb.build();
            } else {
                return Text.builder(s).color(TextColors.RED).onHover(TextActions.showText(plugin.getMessageProvider().getTextMessageWithFormat("command.warps.unavailable")))
                        .build();
            }
        }).collect(Collectors.toList());

        PaginationList.Builder pb = ps.builder().title(plugin.getMessageProvider().getTextMessageWithFormat("command.warps.list.header")).padding(Text.of(TextColors.GREEN, "-")).contents(lt);
        if (!(src instanceof Player)) {
            pb.linesPerPage(-1);
        }

        pb.sendTo(src);
        return CommandResult.success();
    }

    private boolean canView(CommandSource src, String warp) {
        return !adapter.getNodeOrDefault().isSeparatePermissions()
                || src.hasPermission(PermissionRegistry.PERMISSIONS_PREFIX + "warps." + warp.toLowerCase());
    }
}
