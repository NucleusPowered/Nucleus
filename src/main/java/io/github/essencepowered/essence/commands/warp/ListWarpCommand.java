/*
 * This file is part of Essence, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.essencepowered.essence.commands.warp;

import com.google.inject.Inject;
import io.github.essencepowered.essence.Util;
import io.github.essencepowered.essence.api.service.EssenceWarpService;
import io.github.essencepowered.essence.config.MainConfig;
import io.github.essencepowered.essence.internal.CommandBase;
import io.github.essencepowered.essence.internal.PermissionRegistry;
import io.github.essencepowered.essence.internal.annotations.Permissions;
import io.github.essencepowered.essence.internal.annotations.RegisterCommand;
import io.github.essencepowered.essence.internal.annotations.RunAsync;
import io.github.essencepowered.essence.internal.permissions.SuggestedLevel;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.service.pagination.PaginationService;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.action.TextActions;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.text.format.TextStyles;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Lists all the warps that a player can access.
 *
 * Command Usage: /warp list
 * Permission: quickstart.warp.list.base
 */
@Permissions(root = "warp", suggestedLevel = SuggestedLevel.USER)
@RunAsync
@RegisterCommand(value = { "list" }, subcommandOf = WarpCommand.class)
public class ListWarpCommand extends CommandBase<CommandSource> {
    private final EssenceWarpService service = Sponge.getServiceManager().provideUnchecked(EssenceWarpService.class);
    @Inject private MainConfig mainConfig;

    @Override
    public CommandSpec createSpec() {
        return CommandSpec.builder().executor(this)
                .description(Text.of("Lists the warps available to you.")).build();
    }

    @Override
    public String[] getAliases() {
        return new String[] { "list" };
    }

    @Override
    public CommandResult executeCommand(final CommandSource src, CommandContext args) throws Exception {
        PaginationService ps = Sponge.getServiceManager().provideUnchecked(PaginationService.class);

        // Get the warp list.
        Set<String> ws = service.getWarpNames();
        List<Text> lt = ws.stream()
                .filter(s -> canView(src, s.toLowerCase()))
                .map(s -> {
                    if (service.getWarp(s).isPresent()) {
                        return Text.builder(s).color(TextColors.GREEN)
                                .style(TextStyles.UNDERLINE).onClick(TextActions.runCommand("/warp " + s))
                                .onHover(TextActions.showText(Text.of(TextColors.YELLOW, Util.getMessageWithFormat("command.warps.warpprompt", s)))).build();
                    } else {
                        return Text.builder(s).color(TextColors.RED)
                                .onHover(TextActions.showText(Text.of(TextColors.YELLOW, Util.getMessageWithFormat("command.warps.unavailable")))).build();
                    }
                })
                .collect(Collectors.toList());

        ps.builder().title(Text.of(TextColors.YELLOW, Util.getMessageWithFormat("command.warps.list.header")))
                .paddingString("-").contents(lt).sendTo(src);
        return CommandResult.success();
    }

    private boolean canView(CommandSource src, String warp) {
        return !mainConfig.useSeparatePermissionsForWarp()
                || src.hasPermission(PermissionRegistry.PERMISSIONS_PREFIX + "warps." + warp.toLowerCase());
    }
}
