/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.home.commands;

import com.google.inject.Inject;
import io.github.nucleuspowered.nucleus.Nucleus;
import io.github.nucleuspowered.nucleus.Util;
import io.github.nucleuspowered.nucleus.api.data.Home;
import io.github.nucleuspowered.nucleus.api.data.NamedLocation;
import io.github.nucleuspowered.nucleus.argumentparsers.NicknameArgument;
import io.github.nucleuspowered.nucleus.internal.annotations.NoCooldown;
import io.github.nucleuspowered.nucleus.internal.annotations.NoCost;
import io.github.nucleuspowered.nucleus.internal.annotations.NoWarmup;
import io.github.nucleuspowered.nucleus.internal.annotations.Permissions;
import io.github.nucleuspowered.nucleus.internal.annotations.RegisterCommand;
import io.github.nucleuspowered.nucleus.internal.annotations.RunAsync;
import io.github.nucleuspowered.nucleus.internal.command.AbstractCommand;
import io.github.nucleuspowered.nucleus.internal.command.ReturnMessageException;
import io.github.nucleuspowered.nucleus.internal.permissions.PermissionInformation;
import io.github.nucleuspowered.nucleus.internal.permissions.SuggestedLevel;
import io.github.nucleuspowered.nucleus.modules.home.handlers.HomeHandler;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.service.pagination.PaginationList;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.action.TextActions;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.text.format.TextStyles;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Permissions(prefix = "home", mainOverride = "list", suggestedLevel = SuggestedLevel.USER)
@RunAsync
@NoCooldown
@NoWarmup
@NoCost
@RegisterCommand({"listhomes", "homes"})
public class ListHomeCommand extends AbstractCommand<CommandSource> {

    private final String player = "player";
    private final String exempt = Nucleus.getNucleus().getPermissionRegistry().getService(HomeOtherCommand.class)
        .getPermissionWithSuffix(HomeOtherCommand.OTHER_EXEMPT_PERM_SUFFIX);

    @Inject private HomeHandler homeHandler;

    @Override
    public Map<String, PermissionInformation> permissionSuffixesToRegister() {
        Map<String, PermissionInformation> m = new HashMap<>();
        m.put("others", new PermissionInformation(plugin.getMessageProvider().getMessageWithFormat("permission.others"), SuggestedLevel.ADMIN));
        return m;
    }

    @Override
    public CommandElement[] getArguments() {
        return new CommandElement[] {GenericArguments.optional(GenericArguments.onlyOne(
                GenericArguments.requiringPermission(
                        new NicknameArgument(Text.of(player), plugin.getUserDataManager(), NicknameArgument.UnderlyingType.USER),
                        permissions.getPermissionWithSuffix("others"))))};
    }

    @Override
    public CommandResult executeCommand(CommandSource src, CommandContext args) throws Exception {
        User user = this.getUserFromArgs(User.class, src, player, args); // args.getOne(player);
        Text header;

        boolean other = src instanceof User && !((User) src).getUniqueId().equals(user.getUniqueId());
        if (other && user.hasPermission(this.exempt)) {
            throw new ReturnMessageException(plugin.getMessageProvider().getTextMessageWithFormat("command.listhome.exempt"));
        }

        List<Home> msw = homeHandler.getHomes(user);
        if (msw.isEmpty()) {
            src.sendMessage(plugin.getMessageProvider().getTextMessageWithFormat("command.home.nohomes"));
            return CommandResult.empty();
        }

        if (other) {
            header = plugin.getMessageProvider().getTextMessageWithFormat("home.title.name", user.getName());
        } else {
            header = plugin.getMessageProvider().getTextMessageWithFormat("home.title.normal");
        }

        List<Text> lt = msw.stream().sorted(Comparator.comparing(NamedLocation::getName)).map(x -> {
            Optional<Location<World>> olw = x.getLocation();
            if (!olw.isPresent()) {
                return Text.builder().append(
                                Text.builder(x.getName()).color(TextColors.RED)
                                        .onHover(TextActions.showText(plugin.getMessageProvider().getTextMessageWithFormat("home.warphoverinvalid", x.getName())))
                                        .build())
                        .build();
            } else {
                final Location<World> lw = olw.get();
                return Text.builder().append(
                                Text.builder(x.getName()).color(TextColors.GREEN).style(TextStyles.UNDERLINE)
                                        .onHover(TextActions.showText(plugin.getMessageProvider().getTextMessageWithFormat("home.warphover", x.getName())))
                                        .onClick(TextActions.runCommand(other ? "/homeother " + user.getName() + " " + x.getName()
                                                : "/home " + x.getName()))
                                        .build())
                        .append(plugin.getMessageProvider().getTextMessageWithFormat("home.location", lw.getExtent().getName(), String.valueOf(lw.getBlockX()),
                                String.valueOf(lw.getBlockY()), String.valueOf(lw.getBlockZ())))
                        .build();
            }
        }).collect(Collectors.toList());

        PaginationList.Builder pb =
            Util.getPaginationBuilder(src).title(Text.of(TextColors.YELLOW, header)).padding(Text.of(TextColors.GREEN, "-")).contents(lt);

        pb.sendTo(src);
        return CommandResult.success();
    }
}
