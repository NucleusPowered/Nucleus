/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.core.commands;

import com.google.common.collect.Lists;
import io.github.nucleuspowered.nucleus.Util;
import io.github.nucleuspowered.nucleus.modules.core.CorePermissions;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandContext;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandExecutor;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandResult;
import io.github.nucleuspowered.nucleus.scaffold.command.annotation.Command;
import io.github.nucleuspowered.nucleus.scaffold.command.control.CommandControl;
import io.github.nucleuspowered.nucleus.services.INucleusServiceCollection;
import io.github.nucleuspowered.nucleus.services.interfaces.IMessageProviderService;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandCallable;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandMapping;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.ArgumentParseException;
import org.spongepowered.api.command.args.ChildCommandElementExecutor;
import org.spongepowered.api.command.args.CommandArgs;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.Text;

import java.lang.reflect.Field;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

@Command(
        aliases = "commandinfo",
        basePermission = CorePermissions.BASE_COMMANDINFO,
        commandDescriptionKey = "commandinfo",
        hasHelpCommand = false
)
public class CommandInfoCommand implements ICommandExecutor<CommandSource> {

    private final String commandKey = "command";

    @Override
    public CommandElement[] parameters(INucleusServiceCollection serviceCollection) {
        return new CommandElement[] {
                new CommandChoicesArgument(serviceCollection.messageProvider())
        };
    }

    @Override
    public ICommandResult execute(ICommandContext<? extends CommandSource> context) throws CommandException {
        // we have the command, get the mapping
        CommandMapping mapping = context.requireOne(this.commandKey, CommandMapping.class);

        CommandSource source = context.getCommandSourceUnchecked();
        IMessageProviderService provider = context.getServiceCollection().messageProvider();
        Text header = provider.getMessageFor(source, "command.commandinfo.title", mapping.getPrimaryAlias());

        List<Text> content = Lists.newArrayList();

        // Owner
        content.add(provider.getMessageFor(source, "command.commandinfo.owner", Sponge.getCommandManager().getOwner(mapping)
                .map(x -> x.getName() + " (" + x.getId() + ")")
                .orElseGet(() -> provider.getMessageString(source, "standard.unknown"))));
        content.add(provider.getMessageFor(source,"command.commandinfo.aliases", String.join(", ", mapping.getAllAliases())));

        if (mapping.getCallable() instanceof CommandControl) {
            nucleusCommand(content, context, provider, (CommandControl) mapping.getCallable());
        } else if (mapping.getCallable() instanceof CommandSpec) {
            specCommand(content, context, provider, mapping.getPrimaryAlias(), (CommandSpec) mapping.getCallable());
        } else {
            lowCommand(content, context, provider, mapping.getPrimaryAlias(), mapping.getCallable());
        }

        Util.getPaginationBuilder(context.is(Player.class))
                .title(header)
                .contents(content)
                .sendTo(context.getCommandSource());
        return context.successResult();
    }

    private void nucleusCommand(
            List<Text> content,
            ICommandContext<? extends CommandSource> context,
            IMessageProviderService provider,
            CommandControl abstractCommand) throws CommandException{
        CommandSource source = context.getCommandSource();
        content.add(provider.getMessageFor(source, "command.commandinfo.type", "loc:command.commandinfo.nucleus"));
        content.add(Text.EMPTY);
        Text text = abstractCommand.getUsageText(source);
        if (text.isEmpty()) {
            content.add(provider.getMessageFor(source, "command.commandinfo.noinfo"));
        } else {
            content.add(text);
        }
    }

    private void specCommand(List<Text> content,
            ICommandContext<? extends CommandSource> context,
            IMessageProviderService provider,
            String alias,
            CommandSpec spec) throws CommandException{ //List<Text> content, CommandSource source, MessageProvider provider, String
                                                                     // alias, CommandSpec spec) {
        CommandSource src = context.getCommandSource();
        content.add(provider.getMessageFor(src, "command.commandinfo.type", "loc:command.commandinfo.spec"));
        CommandExecutor executor = spec.getExecutor();
        if (executor instanceof ChildCommandElementExecutor) {
            try {
                content.add(provider.getMessageFor(src, "command.commandinfo.haschildren"));
                Field field = ChildCommandElementExecutor.class.getDeclaredField("fallbackExecutor");
                field.setAccessible(true);
                content.add(provider.getMessageFor(src, "command.commandinfo.execclass", field.get(executor).getClass().getName()));
            } catch (NoSuchFieldException | IllegalAccessException e) {
                content.add(provider.getMessageFor(src, "command.commandinfo.execclass", "loc:standard.unknown"));
            }
        }

        content.add(Text.EMPTY);
        content.add(provider.getMessageFor(src, "command.commandinfo.description"));

        spec.getShortDescription(src).ifPresent(x -> {
            content.add(provider.getMessageFor(src, "command.commandinfo.shortdescription"));
            content.add(x);
            content.add(Text.EMPTY);
        });
        spec.getExtendedDescription(src).ifPresent(x -> {
            content.add(provider.getMessageFor(src, "command.commandinfo.description"));
            content.add(x);
            content.add(Text.EMPTY);
        });

        content.add(provider.getMessageFor(src, "command.commandinfo.usage"));
        content.add(Text.of("/", alias, " ", spec.getUsage(src)));
    }

    private void lowCommand(
            List<Text> content,
            ICommandContext<? extends CommandSource> context,
            IMessageProviderService provider,
            String alias,
            CommandCallable callable) throws CommandException {
        CommandSource src = context.getCommandSource();
        content.add(provider.getMessageFor(src, "command.commandinfo.type", "loc:command.commandinfo.callable"));
        content.add(Text.EMPTY);

        callable.getShortDescription(src).ifPresent(x -> {
            content.add(provider.getMessageFor(src, "command.commandinfo.shortdescription"));
            content.add(x);
            content.add(Text.EMPTY);
        });
        callable.getHelp(src).ifPresent(x -> {
            content.add(provider.getMessageFor(src, "command.commandinfo.description"));
            content.add(x);
            content.add(Text.EMPTY);
        });

        content.add(provider.getMessageFor(src, "command.commandinfo.usage"));
        content.add(Text.of("/", alias, " ", callable.getUsage(src)));
    }

    private class CommandChoicesArgument extends CommandElement {

        private final IMessageProviderService messageProviderService;

        CommandChoicesArgument(IMessageProviderService messageProviderService) {
            super(Text.of(CommandInfoCommand.this.commandKey));
            this.messageProviderService = messageProviderService;
        }

        @Nullable
        @Override
        protected Object parseValue(@NonNull CommandSource source, CommandArgs args) throws ArgumentParseException {
            String next = args.next();
            return Sponge.getCommandManager().get(next).orElseThrow(() -> args.createError(
                    this.messageProviderService.getMessageFor(source, "command.commandinfo.nocommand", next)
            ));
        }

        @Override
        @NonNull
        public List<String> complete(@NonNull CommandSource src, @NonNull CommandArgs args, @NonNull CommandContext context) {
            try {
                final String s = args.peek().toLowerCase();
                return Sponge.getCommandManager().getAliases().stream().filter(x -> x.toLowerCase().startsWith(s)).collect(Collectors.toList());
            } catch (ArgumentParseException e) {
                return Lists.newArrayList(Sponge.getCommandManager().getAliases());
            }
        }
    }
}
