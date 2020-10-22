/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.scaffold.command.control;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import io.github.nucleuspowered.nucleus.Util;
import io.github.nucleuspowered.nucleus.api.util.NoExceptionAutoClosable;
import io.github.nucleuspowered.nucleus.modules.core.config.CoreConfig;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandContext;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandExecutor;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandInterceptor;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandResult;
import io.github.nucleuspowered.nucleus.scaffold.command.NucleusArgumentParseException;
import io.github.nucleuspowered.nucleus.scaffold.command.NucleusCommandException;
import io.github.nucleuspowered.nucleus.scaffold.command.annotation.Command;
import io.github.nucleuspowered.nucleus.scaffold.command.annotation.CommandModifier;
import io.github.nucleuspowered.nucleus.scaffold.command.config.CommandModifiersConfig;
import io.github.nucleuspowered.nucleus.scaffold.command.impl.CommandContextImpl;
import io.github.nucleuspowered.nucleus.scaffold.command.modifier.CommandModifierFactory;
import io.github.nucleuspowered.nucleus.scaffold.command.modifier.CommandModifiers;
import io.github.nucleuspowered.nucleus.scaffold.command.modifier.ICommandModifier;
import io.github.nucleuspowered.nucleus.services.INucleusServiceCollection;
import io.github.nucleuspowered.nucleus.services.interfaces.IReloadableService;
import io.github.nucleuspowered.nucleus.util.PrettyPrinter;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.event.Level;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandCallable;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandPermissionException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.ArgumentParseException;
import org.spongepowered.api.command.args.CommandArgs;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.command.args.parsing.InputTokenizer;
import org.spongepowered.api.command.args.parsing.SingleArg;
import org.spongepowered.api.command.source.CommandBlockSource;
import org.spongepowered.api.command.source.ConsoleSource;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.service.context.Context;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.util.Tuple;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

public class CommandControl implements CommandCallable {

    private static final String CONTEXT_KEY = "nucleus_command";
    private static final InputTokenizer tokeniser = InputTokenizer.quotedStrings(false);

    private final INucleusServiceCollection serviceCollection;
    private final ImmutableList<String> basicPermission;
    private final CommandMetadata metadata;
    @Nullable private final ICommandExecutor<? extends CommandSource> executor;
    private final Class<? extends CommandSource> sourceType;
    private final UsageCommand usageCommand;
    private final boolean isAsync;

    private final SortedMap<String, CommandCallable> subcommands = new TreeMap<>();
    private final Map<String, CommandCallable> primarySubcommands = new HashMap<>();
    private final CommandElement element;
    private final String commandKey;
    private final Context context;
    private final List<String> aliases;
    private final boolean hasHelpCommand;
    private final ImmutableMap<CommandModifier, ICommandModifier> modifiers;
    private final Map<String, String> exemptionPermissions;
    private final CommandModifiersConfig commandModifiersConfig = new CommandModifiersConfig();

    private final String command;
    private boolean acceptingRegistration = true;

    public CommandControl(
            @Nullable ICommandExecutor<? extends CommandSource> executor,
            @Nullable CommandControl parent,
            CommandMetadata meta,
            INucleusServiceCollection serviceCollection) {
        this.executor = meta.getCommandAnnotation().hasExecutor() ? executor : null;
        this.metadata = meta;
        this.commandKey = meta.getCommandKey();
        this.context = new Context(CONTEXT_KEY, this.commandKey.replace(".", " "));
        this.basicPermission = ImmutableList.copyOf(meta.getCommandAnnotation().basePermission());
        this.serviceCollection = serviceCollection;
        this.hasHelpCommand = meta.getCommandAnnotation().hasHelpCommand();
        CommandElement[] elements = executor == null ? new CommandElement[0] : executor.parameters(serviceCollection);
        if (elements.length == 0) {
            this.element = GenericArguments.none();
        } else if (elements.length == 1) {
            this.element = elements[0];
        } else {
            this.element = GenericArguments.seq(elements);
        }

        this.aliases = ImmutableList.copyOf(meta.getAliases());
        Class<? extends CommandSource> c = CommandSource.class;
        if (this.executor != null) {
            for (Type type : this.executor.getClass().getGenericInterfaces()) {
                if (type.getTypeName().startsWith(ICommandExecutor.class.getName()) && type instanceof ParameterizedType) {
                    //noinspection unchecked
                    c = (Class<? extends CommandSource>) (((ParameterizedType) type).getActualTypeArguments()[0]);
                    break;
                }
            }
        }

        this.sourceType = c;
        this.usageCommand = new UsageCommand(this);
        if (parent != null) {
            this.command = parent.command + " " + meta.getAliases()[0];
        } else {
            this.command = meta.getAliases()[0];
        }
        this.isAsync = meta.getCommandAnnotation().async();

        // this must be last.
        this.modifiers = validateModifiers(this, serviceCollection.logger(), meta.getCommandAnnotation());
        this.exemptionPermissions = this.modifiers.keySet().stream().collect(Collectors.toMap(CommandModifier::value, CommandModifier::exemptPermission));
    }

    public void attach(String alias, CommandControl commandControl) {
        Preconditions.checkState(this.acceptingRegistration, "Registration is complete.");
        this.subcommands.putIfAbsent(alias, commandControl);
        this.primarySubcommands.putIfAbsent(
                commandControl.getCommandKey().substring(commandControl.getCommandKey().lastIndexOf(".") + 1),
                commandControl);
    }

    public void completeRegistration(INucleusServiceCollection serviceCollection) {
        Preconditions.checkState(this.acceptingRegistration, "Registration is complete.");
        this.acceptingRegistration = false;
        if (this.executor instanceof IReloadableService.Reloadable) {
            IReloadableService.Reloadable reloadable = (IReloadableService.Reloadable) this.executor;
            serviceCollection.reloadableService().registerReloadable(reloadable);
            reloadable.onReload(serviceCollection);
        }
    }

    @Override
    @NonNull
    public CommandResult process(@NonNull CommandSource source, @NonNull String arguments) throws CommandException {
        // do we have a subcommand?
        CommandArgs args = new CommandArgs(arguments, tokeniser.tokenize(arguments, false));
        ICommandResult commandResult = process(Sponge.getCauseStackManager().getCurrentCause(), source, arguments, args);
        return commandResult.isSuccess() ? CommandResult.success() : CommandResult.empty();
    }

    public ICommandResult process(@Nonnull Cause cause,
            @NonNull CommandSource source,
            @NonNull String arguments,
            CommandArgs args) throws CommandException {
        // Phase one: child command processing. Keep track of all thrown arguments.
        List<Tuple<String, CommandException>> thrown = Lists.newArrayList();
        final CommandContext context = new CommandContext();
        final CommandArgs.Snapshot state = args.getSnapshot();

        if (args.hasNext()) {
            String next = args.peek();

            // If this works, then we're A-OK.
            CommandCallable callable = this.subcommands.get(next);
            if (callable != null) {
                try {
                    if (callable instanceof CommandControl) {
                        args.next();
                        return ((CommandControl) callable).process(cause, source, getArgumentStringFromCurrentArgumentState(args), args);
                    } else {
                        int successCount = callable.process(source, getArgumentStringFromCurrentArgumentState(args)).getSuccessCount().orElse(0);
                        return successCount > 0 ? ICommandResult.success() : ICommandResult.fail();
                    }
                } catch (ArgumentParseException e) {
                    // We should only fallback if arguments couldn't parse.
                    // If the Exception is _not_ of right type, wrap it and add it. This shouldn't happen though.
                    if (callable instanceof CommandControl) {
                        CommandControl control = (CommandControl) callable;
                        thrown.add(Tuple.of(command + " " + next, NucleusArgumentParseException.from(
                                this.serviceCollection.messageProvider(),
                                e,
                                control.getUsage(source),
                                control.getSubcommandTexts()
                        )));
                    } else {
                        thrown.add(Tuple.of(command + " " + next, NucleusArgumentParseException.from(
                                this.serviceCollection.messageProvider(),
                                e,
                                callable.getUsage(source),
                                Text.of()
                        )));
                    }
                } finally {
                    args.applySnapshot(state);
                }
            }

            if (this.hasHelpCommand && next.toLowerCase().equals("help") || next.toLowerCase().equals("?")) {
                this.usageCommand.process(
                        getContextFrom(source,
                            cause,
                            context,
                            ImmutableMap.of(),
                            this,
                            this.serviceCollection
                    ), this.command, null);
                return ICommandResult.success();
            }
        }

        // Ensure we're the correct type.
        checkSourceType(source);

        // Create the ICommandContext
        // TODO: Abstract this away
        Map<CommandModifier, ICommandModifier> modifiers = selectAppropriateModifiers(source);
        ICommandContext.Mutable<? extends CommandSource> contextSource = getContextFrom(source,
                cause,
                context,
                modifiers,
                this,
                this.serviceCollection);

        try (NoExceptionAutoClosable closable = this.serviceCollection.permissionService().setContextTemporarily(source, this.context)) {
            // Do we have permission?
            if (!testPermission(source)) {
                throw new CommandPermissionException();
            }

            // Can we run this command? Exception will be thrown if not.
            for (Map.Entry<CommandModifier, ICommandModifier> x : modifiers.entrySet()) {
                Optional<Text> req = x.getValue().testRequirement(contextSource, this, this.serviceCollection, x.getKey());
                if (req.isPresent()) {
                    // Nope, we're out
                    throw new CommandException(req.get());
                }
            }

            if (this.executor == null) {
                if (thrown.isEmpty()) {
                    // OK, we just process the usage command instead.
                    this.usageCommand.process(contextSource, command, args.nextIfPresent().map(String::toLowerCase).orElse(null));
                    return contextSource.successResult();
                } else {
                    throw new NucleusCommandException(thrown, false, this.serviceCollection.messageProvider());
                }
            }

            try {
                try {
                    // Okay, parse this.
                    this.element.parse(source, args, context);
                    if (args.hasNext()) {
                        thrown.add(Tuple.of(command, new NucleusArgumentParseException(
                                this.serviceCollection.messageProvider(),
                                Text.of(TextColors.RED, "Too many arguments"),
                                args.getRaw(),
                                args.getRawPosition(),
                                Text.of(getUsage(source)),
                                getSubcommandTexts(source),
                                true)));
                        throw new NucleusCommandException(thrown, true, this.serviceCollection.messageProvider());
                    }
                } catch (NucleusCommandException nce) {
                    throw nce;
                } catch (ArgumentParseException ape) {
                    // get the command to get the usage/subs from.
                    thrown.add(Tuple.of(this.command, NucleusArgumentParseException.from(this.serviceCollection.messageProvider(),
                            ape,
                            Text.of(getUsage(source)),
                            getSubcommandTexts(source))));
                    throw new NucleusCommandException(thrown, true, this.serviceCollection.messageProvider());
                } catch (Throwable throwable) {
                    String m;
                    if (throwable.getMessage() == null) {
                        m = "null";
                    } else {
                        m = throwable.getMessage();
                    }

                    thrown.add(
                            Tuple.of(this.command, new CommandException(
                                    this.serviceCollection.messageProvider().getMessageFor(source, "command.exception.unexpected", m), throwable)));
                    throwable.printStackTrace(); // this is on demand, so we should throw it.
                    throw new NucleusCommandException(thrown, true, this.serviceCollection.messageProvider());
                }


                // execution
                //noinspection unchecked
                Optional<ICommandResult> result = this.executor.preExecute((ICommandContext.Mutable) contextSource);
                if (result.isPresent()) {
                    // STOP.
                    onResult(source, contextSource, result.get());
                    return result.get();
                }

                // Modifiers might have something to say about it.
                for (Map.Entry<CommandModifier, ICommandModifier> modifier : contextSource.modifiers().entrySet()) {
                    if (modifier.getKey().onExecute()) {
                        result = modifier.getValue().preExecute(contextSource, this, this.serviceCollection, modifier.getKey());
                        if (result.isPresent()) {
                            // STOP.
                            onResult(source, contextSource, result.get());
                            return result.get();
                        }
                    }
                }

                return execute(source, contextSource);
            } catch (Exception ex) {
                // Run any fail actions.
                runFailActions(contextSource);
                throw ex;
            }
        }
    }

    public Text getSubcommandTexts() {
        return getSubcommandTexts(null);
    }

    public Text getSubcommandTexts(@Nullable CommandSource source) {
        return Text.joinWith(Text.of(", "), this.primarySubcommands.entrySet()
                .stream()
                .filter(x -> source == null || x.getValue().testPermission(source))
                .map(x -> Text.of(x.getKey()))
                .collect(Collectors.toList()));
    }

    private <T extends CommandSource> void runFailActions(ICommandContext<T> contextSource) {
        contextSource.failActions().forEach(x -> x.accept(contextSource));
    }

    // Entry point for warmups.
    public void startExecute(@NonNull ICommandContext<? extends CommandSource> contextSource) {
        CommandSource source;
        try {
            source = contextSource.getCommandSource();
        } catch (CommandException ex) {
            this.serviceCollection.logger().warn("Could not get command source, cancelling command execution (did the player disconnect?)", ex);
            return;
        }

        try {
            execute(source, contextSource);
        } catch (CommandException ex) {
            // If we are here, then we're handling the command ourselves.
            Text message = ex.getText() == null ? Text.of(TextColors.RED, "Unknown error!") : ex.getText();
            onFail(contextSource, message);
            this.serviceCollection.logger().warn("Error executing command {}", this.command, ex);
        }
    }

    @SuppressWarnings("unchecked")
    private ICommandResult execute(
            CommandSource source,
            @NonNull ICommandContext<? extends CommandSource> context) throws CommandException {
        Preconditions.checkState(this.executor != null, "executor");
        for (ICommandInterceptor commandInterceptor : context.getServiceCollection().commandMetadataService().interceptors()) {
            commandInterceptor.onPreCommand(
                    (Class<ICommandExecutor<?>>) this.executor.getClass(),
                    this,
                    context
            );
        }

        ICommandResult result;
        if (this.isAsync) {
            runAsync(source, context);
            result = context.successResult();
        } else {
            // Anything else to go here?
            result = this.executor.execute((ICommandContext) context);
            onResult(source, context, result);
            for (ICommandInterceptor commandInterceptor : context.getServiceCollection().commandMetadataService().interceptors()) {
                commandInterceptor.onPostCommand(
                        (Class<ICommandExecutor<?>>) this.executor.getClass(),
                        this,
                        context,
                        result
                );
            }
        }

        return result;
    }

    private void runAsync(final CommandSource source, final ICommandContext<? extends CommandSource> context) {
        Preconditions.checkState(this.executor != null, "executor");
        Task.builder().execute(task -> {
            ICommandResult result;
            try {
                //noinspection unchecked
                result = this.executor.execute((ICommandContext) context);
            } catch (CommandException e) {
                result = context.errorResultLiteral(e.getText());
            }

            final ICommandResult fResult = result;
            Task.builder().execute(t -> {
                try {
                    onResult(source, context, fResult);
                    for (ICommandInterceptor commandInterceptor : context.getServiceCollection().commandMetadataService().interceptors()) {
                        commandInterceptor.onPostCommand(
                                (Class<ICommandExecutor<?>>) this.executor.getClass(),
                                this,
                                context,
                                fResult
                        );
                    }
                } catch (CommandException e) {
                    e.printStackTrace();
                }
            }).submit(this.serviceCollection.pluginContainer());
        }).async().submit(this.serviceCollection.pluginContainer());
    }

    private void onResult(CommandSource source, ICommandContext<? extends CommandSource> contextSource, ICommandResult result) throws CommandException {
        if (result.isSuccess()) {
            onSuccess(contextSource);
        } else if (!result.isWillContinue()) {
            onFail(contextSource, result.getErrorMessage(source).orElse(null));
        }

        // The command will continue later. Don't do anything.
    }

    private void onSuccess(ICommandContext<? extends CommandSource> source) throws CommandException {
        for (Map.Entry<CommandModifier, ICommandModifier> x : source.modifiers().entrySet()) {
            if (x.getKey().onCompletion()) {
                x.getValue().onCompletion(source, this, this.serviceCollection, x.getKey());
            }
        }
    }

    public void onFail(ICommandContext<? extends CommandSource> source, @Nullable Text errorMessage) {
        // Run any fail actions.
        runFailActions(source);
        if (errorMessage != null) {
            source.getCommandSourceUnchecked().sendMessage(errorMessage);
        }
    }

    private Map<CommandModifier, ICommandModifier> selectAppropriateModifiers(CommandSource source) throws CommandException {
        return this.modifiers
                .entrySet()
                .stream()
                .filter(x -> x.getKey().target().isInstance(source))
                .filter(x -> {
                    try {
                        return x.getValue().canExecuteModifier(this.serviceCollection, source);
                    } catch (CommandException e) {
                        e.printStackTrace();
                        return false;
                    }
                })
                .filter(x -> x.getKey().exemptPermission().isEmpty() ||
                        !this.serviceCollection.permissionService().hasPermission(source, x.getKey().exemptPermission()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    @Override
    @NonNull
    public List<String> getSuggestions(@NonNull CommandSource source, @NonNull String arguments, @Nullable Location<World> targetPosition)
            throws CommandException {
        List<SingleArg> singleArgs = Lists.newArrayList(tokeniser.tokenize(arguments, false));
        // If we end with a space - then we add another argument.
        if (arguments.isEmpty() || arguments.endsWith(" ")) {
            singleArgs.add(new SingleArg("", arguments.length() - 1, arguments.length() - 1));
        }

        final CommandArgs args = new CommandArgs(arguments, singleArgs);

        final List<String> options = Lists.newArrayList();
        CommandContext context = new CommandContext();
        context.putArg(CommandContext.TAB_COMPLETION, true); // We don't care for the value.

        // Subcommand
        if (args.size() == 1) {
            this.subcommands.keySet()
                    .stream()
                    .filter(x -> x.toLowerCase().startsWith(args.get(0).toLowerCase()))
                    .forEach(options::add);
        } else if (args.size() > 1) {
            CommandCallable callable = this.subcommands.get(args.peek().toLowerCase());

            // If we have a subcommand, get the completions for that too.
            if (callable != null) {
                CommandArgs.Snapshot state = args.getSnapshot();
                String[] splitArgs = arguments.split(" ", 2);
                String targetArg = splitArgs.length == 1 ? "" : splitArgs[1];
                options.addAll(callable.getSuggestions(source, targetArg, targetPosition));
                args.applySnapshot(state);
            }
        }

        options.addAll(this.element.complete(source, args, context));
        return options.stream().distinct().collect(Collectors.toList());
    }

    public ImmutableList<String> getPermission() {
        return this.basicPermission;
    }

    @Override
    public boolean testPermission(@NonNull CommandSource source) {
        return this.basicPermission.stream().allMatch(x -> this.serviceCollection.permissionService().hasPermission(source, x));
    }

    @Override
    @NonNull
    public Optional<Text> getShortDescription(@NonNull CommandSource source) {
        return Optional.of(this.serviceCollection
                .messageProvider()
                .getMessageFor(source, this.metadata.getCommandAnnotation().commandDescriptionKey() + ".desc"));
    }

    public Optional<Text> getExtendedDescription(@NonNull CommandSource source) {
        try {
            return Optional.ofNullable(
                    this.serviceCollection
                            .messageProvider()
                            .getMessageFor(source, this.metadata.getCommandAnnotation().commandDescriptionKey() + ".extended")
            );
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    @Override
    @NonNull
    public Optional<Text> getHelp(@NonNull CommandSource source) {
        final Optional<Text> extended = getExtendedDescription(source);
        if (extended.isPresent()) {
            return getShortDescription(source)
                    .map(text -> Optional.of(Text.of(text, Text.NEW_LINE, Util.SPACE, Text.NEW_LINE, extended.get())))
                    .orElse(extended);
        } else {
            return getShortDescription(source);
        }
    }

    @Override
    @NonNull
    public Text getUsage(@NonNull final CommandSource source) {
        Text.Builder builder = getUsageText(source).toBuilder();
        Collection<Text> ct = getSubcommandText(source);
        if (!ct.isEmpty()) {
            builder.append(Text.NEW_LINE)
                    .append(Text.joinWith(Text.NEW_LINE, ct));
        }
        return builder.build();
    }

    public Collection<Text> getSubcommandText(@NonNull CommandSource source) {
        List<Text> texts = new ArrayList<>();
        if (!this.primarySubcommands.isEmpty()) {
            this.primarySubcommands.values().stream()
                    .filter(commandControl -> commandControl instanceof CommandControl && commandControl.testPermission(source))
                    .map(commandControl -> ((CommandControl) commandControl).getUsageText(source))
                    .forEach(texts::add);
        }

        return texts;
    }

    public Text getUsageText(@NonNull CommandSource source) {
        return this.serviceCollection.messageProvider().getMessageFor(source,
                "command.usage.bl",
                Text.of(this.command),
                this.element.getUsage(source));
    }

    @Nullable
    private CommandCallable getSubcommand(String subcommand, @Nullable CommandSource source) {
        CommandCallable control = this.subcommands.get(subcommand.toLowerCase());
        if (source == null || control.testPermission(source)) {
            return control;
        }

        return null;
    }

    public CommandModifiersConfig getCommandModifiersConfig() {
        return this.commandModifiersConfig;
    }

    Collection<CommandCallable> getSubcommands() {
        return this.subcommands.values();
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

    Class<? extends CommandSource> getSourceType() {
        return this.sourceType;
    }

    public CommandMetadata getMetadata() {
        return this.metadata;
    }

    boolean hasExecutor() {
        return this.executor != null;
    }

    private CommandException getExceptionFromKey(CommandSource source, String key, String... subs) {
        return new CommandException(this.serviceCollection.messageProvider().getMessageFor(source, key, subs));
    }

    private void checkSourceType(CommandSource source) throws CommandException {
        if (!this.sourceType.isInstance(source)) {
            if (this.sourceType.equals(Player.class) && !(source instanceof Player)) {
                throw getExceptionFromKey(source, "command.playeronly");
            } else if (this.sourceType.equals(ConsoleSource.class) && !(source instanceof ConsoleSource)) {
                throw getExceptionFromKey(source, "command.consoleonly");
            } else if (this.sourceType.equals(CommandBlockSource.class) && !(source instanceof CommandBlockSource)) {
                throw getExceptionFromKey(source, "command.commandblockonly");
            }

            throw getExceptionFromKey(source, "command.unknownsource");
        }
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

    public int getCooldown(Subject subject) {
        final String exemptionPermission = this.exemptionPermissions.get(CommandModifiers.HAS_COOLDOWN);
        if (exemptionPermission != null && this.serviceCollection.permissionService().hasPermission(subject, exemptionPermission)) {
            return 0;
        }
        return this.serviceCollection.permissionService()
                .getIntOptionFromSubject(subject, String.format("nucleus.%s.cooldown", this.command.replace(" ", ".")))
                .orElseGet(this::getCooldown);
    }

    public int getWarmup() {
        return this.commandModifiersConfig.getWarmup();
    }

    public int getWarmup(Subject subject) {
        final String exemptionPermission = this.exemptionPermissions.get(CommandModifiers.HAS_WARMUP);
        if (exemptionPermission != null && this.serviceCollection.permissionService().hasPermission(subject, exemptionPermission)) {
            return 0;
        }
        return this.serviceCollection.permissionService()
                .getIntOptionFromSubject(subject, String.format("nucleus.%s.warmup", this.command.replace(" ", ".")))
                .orElseGet(this::getWarmup);
    }

    public double getCost() {
        return this.commandModifiersConfig.getCost();
    }

    public double getCost(Subject subject) {
        final String exemptionPermission = this.exemptionPermissions.get(CommandModifiers.HAS_COST);
        if (exemptionPermission != null && this.serviceCollection.permissionService().hasPermission(subject, exemptionPermission)) {
            return 0;
        }
        return this.serviceCollection.permissionService()
                .getDoubleOptionFromSubject(subject, String.format("nucleus.%s.cost", this.command.replace(" ", ".")))
                .orElseGet(this::getCost);
    }

    Collection<String> getAliases() {
        return ImmutableList.copyOf(this.aliases);
    }

    private static ImmutableMap<CommandModifier, ICommandModifier> validateModifiers(CommandControl control, Logger logger, Command command) {
        if (command.modifiers().length == 0) {
            return ImmutableMap.of();
        }

        ImmutableMap.Builder<CommandModifier, ICommandModifier> modifiers = new ImmutableMap.Builder<>();
        for (CommandModifier modifier : command.modifiers()) {
            try {
                // Get the registry entry.
                ICommandModifier commandModifier = Sponge.getRegistry().getType(CommandModifierFactory.class, modifier.value())
                        .map(x -> x.apply(control))
                        .orElseThrow(() -> new IllegalArgumentException("Could not get registry entry for \"" + modifier.value() + "\""));
                commandModifier.validate(modifier);
                modifiers.put(modifier, commandModifier);
            } catch (IllegalArgumentException ex) {
                // could not validate
                PrettyPrinter printer = new PrettyPrinter();
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

        return modifiers.build();
    }

    private static ICommandContext.Mutable<? extends CommandSource> getContextFrom(CommandSource source,
            Cause cause,
            CommandContext context,
            Map<CommandModifier, ICommandModifier> modifiers,
            CommandControl control,
            INucleusServiceCollection serviceCollection) throws CommandException {
        if (source instanceof Player) {
            return new CommandContextImpl.PlayerSource(
                    cause,
                    context,
                    serviceCollection,
                    () -> Sponge.getServer().getPlayer(((Player) source).getUniqueId()).orElseThrow(() -> new CommandException(
                            Text.of("Player is no longer available.")
                    )),
                    (Player) source,
                    control,
                    modifiers
            );
        } else if (source instanceof ConsoleSource) {
            return new CommandContextImpl.Console(
                    cause,
                    context,
                    serviceCollection,
                    Sponge.getServer().getConsole(),
                    control,
                    modifiers,
                    serviceCollection.moduleDataProvider().getModuleConfig(CoreConfig.class).isConsoleOverride()
            );
        } else {
            return new CommandContextImpl.Any(
                    cause,
                    context,
                    serviceCollection,
                    source,
                    control,
                    modifiers
            );
        }
    }

    private static String getArgumentStringFromCurrentArgumentState(CommandArgs args) {
        return args.getRaw().substring(args.getRawPosition()).trim();
    }

}
