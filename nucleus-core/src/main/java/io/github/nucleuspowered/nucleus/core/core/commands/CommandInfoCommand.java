/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.core.core.commands;

import com.google.inject.Inject;
import io.github.nucleuspowered.nucleus.core.Util;
import io.github.nucleuspowered.nucleus.core.core.CorePermissions;
import io.github.nucleuspowered.nucleus.core.scaffold.command.ICommandContext;
import io.github.nucleuspowered.nucleus.core.scaffold.command.ICommandExecutor;
import io.github.nucleuspowered.nucleus.core.scaffold.command.ICommandResult;
import io.github.nucleuspowered.nucleus.core.scaffold.command.annotation.Command;
import io.github.nucleuspowered.nucleus.core.scaffold.command.control.CommandControl;
import io.github.nucleuspowered.nucleus.core.services.INucleusServiceCollection;
import io.github.nucleuspowered.nucleus.core.services.interfaces.IMessageProviderService;
import io.github.nucleuspowered.nucleus.core.util.AdventureUtils;
import net.kyori.adventure.text.Component;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandCompletion;
import org.spongepowered.api.command.exception.ArgumentParseException;
import org.spongepowered.api.command.exception.CommandException;
import org.spongepowered.api.command.manager.CommandMapping;
import org.spongepowered.api.command.parameter.ArgumentReader;
import org.spongepowered.api.command.parameter.CommandContext;
import org.spongepowered.api.command.parameter.Parameter;
import org.spongepowered.api.command.parameter.managed.ValueParameter;
import org.spongepowered.plugin.PluginContainer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Command(
        aliases = "commandinfo",
        basePermission = CorePermissions.BASE_COMMANDINFO,
        commandDescriptionKey = "commandinfo",
        hasHelpCommand = false
)
public class CommandInfoCommand implements ICommandExecutor {

    private final Parameter.Value<CommandMapping> commandParameter;

    @Inject
    public CommandInfoCommand(final IMessageProviderService messageProviderService) {
        this.commandParameter = Parameter.builder(CommandMapping.class)
                .key("command")
                .addParser(new CommandChoicesArgument(messageProviderService))
                .build();
    }

    @Override
    public Parameter[] parameters(final INucleusServiceCollection serviceCollection) {
        return new Parameter[] {
                this.commandParameter
        };
    }

    @Override
    public ICommandResult execute(final ICommandContext context) throws CommandException {
        // we have the command, get the mapping
        final CommandMapping mapping = context.requireOne(this.commandParameter);

        final IMessageProviderService provider = context.getServiceCollection().messageProvider();
        final Component header = context.getMessage("command.commandinfo.title", mapping.primaryAlias());

        final List<Component> content = new ArrayList<>();

        // Owner
        final Optional<PluginContainer> owner = mapping.plugin();
        content.add(provider.getMessage("command.commandinfo.owner", owner.flatMap(x -> x.metadata().name())
                .orElseGet(() -> context.getMessageString("standard.unknown")) + " (" + owner.map(x -> x.metadata().id()).orElse("unknown") + ")"));
        content.add(context.getMessage("command.commandinfo.aliases", String.join(", ", mapping.allAliases())));

        if (owner.map(x -> x.equals(context.getServiceCollection().pluginContainer())).orElse(false)) {
            // we did it, do we have a control for it?
            this.nucleusCommand(content, context, provider, mapping);
        } else {
            this.lowCommand(content, context, provider, mapping);
        }

        Util.getPaginationBuilder(context.audience())
                .title(header)
                .contents(content)
                .sendTo(context.audience());
        return context.successResult();
    }

    private void nucleusCommand(
            final List<Component> content,
            final ICommandContext context,
            final IMessageProviderService provider,
            final CommandMapping mapping) throws CommandException {
        content.add(context.getMessage("command.commandinfo.type", "loc:command.commandinfo.nucleus"));
        content.add(Component.empty());
        final CommandControl control = context.getServiceCollection().commandMetadataService().getControl(mapping.primaryAlias());
        final Component text = control.getUsage(context);
        if (AdventureUtils.isEmpty(text)) {
            content.add(context.getMessage("command.commandinfo.noinfo"));
        } else {
            content.add(text);
        }
    }

    private void lowCommand(
            final List<Component> content,
            final ICommandContext context,
            final IMessageProviderService provider,
            final CommandMapping mapping) throws CommandException {
        content.add(context.getMessage("command.commandinfo.type", "loc:command.commandinfo.callable"));
        content.add(Component.empty());
        final Optional<Component> help = mapping.registrar().help(context.cause(), mapping);
        help.ifPresent(content::add);
    }

    private static class CommandChoicesArgument implements ValueParameter<CommandMapping> {

        private final IMessageProviderService messageProviderService;

        CommandChoicesArgument(final IMessageProviderService messageProviderService) {
            this.messageProviderService = messageProviderService;
        }

        @Override
        public List<CommandCompletion> complete(final CommandContext context, final String string) {
            // grabs the commands the player can see.
            return Collections.unmodifiableList(Sponge.server().commandManager().complete(context.subject(), context.cause().audience(), string));
        }

        @Override
        public Optional<? extends CommandMapping> parseValue(
                final Parameter.@NonNull Key<? super CommandMapping> parameterKey,
                final ArgumentReader.@NonNull Mutable reader,
                final CommandContext.@NonNull Builder context) throws ArgumentParseException {
            final String next = reader.parseString();
            final Optional<CommandMapping> commandMapping = Sponge.server().commandManager().commandMapping(next);
            if (commandMapping.filter(x -> x.registrar().canExecute(context.cause(), x)).isPresent()) {
                return commandMapping;
            }
            throw reader.createException(this.messageProviderService.getMessageFor(context.cause().audience(), "command.commandinfo.nocommand", next));
        }
    }
}
