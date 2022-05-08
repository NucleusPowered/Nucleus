/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.core.services.impl.commandmetadata;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.github.nucleuspowered.nucleus.core.IPluginInfo;
import io.github.nucleuspowered.nucleus.core.guice.ConfigDirectory;
import io.github.nucleuspowered.nucleus.core.scaffold.command.ICommandExecutor;
import io.github.nucleuspowered.nucleus.core.scaffold.command.annotation.Command;
import io.github.nucleuspowered.nucleus.core.scaffold.command.annotation.CommandModifier;
import io.github.nucleuspowered.nucleus.core.scaffold.command.annotation.EssentialsEquivalent;
import io.github.nucleuspowered.nucleus.core.scaffold.command.control.CommandControl;
import io.github.nucleuspowered.nucleus.core.scaffold.command.control.CommandMetadata;
import io.github.nucleuspowered.nucleus.core.scaffold.command.modifier.ICommandModifier;
import io.github.nucleuspowered.nucleus.core.services.INucleusServiceCollection;
import io.github.nucleuspowered.nucleus.core.services.interfaces.ICommandMetadataService;
import io.github.nucleuspowered.nucleus.core.services.interfaces.IConfigurateHelper;
import io.github.nucleuspowered.nucleus.core.services.interfaces.IMessageProviderService;
import io.github.nucleuspowered.nucleus.core.services.interfaces.IReloadableService;
import io.github.nucleuspowered.nucleus.core.startuperror.NucleusErrorHandler;
import io.leangen.geantyref.TypeToken;
import io.vavr.Tuple;
import io.vavr.Tuple3;
import io.vavr.Tuple4;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.api.command.manager.CommandMapping;
import org.spongepowered.api.event.lifecycle.RegisterCommandEvent;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.hocon.HoconConfigurationLoader;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.plugin.PluginContainer;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@Singleton
public class CommandMetadataService implements ICommandMetadataService, IReloadableService.Reloadable {

    private static final String ROOT_ALIASES = "root level aliases";
    private static final String ENABLED = "enabled";
    private static final TypeToken<Map<String, Boolean>> MAP_TYPE_TOKEN = new TypeToken<Map<String, Boolean>>() {};

    private final Map<String, String> commandremap = new HashMap<>();
    private final Path commandsFile;
    private final IConfigurateHelper configurateHelper;
    private final IMessageProviderService messageProviderService;
    private final Map<String, CommandMetadata> commandMetadataMap = new HashMap<>();
    private final Map<CommandControl, List<String>> controlToAliases = new HashMap<>();
    private final BiMap<Class<? extends ICommandExecutor>, CommandControl> controlToExecutorClass = HashBiMap.create();
    private final Logger logger;
    private final PluginContainer pluginContainer;
    private final IPluginInfo pluginInfo;
    private final IReloadableService reloadableService;

    private CommentedConfigurationNode commandsConfConfigNode;
    private boolean registrationComplete = false;
    private final List<String> registeredAliases = new ArrayList<>();
    private final Map<CommandMetadata, CommandControl> registeredCommands = new HashMap<>();

    @Inject
    public CommandMetadataService(@ConfigDirectory final Path configDirectory,
            final IReloadableService reloadableService,
            final IMessageProviderService messageProviderService,
            final IConfigurateHelper helper,
            final Logger logger,
            final PluginContainer pluginContainer,
            final IPluginInfo pluginInfo) {
        reloadableService.registerReloadable(this);
        this.reloadableService = reloadableService;
        this.configurateHelper = helper;
        this.messageProviderService = messageProviderService;
        this.commandsFile = configDirectory.resolve("commands.conf");
        this.logger = logger;
        this.pluginContainer = pluginContainer;
        this.pluginInfo = pluginInfo;
    }

    @Override
    public void reset() {
        this.controlToAliases.clear();
        this.registeredAliases.clear();
        this.registeredCommands.clear();
        this.registrationComplete = false;
    }

    private String getKey(final Command command) {
        return this.getKey(new LinkedHashSet<>(), new StringBuilder(), command).toString().toLowerCase();
    }

    private StringBuilder getKey(
            final LinkedHashSet<Class<? extends ICommandExecutor>> traversed,
            final StringBuilder stringBuilder,
            final Command command) {
        if (command.parentCommand() != ICommandExecutor.class) {
            if (!traversed.add(command.parentCommand())) {
                final List<String> elements = new ArrayList<>();
                for (final Class<?> c : traversed) {
                    elements.add(c.getName());
                }
                throw new IllegalStateException("Circularity detected: " + System.lineSeparator() +
                        String.join(System.lineSeparator(), elements));
            }

            this.getKey(traversed, stringBuilder, command.parentCommand().getAnnotation(Command.class)).append(".");
        }

        return stringBuilder.append(command.aliases()[0]);
    }

    @Override
    public void registerCommands(
            final String id,
            final String name,
            final Collection<? extends Class<? extends ICommandExecutor>> associatedContext) {
        for (final Class<? extends ICommandExecutor> c : Objects.requireNonNull(associatedContext, "Module " + id + " has a null command call.")) {
            this.registerCommand(id, name, c);
        }
    }

    @Override
    public void registerCommand(
            final String id,
            final String name,
            final Class<? extends ICommandExecutor> associatedContext) {
        if (this.registrationComplete) {
            throw new IllegalStateException("Registration has completed.");
        }
        final Command command = associatedContext.getAnnotation(Command.class);
        if (command == null) {
            throw new NullPointerException("Command annotation is missing");
        }
        final String key = this.getKey(command);
        this.commandMetadataMap.put(key, new CommandMetadata(
                id,
                name,
                command,
                associatedContext,
                key,
                associatedContext.getAnnotation(EssentialsEquivalent.class)
        ));
    }

    /**
     * This is where the magic happens with registering commands. We need to:
     *
     * <ol>
     *     <li>Update command.conf</li>
     *     <li>Sift through and get the aliases to register.</li>
     *     <li>Register "root" aliases</li>
     *     <li>Then subcommands... obviously.</li>
     * </ol>
     */
    @Override
    public void completeRegistrationPhase(final INucleusServiceCollection serviceCollection,
            final RegisterCommandEvent<org.spongepowered.api.command.Command.Parameterized> event) {
        if (this.registrationComplete) {
            this.registeredCommands.values().forEach(x -> x.completeRegistration(serviceCollection));
            return;
        }
        this.registrationComplete = true;
        this.load();

        final Map<Class<? extends ICommandExecutor>, String> metadataStringMap = new HashMap<>();
        for (final Map.Entry<String, CommandMetadata> commandMetadataEntry : this.commandMetadataMap.entrySet()) {
            metadataStringMap.put(commandMetadataEntry.getValue().getExecutor(), commandMetadataEntry.getKey());
        }

        final Map<CommandMetadata, CommandControl> commands = new HashMap<>();
        // this.commandMetadataMap.values().forEach(metadata -> execToMeta.put(metadata.getExecutor(), metadata));

        final Map<Class<? extends ICommandExecutor>, Map<String, CommandMetadata>> toRegister = new HashMap<>();
        toRegister.put(ICommandExecutor.class, new HashMap<>());

        // We need aliases out
        this.mergeAliases();

        for (final CommandMetadata metadata : this.commandMetadataMap.values()) {
            // Only do this if it's enabled.
            final CommentedConfigurationNode commandNode = this.commandsConfConfigNode.node(metadata.getCommandKey());
            if (commandNode.node(ENABLED).getBoolean(false)) {
                // Get the aliases
                try {
                    final Map<String, Boolean> m = commandNode.node(ROOT_ALIASES).get(MAP_TYPE_TOKEN);
                    if (m != null) {
                            m.entrySet()
                                .stream()
                                .filter(Map.Entry::getValue)
                                .map(Map.Entry::getKey)
                                .forEach(x -> toRegister.computeIfAbsent(
                                        metadata.getCommandAnnotation().parentCommand(), y -> new HashMap<>())
                                            .put(x, metadata));
                    }
                } catch (final ConfigurateException e) {
                    e.printStackTrace();
                }

                if (!metadata.isRoot()) {
                    final String prefix =
                            metadataStringMap.get(metadata.getCommandAnnotation().parentCommand()).replace(".", " ");
                    for (final String x : metadata.getAtLevelAliases()) {
                        toRegister.computeIfAbsent(
                                metadata.getCommandAnnotation().parentCommand(),
                                y -> new HashMap<>()).put(prefix + " " + x, metadata);
                    }
                }
            }
        }

        // Now for mappings
        final Set<String> toRemove = new HashSet<>();
        for (final Map.Entry<String, String> entry : this.commandremap.entrySet()) {
            final CommentedConfigurationNode node = this.commandsConfConfigNode.node(entry.getKey()).node("enabled");
            if (node.virtual()) {
                try {
                    node.set(true).comment(serviceCollection.messageProvider().getMessageString("config.enabled"));
                } catch (final SerializationException e) {
                    // this better not happen
                    e.printStackTrace();
                }
            } else if (!node.getBoolean(true)) {
                // remove from mapping
                toRemove.add(entry.getKey());
            }
        }

        toRemove.forEach(this.commandremap::remove);
        // save();

        // use aliases to register commands.
        this.register(toRegister, commands, ICommandExecutor.class, null, serviceCollection, event);

        // Okay, now we've created our commands, time to update command conf with the modifiers.
        this.mergeModifierDefaults();
        this.save();

        // Now set the data.
        this.setupData();
    }

    private <T extends ICommandExecutor> void register(
            final Map<Class<? extends ICommandExecutor>, Map<String, CommandMetadata>> toStart,
            final Map<CommandMetadata, CommandControl> commands,
            final Class<T> keyToCheck,
            @Nullable final CommandControl parentControl,
            final INucleusServiceCollection collection,
            final RegisterCommandEvent<org.spongepowered.api.command.Command.Parameterized> event) {

        for (final Map.Entry<String, CommandMetadata> entry : toStart.get(keyToCheck).entrySet()) {
            final CommandControl control = commands.computeIfAbsent(entry.getValue(), mm -> this.construct(parentControl, mm, collection));
            this.controlToExecutorClass.putIfAbsent(entry.getValue().getExecutor(), control);
            final Class<? extends ICommandExecutor> currentKey = entry.getValue().getExecutor();
            final boolean hasKey = toStart.containsKey(currentKey);
            if (hasKey) {
                // register entries with this executor.
                this.register(toStart, commands, entry.getValue().getExecutor(), control, collection, event);
            }

            // actual parent
            if (parentControl == null || !entry.getKey().contains(" ")) {
                this.controlToAliases.computeIfAbsent(control, c -> new ArrayList<>()).add(entry.getKey());
            } else {
                final String key = entry.getKey().substring(entry.getKey().lastIndexOf(" ") + 1);
                parentControl.attach(key, control);
            }

        }

        // Now we register all root commands as necessary.
        final List<Tuple4<String, String[], org.spongepowered.api.command.Command.Parameterized, CommandControl>> builtCommands = new LinkedList<>();
        boolean tripError = false;
        if (parentControl == null) {
            for (final Map.Entry<CommandControl, List<String>> aliases : this.controlToAliases.entrySet()) {
                // Ensure that the first entry in the list is the one specified first
                final CommandControl control = aliases.getKey();
                final List<String> orderedAliases = new ArrayList<>();
                final List<String> aliasesToAdd = new ArrayList<>(aliases.getValue());
                for (final String a : control.getMetadata().getRootAliases()) {
                    if (aliases.getValue().contains(a)) {
                        orderedAliases.add(a);
                        aliasesToAdd.remove(a);
                    }
                }

                // Additions
                orderedAliases.addAll(aliasesToAdd);
                final String first = orderedAliases.get(0);
                final Collection<String> others = orderedAliases.size() > 1 ? orderedAliases.subList(1, orderedAliases.size()) :
                        Collections.emptyList();
                try {
                    builtCommands.add(Tuple.of(first, others.toArray(new String[0]), this.createCommand(aliases.getKey()), aliases.getKey()));
                } catch (final Exception e) {
                    this.logger.error("Failed to register: {}", first);
                    if (!tripError) {
                        new NucleusErrorHandler(this.pluginContainer, e, false, this.logger, this.pluginInfo)
                                .generatePrettyPrint(this.logger, Level.ERROR);
                    }
                    tripError = true;
                }
            }

            if (!tripError) {
                // Finally, register all commands
                for (final Tuple4<String, String[], org.spongepowered.api.command.Command.Parameterized, CommandControl> command : builtCommands) {
                    final CommandMapping mapping = event.register(collection.pluginContainer(), command._3, command._1, command._2).mapping();
                    command._4.setMapping(mapping);
                    this.registeredAliases.addAll(mapping.allAliases());
                }

                this.registeredCommands.putAll(commands);
            }
        }
    }

    private void mergeAliases() {
        final CommentedConfigurationNode toMerge = this.configurateHelper.createConfigNode();
        this.commandMetadataMap.values().forEach(metadata -> {
            try {
                final CommentedConfigurationNode node = toMerge.node(metadata.getCommandKey());
                final String messageKey = metadata.getCommandAnnotation().commandDescriptionKey() + ".desc";
                if (this.messageProviderService.hasKey(messageKey)) {
                    node.comment(this.messageProviderService.getMessageString(messageKey));
                }
                node.node(ENABLED).comment(this.messageProviderService.getMessageString("config.enabled")).set(true);
                final CommentedConfigurationNode al = node.node(ROOT_ALIASES);
                for (final String a : metadata.getRootAliases()) {
                    al.node(a).set(!metadata.getDisabledByDefaultRootAliases().contains(a));
                }
                if (!al.virtual()) {
                    al.comment(this.messageProviderService.getMessageString("config.rootaliases"));
                }
            } catch (final ConfigurateException e) {
                e.printStackTrace();
            }
        });

        this.commandsConfConfigNode.mergeFrom(toMerge);
    }

    private void mergeModifierDefaults() {
        final CommentedConfigurationNode toMerge = this.configurateHelper.createConfigNode();
        this.controlToAliases.keySet().forEach(control -> {
            final CommentedConfigurationNode node = toMerge.node(control.getCommandKey());
            if (!control.isModifierKeyRedirected()) { // if redirected, another command will deal with this.
                for (final Map.Entry<CommandModifier, ICommandModifier> modifier : control.getCommandModifiers().entrySet()) {
                    if (modifier.getKey().useFrom() == ICommandExecutor.class) {
                        modifier.getValue().getDefaultNode(node, this.messageProviderService);
                    }
                }
            }
        });

        this.commandsConfConfigNode.mergeFrom(toMerge);
    }

    private void setupData() {
        final List<Runnable> postponeAction = new ArrayList<>();
        this.controlToAliases.keySet().forEach(control -> {
            final CommentedConfigurationNode node;
            if (control.isModifierKeyRedirected()) {
                node = this.commandsConfConfigNode.node(control.getMetadata().getCommandAnnotation().modifierOverride());
            } else {
                node = this.commandsConfConfigNode.node(control.getCommandKey());
            }
            for (final Map.Entry<CommandModifier, ICommandModifier> modifier : control.getCommandModifiers().entrySet()) {
                if (modifier.getKey().useFrom() != ICommandExecutor.class) {
                    final CommandControl useFromControl = this.controlToExecutorClass.get(modifier.getKey().useFrom());
                    postponeAction.add(() ->
                            modifier.getValue().setValueFromOther(useFromControl.getCommandModifiersConfig(), control.getCommandModifiersConfig()));
                }
                modifier.getValue().setDataFromNode(control.getCommandModifiersConfig(), node);
            }
        });

        postponeAction.forEach(Runnable::run);
    }

    private CommandControl construct(@Nullable final CommandControl parent, final CommandMetadata metadata, final INucleusServiceCollection serviceCollection) {
        final ICommandExecutor executor = serviceCollection.injector().getInstance(metadata.getExecutor());
        if (executor instanceof IReloadableService.Reloadable) {
            this.reloadableService.registerReloadable((IReloadableService.Reloadable) executor);
        }
        return new CommandControl(
                executor,
                parent,
                metadata,
                serviceCollection
        );
    }

    @Override public Optional<CommandControl> getControl(final Class<? extends ICommandExecutor> executorClass) {
        return Optional.ofNullable(this.controlToExecutorClass.get(executorClass));
    }

    @Override public Collection<CommandControl> getCommands() {
        return this.controlToAliases.keySet();
    }

    @Override public Collection<CommandControl> getCommandsAndSubcommands() {
        return this.controlToExecutorClass.values();
    }

    @Override
    public CommandControl getControl(final String primaryAlias) {
        return this.registeredCommands.get(this.commandMetadataMap.get(primaryAlias));
    }

    @Override public void onReload(final INucleusServiceCollection serviceCollection) {
        // reload the file.
        this.load();
        this.setupData();
    }

    private void load() {
        try {
            this.commandsConfConfigNode = HoconConfigurationLoader.builder()
                    .path(this.commandsFile)
                    .build()
                    .load();
        } catch (final IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    private void save() {
        try {
            HoconConfigurationLoader.builder()
                    .path(this.commandsFile)
                    .build()
                    .save(this.commandsConfConfigNode);
        } catch (final IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    private org.spongepowered.api.command.Command.Parameterized createCommand(final CommandControl control) {
        final org.spongepowered.api.command.Command.Parameterized parameterized = control.createCommand();
        // TODO: Store
        return parameterized;
    }
}
