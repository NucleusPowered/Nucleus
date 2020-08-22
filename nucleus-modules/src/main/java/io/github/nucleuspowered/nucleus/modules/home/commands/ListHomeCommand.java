/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.home.commands;

import io.github.nucleuspowered.nucleus.Util;
import io.github.nucleuspowered.nucleus.api.module.home.data.Home;
import io.github.nucleuspowered.nucleus.api.util.data.NamedLocation;
import io.github.nucleuspowered.nucleus.modules.home.HomePermissions;
import io.github.nucleuspowered.nucleus.modules.home.config.HomeConfig;
import io.github.nucleuspowered.nucleus.modules.home.services.HomeService;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandContext;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandExecutor;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandResult;
import io.github.nucleuspowered.nucleus.scaffold.command.annotation.Command;
import io.github.nucleuspowered.nucleus.services.INucleusServiceCollection;
import io.github.nucleuspowered.nucleus.services.interfaces.IMessageProviderService;
import io.github.nucleuspowered.nucleus.services.interfaces.IPermissionService;
import io.github.nucleuspowered.nucleus.services.interfaces.IReloadableService;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.service.pagination.PaginationList;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.action.TextActions;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.text.format.TextStyles;
import org.spongepowered.api.util.annotation.NonnullByDefault;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@NonnullByDefault
@Command(
        aliases = {"list", "#listhomes", "#homes"},
        basePermission = HomePermissions.BASE_HOME_LIST,
        commandDescriptionKey = "home.list",
        parentCommand = HomeCommand.class,
        async = true,
        associatedPermissions = HomePermissions.OTHERS_LIST_HOME
)
public class ListHomeCommand implements ICommandExecutor<CommandSource>, IReloadableService.Reloadable {

    private boolean isOnlySameDimension = false;

    @Override
    public CommandElement[] parameters(INucleusServiceCollection serviceCollection) {
        return new CommandElement[] {
                serviceCollection.commandElementSupplier().createOnlyOtherUserPermissionElement(false, HomePermissions.OTHERS_LIST_HOME)
        };
    }

    @Override public ICommandResult execute(ICommandContext<? extends CommandSource> context) throws CommandException {
        User user = context.getUserFromArgs();
        Text header;

        boolean other = !context.is(user);
        if (other && (context.isConsoleAndBypass() || context.testPermissionFor(user, HomePermissions.HOME_OTHER_EXEMPT_TARGET))) {
            return context.errorResult("command.listhome.exempt");
        }

        List<Home> msw = context.getServiceCollection().getServiceUnchecked(HomeService.class).getHomes(user);
        if (msw.isEmpty()) {
            return context.errorResult("command.home.nohomes");
        }

        final CommandSource source = context.getCommandSource();
        final IMessageProviderService messageProviderService = context.getServiceCollection().messageProvider();
        if (other) {
            header = messageProviderService.getMessageFor(source, "home.title.name", user.getName());
        } else {
            header = messageProviderService.getMessageFor(source, "home.title.normal");
        }

        IPermissionService permissionService = context.getServiceCollection().permissionService();
        List<Text> lt = msw.stream().sorted(Comparator.comparing(NamedLocation::getName)).map(x -> {
            Optional<Location<World>> olw = x.getLocation();
            if (!olw.isPresent()) {
                return Text.builder().append(
                                Text.builder(x.getName()).color(TextColors.RED)
                                        .onHover(TextActions.showText(
                                                messageProviderService.getMessageFor(source, "home.warphoverinvalid", x.getName())))
                                        .build())
                        .build();
            } else {
                final Location<World> lw = olw.get();
                final Text textMessage = messageProviderService.getMessageFor(source, "home.location",
                                                 lw.getExtent().getName(), lw.getBlockX(), lw.getBlockY(), lw.getBlockZ());

                if (this.isOnlySameDimension && source instanceof Player && !other) {
                    if (!lw.getExtent().getUniqueId().equals(((Player) source).getLocation().getExtent().getUniqueId())) {
                        if (!context.isConsoleAndBypass() && !permissionService.hasPermission(user, HomePermissions.HOME_EXEMPT_SAMEDIMENSION)) {
                            return Text.builder()
                                       .append(Text.builder(x.getName())
                                                   .color(TextColors.LIGHT_PURPLE)
                                                   .onHover(TextActions.showText(
                                                           messageProviderService.getMessageFor(source, "home.warphoverotherdimension", x.getName())))
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
                                    .onHover(TextActions.showText(messageProviderService.getMessageFor(source, "home.warphover", x.getName())))
                                    .onClick(TextActions.runCommand(other ? "/homeother " + user.getName() + " " + x.getName()
                                                                          : "/home " + x.getName()))
                                    .build())
                           .append(textMessage)
                           .build();
            }
        }).collect(Collectors.toList());

        PaginationList.Builder pb =
            Util.getPaginationBuilder(source).title(Text.of(TextColors.YELLOW, header)).padding(Text.of(TextColors.GREEN, "-")).contents(lt);

        pb.sendTo(source);
        return context.successResult();
    }

    @Override
    public void onReload(INucleusServiceCollection serviceCollection) {
        HomeConfig hc = serviceCollection.moduleDataProvider().getModuleConfig(HomeConfig.class);
        this.isOnlySameDimension = hc.isOnlySameDimension();
    }
}
