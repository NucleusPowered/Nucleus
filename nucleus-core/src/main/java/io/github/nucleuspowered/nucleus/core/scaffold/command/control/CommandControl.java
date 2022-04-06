/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.core.scaffold.command.control;

import io.github.nucleuspowered.nucleus.api.util.NoExceptionAutoClosable;
import io.github.nucleuspowered.nucleus.core.Registry;
import io.github.nucleuspowered.nucleus.core.scaffold.command.ICommandContext;
import io.github.nucleuspowered.nucleus.core.scaffold.command.ICommandExecutor;
import io.github.nucleuspowered.nucleus.core.scaffold.command.ICommandResult;
import io.github.nucleuspowered.nucleus.core.scaffold.command.annotation.Command;
import io.github.nucleuspowered.nucleus.core.scaffold.command.annotation.CommandModifier;
import io.github.nucleuspowered.nucleus.core.scaffold.command.config.CommandModifiersConfig;
import io.github.nucleuspowered.nucleus.core.scaffold.command.impl.CommandContextImpl;
import io.github.nucleuspowered.nucleus.core.scaffold.command.modifier.ICommandModifier;
import io.github.nucleuspowered.nucleus.core.services.INucleusServiceCollection;
import io.github.nucleuspowered.nucleus.core.services.interfaces.IReloadableService;
import io.github.nucleuspowered.nucleus.core.util.PrettyPrinter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandCause;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.exception.CommandException;
import org.spongepowered.api.command.exception.CommandPermissionException;
import org.spongepowered.api.command.parameter.CommandContext;
import org.spongepowered.api.command.parameter.Parameter;
import org.spongepowered.api.command.parameter.managed.Flag;
import org.spongepowered.api.service.context.Context;
import org.spongepowered.api.service.permission.Subject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public final class CommandControl {

    private static final String CONTEXT_KEY = "nucleus_command";

    private final INucleusServiceCollection serviceCollection;
    private final List<String> basicPermission;
    private final CommandMetadata metadata;
    @Nullable private final ICommandExecutor executor;

    private final Map<CommandControl, List<String>> subcommands = new HashMap<>();
    private final Map<String, CommandControl> primarySubcommands = new HashMap<>();
    private final String commandKey;
    private final Context context;
    private final List<String> aliases;
    private final Map<CommandModifier, ICommandModifier> modifiers;
    private final CommandModifiersConfig commandModifiersConfig = new CommandModifiersConfig();

    private final String command;
    private boolean acceptingRegistration = true;

    public CommandControl(
            @Nullable final ICommandExecutor executor,
            @Nullable final CommandControl parent,
            final CommandMetadata meta,
            final INucleusServiceCollection serviceCollection) {
        this.executor = meta.getCommandAnnotation().hasExecutor() ? executor : null;
        this.metadata = meta;
        this.commandKey = meta.getCommandKey();
        this.context = new Context(CONTEXT_KEY, this.commandKey.replace(".", " "));
        this.basicPermission = Collections.unmodifiableList(Arrays.asList(meta.getCommandAnnotation().basePermission()));
        this.serviceCollection = serviceCollection;

        this.aliases = Collections.unmodifiableList(Arrays.asList(meta.getAliases()));
        if (parent != null) {
            this.command = parent.command + " " + meta.getAliases()[0];
        } else {
            this.command = meta.getAliases()[0];
        }

        // this must be last.
        this.modifiers = CommandControl.validateModifiers(this, serviceCollection.logger(), meta.getCommandAnnotation());
    }

    public void attach(final String alias, final CommandControl commandControl) {
        if (!this.acceptingRegistration) {
            throw new IllegalStateException("Registration is complete.");
        }
        this.subcommands.computeIfAbsent(commandControl, x -> new ArrayList<>()).add(alias.toLowerCase());
        this.primarySubcommands.putIfAbsent(
                commandControl.getCommandKey().substring(commandControl.getCommandKey().lastIndexOf(".") + 1),
                commandControl);
    }

    public void completeRegistration(final INucleusServiceCollection serviceCollection) {
        if (!this.acceptingRegistration) {
            throw new IllegalStateException("Registration is complete.");
        }
        this.acceptingRegistration = false;
        if (this.executor instanceof IReloadableService.Reloadable) {
            final IReloadableService.Reloadable reloadable = (IReloadableService.Reloadable) this.executor;
            serviceCollection.reloadableService().registerReloadable(reloadable);
            reloadable.onReload(serviceCollection);
        }
    }

    @Nullable
    public ICommandExecutor getExecutor() {
        return this.executor;
    }

    public org.spongepowered.api.command.Command.Parameterized createCommand() {
        final org.spongepowered.api.command.Command.Builder b = org.spongepowered.api.command.Command.builder();
        if (this.executor != null) {
            b.executor(this::process);
            for (final Parameter parameter : this.executor.parameters(this.serviceCollection)) {
                b.addParameter(parameter);
            }
            for (final Flag flag : this.executor.flags(this.serviceCollection)) {
                b.addFlag(flag);
            }
        }
        b.executionRequirements(re -> {
            for (final String perm : this.getPermission()) {
                if (!this.serviceCollection.permissionService().hasPermission(re, perm)) {
                    return false;
                }
            }
            return true;
        });
        b.shortDescription(this::getShortDescription).extendedDescription(this::getExtendedDescription);
        for (final Map.Entry<CommandControl, List<String>> control : this.subcommands.entrySet()) {
            b.addChild(control.getKey().createCommand(), control.getValue());
        }
        return b.build();
    }

    @NonNull
    public CommandResult process(@NonNull final CommandContext context) throws CommandException {
        if (this.executor == null) {
            throw new CommandException(Component.text("This should not be executed"));
        }
         // Create the ICommandContext
        final Map<CommandModifier, ICommandModifier> modifiers = this.selectAppropriateModifiers(context);
        final ICommandContext contextSource = new CommandContextImpl(
                context.cause(),
                context,
                this.serviceCollection,
                this,
                context.cause().subject().equals(Sponge.systemSubject()),
                modifiers
        );

        try (final NoExceptionAutoClosable ignored = this.serviceCollection.permissionService().setContextTemporarily(context, this.context)) {
            // Do we have permission?
            if (!this.testPermission(context)) {
                throw new CommandPermissionException(contextSource.getMessage("permissions.nopermission"));
            }

            // Can we run this command? Exception will be thrown if not.
            for (final Map.Entry<CommandModifier, ICommandModifier> x : modifiers.entrySet()) {
                final Optional<? extends Component> req = x.getValue().testRequirement(contextSource, this, this.serviceCollection, x.getKey());
                if (req.isPresent()) {
                    // Nope, we're out
                    throw new CommandException(req.get());
                }
            }

            try {
                // execution
                Optional<ICommandResult> result = this.executor.preExecute(contextSource);
                if (result.isPresent()) {
                    // STOP.
                    this.onResult(contextSource, result.get());
                    return result.get().getResult(contextSource);
                }

                // Modifiers might have something to say about it.
                for (final Map.Entry<CommandModifier, ICommandModifier> modifier : contextSource.modifiers().entrySet()) {
                    if (modifier.getKey().onExecute()) {
                        result = modifier.getValue().preExecute(contextSource, this, this.serviceCollection, modifier.getKey());
                        if (result.isPresent()) {
                            // STOP.
                            this.onResult(contextSource, result.get());
                            return result.get().getResult(contextSource);
                        }
                    }
                }

                return this.execute(contextSource).getResult(contextSource);
            } catch (final Exception ex) {
                // Run any fail actions.
                this.runFailActions(contextSource);
                throw ex;
            }
        }
    }

    public Component getSubcommandTexts() {
        return this.getSubcommandTexts(null);
    }

    public Component getSubcommandTexts(@Nullable final CommandContext source) {
        return Component.join(Component.text(", "), this.primarySubcommands.entrySet()
                .stream()
                .filter(x -> source == null || x.getValue().testPermission(source))
                .map(x -> Component.text(x.getKey()))
                .collect(Collectors.toList()));
    }

    private void runFailActions(final ICommandContext contextSource) {
        contextSource.failActions().forEach(x -> x.accept(contextSource));
    }

    // Entry point for warmups.
    public void startExecute(@NonNull final ICommandContext contextSource) {
        try {
            this.execute(contextSource);
        } catch (final CommandException ex) {
            // If we are here, then we're handling the command ourselves.
            final Component message = ex.componentMessage() == null ? Component.text("Unknown error!", NamedTextColor.RED) : ex.componentMessage();
            this.onFail(contextSource, message);
            this.serviceCollection.logger().warn("Error executing command {}", this.command, ex);
        }
    }

    private ICommandResult execute(@NonNull final ICommandContext context) throws CommandException {
        if (this.executor == null) {
            throw new IllegalStateException("Executor is missing.");
        }
        final ICommandResult result;
        // Anything else to go here?
        result = this.executor.execute(context);
        this.onResult(context, result);
        return result;
    }

    private void onResult(final ICommandContext contextSource, final ICommandResult result) throws CommandException {
        if (result.isSuccess()) {
            this.onSuccess(contextSource);
        } else if (!result.isWillContinue()) {
            // This occurs when no exception is thrown, so the error in the command result
            // will be returned.
            this.onFail(contextSource, null);
        }

        // The command will continue later. Don't do anything.
    }

    private void onSuccess(final ICommandContext source) throws CommandException {
        for (final Map.Entry<CommandModifier, ICommandModifier> x : source.modifiers().entrySet()) {
            if (x.getKey().onCompletion()) {
                x.getValue().onCompletion(source, this, this.serviceCollection, x.getKey());
            }
        }
    }

    public void onFail(final ICommandContext source, @Nullable final Component errorMessage) {
        // Run any fail actions.
        this.runFailActions(source);
        if (errorMessage != null) {
            source.sendMessageText(errorMessage);
        }
    }

    private Map<CommandModifier, ICommandModifier> selectAppropriateModifiers(final CommandContext source) {
        return this.modifiers
                .entrySet()
                .stream()
                .filter(x -> x.getKey().target().isInstance(source.cause().root()))
                .filter(x -> {
                    try {
                        return x.getValue().canExecuteModifier(this.serviceCollection, source);
                    } catch (final CommandException e) {
                        e.printStackTrace();
                        return false;
                    }
                })
                .filter(x -> x.getKey().exemptPermission().isEmpty() ||
                        !this.serviceCollection.permissionService().hasPermission(source, x.getKey().exemptPermission()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    public Optional<Component> getShortDescription(@NonNull final CommandCause source) {
        return Optional.of(this.serviceCollection
                .messageProvider()
                .getMessageFor(source.audience(), this.metadata.getCommandAnnotation().commandDescriptionKey() + ".desc"));
    }

    public Optional<Component> getExtendedDescription(@NonNull final CommandCause source) {
        try {
            return Optional.ofNullable(
                    this.serviceCollection
                            .messageProvider()
                            .getMessageFor(source.audience(), this.metadata.getCommandAnnotation().commandDescriptionKey() + ".extended")
            );
        } catch (final Exception e) {
            return Optional.empty();
        }
    }

    public Collection<String> getPermission() {
        return this.basicPermission;
    }

    public boolean testPermission(@NonNull final Subject source) {
        return this.basicPermission.stream().allMatch(x -> this.serviceCollection.permissionService().hasPermission(source, x));
    }

    public CommandModifiersConfig getCommandModifiersConfig() {
        return this.commandModifiersConfig;
    }

    public String getCommand() {
        return this.command;
    }

    public String getModifierKey() {
        return this.metadata.getMetadataKey();
    }

    public boolean isModifierKeyRedirected() {
        return this.metadata.isModifierKeyRedirect();
    }

    public CommandMetadata getMetadata() {
        return this.metadata;
    }

    boolean hasExecutor() {
        return this.executor != null;
    }

    public String getCommandKey() {
        return this.commandKey;
    }

    public Context getContext() {
        return this.context;
    }

    public Map<CommandModifier, ICommandModifier> getCommandModifiers() {
        return this.modifiers;
    }

    public int getCooldown() {
        return this.commandModifiersConfig.getCooldown();
    }

    public int getCooldown(final Subject subject) {
        return this.serviceCollection.permissionService()
                .getIntOptionFromSubject(subject, String.format("nucleus.%s.cooldown", this.command.replace(" ", ".")))
                .orElseGet(this::getCooldown);
    }

    public int getWarmup() {
        return this.commandModifiersConfig.getWarmup();
    }

    public int getWarmup(final Subject subject) {
        return this.serviceCollection.permissionService()
                .getIntOptionFromSubject(subject, String.format("nucleus.%s.warmup", this.command.replace(" ", ".")))
                .orElseGet(this::getWarmup);
    }

    public double getCost() {
        return this.commandModifiersConfig.getCost();
    }

    public double getCost(final Subject subject) {
        return this.serviceCollection.permissionService()
                .getDoubleOptionFromSubject(subject, String.format("nucleus.%s.cost", this.command.replace(" ", ".")))
                .orElseGet(this::getCost);
    }

    Collection<String> getAliases() {
        return this.aliases;
    }

    private static Map<CommandModifier, ICommandModifier> validateModifiers(final CommandControl control, final Logger logger,
            final Command command) {
        if (command.modifiers().length == 0) {
            return Collections.emptyMap();
        }

        final Map<CommandModifier, ICommandModifier> modifiers = new LinkedHashMap<>();
        for (final CommandModifier modifier : command.modifiers()) {
            try {
                // Get the registry entry.
                final ICommandModifier commandModifier =
                        Sponge.game()
                                .registry(Registry.Types.COMMAND_MODIFIER_FACTORY)
                                .findValue(ResourceKey.resolve(modifier.value()))
                                .map(x -> x.apply(control))
                                .orElseThrow(() -> new IllegalArgumentException("Could not get registry entry for \"" + modifier.value() + "\""));
                commandModifier.validate(modifier);
                modifiers.put(modifier, commandModifier);
            } catch (final IllegalArgumentException ex) {
                // could not validate
                final PrettyPrinter printer = new PrettyPrinter();
                // Sponge can't find an item type...
                printer.add("Could not add modifier to command!").centre().hr();
                printer.add("Command Description Key: ");
                printer.add("  " + command.commandDescriptionKey());
                printer.add("Modifier: ");
                printer.add("  " + modifier.value());
                printer.hr();
                printer.add("Message:");
                printer.add(ex.getMessage());
                printer.hr();
                printer.add("Stack trace:");
                printer.add(ex);
                printer.log(logger, Level.ERROR);
            }
        }

        return Collections.unmodifiableMap(modifiers);
    }

    public Component getUsage(final ICommandContext context) {
        return Component.empty();
    }
}
