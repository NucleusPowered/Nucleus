/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.jail.commands;

import com.google.inject.Inject;
import io.github.nucleuspowered.nucleus.Util;
import io.github.nucleuspowered.nucleus.modules.jail.JailPermissions;
import io.github.nucleuspowered.nucleus.modules.jail.data.JailData;
import io.github.nucleuspowered.nucleus.modules.jail.services.JailHandler;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandContext;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandExecutor;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandResult;
import io.github.nucleuspowered.nucleus.scaffold.command.annotation.Command;
import io.github.nucleuspowered.nucleus.scaffold.command.parameter.UUIDArgument;
import io.github.nucleuspowered.nucleus.services.INucleusServiceCollection;
import io.github.nucleuspowered.nucleus.services.interfaces.IMessageProviderService;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.service.user.UserStorageService;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.util.annotation.NonnullByDefault;

import java.time.Instant;
import java.util.Optional;

@NonnullByDefault
@Command(
        aliases = "checkjail",
        basePermission = JailPermissions.BASE_CHECKJAIL,
        commandDescriptionKey = "checkjail",
        async = true
)
public class CheckJailCommand implements ICommandExecutor<CommandSource> {

    private final String playerKey = "user/UUID";
    private final JailHandler handler;

    @Inject
    public CheckJailCommand(INucleusServiceCollection serviceCollection) {
        this.handler = serviceCollection.getServiceUnchecked(JailHandler.class);
    }

    @Override
    public CommandElement[] parameters(INucleusServiceCollection serviceCollection) {
        return new CommandElement[] {
            GenericArguments.firstParsing(
                GenericArguments.user(Text.of(this.playerKey)),
                    new UUIDArgument<>(Text.of(this.playerKey), u -> Sponge.getServiceManager().provideUnchecked(UserStorageService.class).get(u), serviceCollection)
            )
        };
    }

    @Override public ICommandResult execute(ICommandContext<? extends CommandSource> context) throws CommandException {
        User user = context.requireOne(this.playerKey, User.class);
        Optional<JailData> jail = this.handler.getPlayerJailDataInternal(user);

        if (!jail.isPresent()) {
            return context.errorResult("command.checkjail.nojail", user.getName());
        }

        IMessageProviderService messageProviderService = context.getServiceCollection().messageProvider();
        JailData md = jail.get();
        String name;
        if (md.getJailerInternal().equals(Util.CONSOLE_FAKE_UUID)) {
            name = Sponge.getServer().getConsole().getName();
        } else {
            name = Sponge.getServiceManager().provideUnchecked(UserStorageService.class)
                    .get(md.getJailerInternal())
                    .map(User::getName).orElseGet(() -> context.getMessageString("standard.unknown"));
        }

        if (md.getRemainingTime().isPresent()) {
            context.sendMessage("command.checkjail.jailedfor", user.getName(), md.getJailName(),
                    name, messageProviderService.getTimeString(
                            context.getCommandSource().getLocale(),
                            md.getRemainingTime().get().getSeconds()));
        } else {
            context.sendMessage("command.checkjail.jailedperm", user.getName(), md.getJailName(), name);
        }

        if (md.getCreationTime() > 0) {
            context.sendMessage("command.checkjail.created",
                    Util.FULL_TIME_FORMATTER.withLocale(context.getCommandSource().getLocale()).format(Instant.ofEpochSecond(md.getCreationTime())));
        } else {
            context.sendMessage("command.checkjail.created", "loc:standard.unknown");
        }

        context.sendMessage("standard.reasoncoloured", md.getReason());
        return context.successResult();
    }
}
