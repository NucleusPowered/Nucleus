/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.core.commands;

import io.github.nucleuspowered.nucleus.Nucleus;
import io.github.nucleuspowered.nucleus.Util;
import io.github.nucleuspowered.nucleus.internal.annotations.command.NoModifiers;
import io.github.nucleuspowered.nucleus.internal.annotations.command.Permissions;
import io.github.nucleuspowered.nucleus.internal.annotations.command.RegisterCommand;
import io.github.nucleuspowered.nucleus.internal.annotations.command.Scan;
import io.github.nucleuspowered.nucleus.internal.command.AbstractCommand;
import io.github.nucleuspowered.nucleus.internal.command.NucleusParameters;
import io.github.nucleuspowered.nucleus.internal.command.ReturnMessageException;
import io.github.nucleuspowered.nucleus.internal.messages.MessageProvider;
import io.github.nucleuspowered.nucleus.modules.core.services.UniqueUserService;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.action.TextActions;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.util.annotation.NonnullByDefault;

import java.util.Collection;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Scan
@NonnullByDefault
@Permissions(prefix = "nucleus")
@RegisterCommand(value = "debug", subcommandOf = NucleusCommand.class, hasExecutor = false)
public class DebugCommand extends AbstractCommand<CommandSource> {

    @Override protected CommandResult executeCommand(CommandSource src, CommandContext args, Cause cause) {
        return CommandResult.empty();
    }

    @NonnullByDefault
    @Permissions(prefix = "nucleus.debug")
    @RegisterCommand(value = "setsession", subcommandOf = DebugCommand.class)
    public static class SetSession extends AbstractCommand<CommandSource> {

        @Override public CommandElement[] getArguments() {
            return new CommandElement[] {
                    NucleusParameters.OPTIONAL_ONE_TRUE_FALSE
            };
        }

        @Override protected CommandResult executeCommand(CommandSource src, CommandContext args, Cause cause) {
            boolean set = args.<Boolean>getOne(NucleusParameters.Keys.BOOL).orElseGet(() -> !Nucleus.getNucleus().isSessionDebug());
            Nucleus.getNucleus().setSessionDebug(set);
            src.sendMessage(Nucleus.getNucleus().getMessageProvider().getTextMessageWithFormat("command.nucleus.debug.setsession", String.valueOf(set)));
            src.sendMessage(Nucleus.getNucleus().getMessageProvider().getTextMessageWithFormat("command.nucleus.debug.setsession2"));
            return CommandResult.success();
        }
    }

    @Permissions(prefix = "nucleus.debug")
    @NoModifiers
    @RegisterCommand(value = "getuuids", subcommandOf = DebugCommand.class)
    public static class GetUUIDSCommand extends AbstractCommand<CommandSource> {

        @Override public CommandElement[] getArguments() {
            return new CommandElement[] {
                    NucleusParameters.MANY_USER_NO_SELECTOR
            };
        }

        @Override protected CommandResult executeCommand(CommandSource src, CommandContext args, Cause cause) throws Exception {
            Collection<User> users = args.getAll(NucleusParameters.Keys.USER);
            if (users.isEmpty()) {
                throw ReturnMessageException.fromKey("command.nucleus.debug.uuid.none");
            }

            MessageProvider provider = Nucleus.getNucleus().getMessageProvider();
            Util.getPaginationBuilder(src)
                .title(provider.getTextMessageWithFormat("command.nucleus.debug.uuid.title", users.iterator().next().getName()))
                .header(provider.getTextMessageWithFormat("command.nucleus.debug.uuid.header"))
                .contents(
                    users.stream()
                        .map(
                            x -> Text.builder(x.getUniqueId().toString()).color(x.isOnline() ? TextColors.GREEN : TextColors.RED)
                                .onHover(TextActions.showText(Nucleus.getNucleus().getMessageProvider().getTextMessageWithFormat(
                                    "command.nucleus.debug.uuid.clicktodelete"
                                )))
                        .onClick(TextActions.runCommand("/nucleus resetuser -a " + x.getUniqueId().toString()))
                        .build()
                    ).collect(Collectors.toList())
                ).sendTo(src);
            return CommandResult.success();
        }
    }

    @Permissions(prefix = "nucleus.debug")
    @NoModifiers
    @RegisterCommand(value = "refreshuniquevisitors", subcommandOf = DebugCommand.class)
    public static class RefreshUniqueVisitors extends AbstractCommand<CommandSource> {

        @Override protected CommandResult executeCommand(CommandSource src, CommandContext args, Cause cause) throws CommandException {
            UniqueUserService uus = getServiceUnchecked(UniqueUserService.class);
            sendMessageTo(src, "command.nucleus.debug.refreshuniquevisitors.started", uus.getUniqueUserCount());

            Supplier<CommandSource> scs;
            if (src instanceof Player) {
                UUID u = ((Player) src).getUniqueId();
                scs = () -> Sponge.getServer().getPlayer(u).map(x -> (CommandSource) x).orElseGet(Sponge.getServer()::getConsole);
            } else {
                scs = Sponge.getServer()::getConsole;
            }

            uus.resetUniqueUserCount(l -> sendMessageTo(scs, "command.nucleus.debug.refreshuniquevisitors.done", String.valueOf(l)));
            return CommandResult.success();
        }
    }
}
