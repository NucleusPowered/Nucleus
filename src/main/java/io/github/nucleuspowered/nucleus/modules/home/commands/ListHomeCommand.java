/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.home.commands;

import io.github.nucleuspowered.nucleus.Nucleus;
import io.github.nucleuspowered.nucleus.Util;
import io.github.nucleuspowered.nucleus.api.nucleusdata.Home;
import io.github.nucleuspowered.nucleus.api.nucleusdata.NamedLocation;
import io.github.nucleuspowered.nucleus.internal.annotations.RunAsync;
import io.github.nucleuspowered.nucleus.internal.annotations.command.NoModifiers;
import io.github.nucleuspowered.nucleus.internal.annotations.command.Permissions;
import io.github.nucleuspowered.nucleus.internal.annotations.command.RegisterCommand;
import io.github.nucleuspowered.nucleus.internal.command.AbstractCommand;
import io.github.nucleuspowered.nucleus.internal.command.NucleusParameters;
import io.github.nucleuspowered.nucleus.internal.command.ReturnMessageException;
import io.github.nucleuspowered.nucleus.internal.interfaces.Reloadable;
import io.github.nucleuspowered.nucleus.internal.permissions.PermissionInformation;
import io.github.nucleuspowered.nucleus.internal.permissions.SuggestedLevel;
import io.github.nucleuspowered.nucleus.modules.home.config.HomeConfig;
import io.github.nucleuspowered.nucleus.modules.home.config.HomeConfigAdapter;
import io.github.nucleuspowered.nucleus.modules.home.services.HomeService;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.service.pagination.PaginationList;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.action.TextActions;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.text.format.TextStyles;
import org.spongepowered.api.util.annotation.NonnullByDefault;
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
@NoModifiers
@NonnullByDefault
@RegisterCommand(value = "list", subcommandOf = HomeCommand.class, rootAliasRegister = {"listhomes", "homes"})
public class ListHomeCommand extends AbstractCommand<CommandSource> implements Reloadable {

    private final String exemptOther = Nucleus.getNucleus().getPermissionRegistry().getPermissionsForNucleusCommand(HomeOtherCommand.class)
                                              .getPermissionWithSuffix(HomeOtherCommand.OTHER_EXEMPT_PERM_SUFFIX);
    private final String exemptSameDimension = Nucleus.getNucleus().getPermissionRegistry().getPermissionsForNucleusCommand(HomeCommand.class)
                                                      .getPermissionWithSuffix(HomeCommand.EXEMPT_SAMEDIMENSION_SUFFIX);

    private final HomeService homeService = getServiceUnchecked(HomeService.class);

    private boolean isOnlySameDimension = false;

    @Override
    public Map<String, PermissionInformation> permissionSuffixesToRegister() {
        Map<String, PermissionInformation> m = new HashMap<>();
        m.put("others", PermissionInformation.getWithTranslation("permission.others", SuggestedLevel.ADMIN));
        // m.put("exempt.samedimension", PermissionInformation.getWithTranslation("permission.home.exempt.samedimension", SuggestedLevel.ADMIN));
        return m;
    }

    @Override
    public CommandElement[] getArguments() {
        return new CommandElement[] {
                GenericArguments.optional(requirePermissionArg(
                        NucleusParameters.ONE_USER, this.permissions.getPermissionWithSuffix("others")))
        };
    }

    @Override
    public CommandResult executeCommand(CommandSource src, CommandContext args, Cause cause) throws Exception {
        User user = this.getUserFromArgs(User.class, src, NucleusParameters.Keys.USER, args);
        Text header;

        boolean other = src instanceof User && !((User) src).getUniqueId().equals(user.getUniqueId());
        if (other && hasPermission(user, this.exemptOther)) {
            throw new ReturnMessageException(Nucleus.getNucleus().getMessageProvider().getTextMessageWithFormat("command.listhome.exempt"));
        }

        List<Home> msw = this.homeService.getHomes(user);
        if (msw.isEmpty()) {
            src.sendMessage(Nucleus.getNucleus().getMessageProvider().getTextMessageWithFormat("command.home.nohomes"));
            return CommandResult.empty();
        }

        if (other) {
            header = Nucleus.getNucleus().getMessageProvider().getTextMessageWithFormat("home.title.name", user.getName());
        } else {
            header = Nucleus.getNucleus().getMessageProvider().getTextMessageWithFormat("home.title.normal");
        }

        List<Text> lt = msw.stream().sorted(Comparator.comparing(NamedLocation::getName)).map(x -> {
            Optional<Location<World>> olw = x.getLocation();
            if (!olw.isPresent()) {
                return Text.builder().append(
                                Text.builder(x.getName()).color(TextColors.RED)
                                        .onHover(TextActions.showText(
                                                Nucleus.getNucleus().getMessageProvider().getTextMessageWithFormat("home.warphoverinvalid", x.getName())))
                                        .build())
                        .build();
            } else {
                final Location<World> lw = olw.get();
                final Text textMessage = Nucleus.getNucleus().getMessageProvider().getTextMessageWithFormat("home.location",
                                                 lw.getExtent().getName(), String.valueOf(lw.getBlockX()), String.valueOf(lw.getBlockY()), String.valueOf(lw.getBlockZ()));

                if (this.isOnlySameDimension && src instanceof Player && !other) {
                    if (!lw.getExtent().getUniqueId().equals(((Player) src).getLocation().getExtent().getUniqueId())) {
                        if (!hasPermission(user, this.exemptSameDimension)) {
                            return Text.builder()
                                       .append(Text.builder(x.getName())
                                                   .color(TextColors.LIGHT_PURPLE)
                                                   .onHover(TextActions.showText(
                                                           Nucleus.getNucleus().getMessageProvider().getTextMessageWithFormat("home.warphoverotherdimension", x.getName())))
                                                   .build())
                                       .append(textMessage)
                                       .build();
                        }
                    }
                }

                return Text.builder()
                           .append(
                                Text.builder(x.getName())
                                    .color(TextColors.GREEN).style(TextStyles.UNDERLINE)
                                    .onHover(TextActions.showText(
                                            Nucleus.getNucleus().getMessageProvider().getTextMessageWithFormat("home.warphover", x.getName())))
                                    .onClick(TextActions.runCommand(other ? "/homeother " + user.getName() + " " + x.getName()
                                                                          : "/home " + x.getName()))
                                    .build())
                           .append(textMessage)
                           .build();
            }
        }).collect(Collectors.toList());

        PaginationList.Builder pb =
            Util.getPaginationBuilder(src).title(Text.of(TextColors.YELLOW, header)).padding(Text.of(TextColors.GREEN, "-")).contents(lt);

        pb.sendTo(src);
        return CommandResult.success();
    }

    @Override
    public void onReload() {
        HomeConfig hc = Nucleus.getNucleus().getInternalServiceManager().getServiceUnchecked(HomeConfigAdapter.class).getNodeOrDefault();
        this.isOnlySameDimension = hc.isOnlySameDimension();
    }
}
