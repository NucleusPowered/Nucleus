/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.playerinfo.commands;

import io.github.nucleuspowered.nucleus.core.Util;
import io.github.nucleuspowered.nucleus.modules.playerinfo.PlayerInfoPermissions;
import io.github.nucleuspowered.nucleus.core.scaffold.command.ICommandContext;
import io.github.nucleuspowered.nucleus.core.scaffold.command.ICommandExecutor;
import io.github.nucleuspowered.nucleus.core.scaffold.command.ICommandResult;
import io.github.nucleuspowered.nucleus.core.scaffold.command.annotation.Command;
import io.github.nucleuspowered.nucleus.core.services.INucleusServiceCollection;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.exception.CommandException;
import org.spongepowered.api.command.parameter.Parameter;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.user.UserManager;

import java.net.InetAddress;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Command(
        aliases = "getfromip",
        basePermission = PlayerInfoPermissions.BASE_GETFROMIP,
        commandDescriptionKey = "getfromip")
public class GetFromIpCommand implements ICommandExecutor {

    private final Parameter.Value<InetAddress> ip = Parameter.ip().key("IP address").build();

    @Override
    public Parameter[] parameters(final INucleusServiceCollection serviceCollection) {
        return new Parameter[] {
            this.ip
        };
    }

    @Override public ICommandResult execute(final ICommandContext context) throws CommandException {
        final InetAddress ip = context.requireOne(this.ip);

        final UserManager uss = Sponge.server().userManager();
        final List<User> users = context
                .getServiceCollection()
                .userCacheService()
                .getForIp(ip.toString())
                .stream()
                .map(uss::load)
                .map(CompletableFuture::join)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());

        if (users.isEmpty()) {
            context.sendMessage("command.getfromip.nousers");
            return context.successResult();
        }

        Util.getPaginationBuilder(context.audience())
                .title(context.getMessage("command.getfromip.title", ip))
                .contents(
                    users.stream().map(y -> {
                        final Component name = context.getDisplayName(y.uniqueId());
                        return name
                                .clickEvent(ClickEvent.runCommand("/nucleus:seen " + y.name()))
                                .hoverEvent(HoverEvent.showText(context.getMessage("command.getfromip.hover", name)));
                    }).collect(Collectors.toList())
                )
                .sendTo(context.audience());
        return context.successResult();
    }
}
