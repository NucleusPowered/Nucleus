/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.playerinfo.commands;

import io.github.nucleuspowered.nucleus.Util;
import io.github.nucleuspowered.nucleus.modules.playerinfo.PlayerInfoPermissions;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandContext;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandExecutor;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandResult;
import io.github.nucleuspowered.nucleus.scaffold.command.annotation.Command;
import io.github.nucleuspowered.nucleus.scaffold.command.parameter.RegexArgument;
import io.github.nucleuspowered.nucleus.services.INucleusServiceCollection;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.service.user.UserStorageService;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.action.TextActions;
import org.spongepowered.api.util.annotation.NonnullByDefault;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@NonnullByDefault
@Command(
        aliases = "getfromip",
        basePermission = PlayerInfoPermissions.BASE_GETFROMIP,
        commandDescriptionKey = "getfromip",
        async = true
)
public class GetFromIpCommand implements ICommandExecutor<CommandSource> {

    private final String ipKey = "IP Address";

    @Override
    public CommandElement[] parameters(INucleusServiceCollection serviceCollection) {
        return new CommandElement[] {
            new RegexArgument(Text.of(this.ipKey), "^(\\d{1,3}\\.){3}\\d{1,3}$", "command.getfromip.notvalid", serviceCollection)
        };
    }

    @Override public ICommandResult execute(ICommandContext<? extends CommandSource> context) throws CommandException {
        String ip = context.requireOne(this.ipKey, String.class);
        if (Arrays.stream(ip.split("\\.")).anyMatch(x -> Integer.parseInt(x) > 255)) {
            return context.errorResult("command.getfromip.notvalid");
        }

        UserStorageService uss = Sponge.getServiceManager().provideUnchecked(UserStorageService.class);
        List<User> users = context
                .getServiceCollection()
                .userCacheService()
                .getForIp(ip).stream().map(uss::get).filter(Optional::isPresent)
                .map(Optional::get).collect(Collectors.toList());

        if (users.isEmpty()) {
            context.sendMessage("command.getfromip.nousers");
            return context.successResult();
        }

        Util.getPaginationBuilder(context.getCommandSource())
                .title(context.getMessage("command.getfromip.title", ip))
                .contents(
                    users.stream().map(y -> {
                        Text n = context.getDisplayName(y.getUniqueId());
                        return n.toBuilder().onClick(TextActions.runCommand("/nucleus:seen " + y.getName()))
                            .onHover(TextActions.showText(context.getMessage("command.getfromip.hover", n)))
                            .build();
                    }).collect(Collectors.toList())
                )
                .sendTo(context.getCommandSource());
        return context.successResult();
    }
}
