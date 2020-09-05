/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.services.impl.commandmetadata;

import com.google.common.base.Preconditions;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableList;
import com.google.common.reflect.TypeToken;
import io.github.nucleuspowered.nucleus.NucleusPluginInfo;
import io.github.nucleuspowered.nucleus.guice.ConfigDirectory;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandExecutor;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandInterceptor;
import io.github.nucleuspowered.nucleus.scaffold.command.annotation.Command;
import io.github.nucleuspowered.nucleus.scaffold.command.annotation.CommandModifier;
import io.github.nucleuspowered.nucleus.scaffold.command.annotation.EssentialsEquivalent;
import io.github.nucleuspowered.nucleus.scaffold.command.control.CommandControl;
import io.github.nucleuspowered.nucleus.scaffold.command.control.CommandMetadata;
import io.github.nucleuspowered.nucleus.scaffold.command.modifier.ICommandModifier;
import io.github.nucleuspowered.nucleus.services.INucleusServiceCollection;
import io.github.nucleuspowered.nucleus.services.interfaces.ICommandMetadataService;
import io.github.nucleuspowered.nucleus.services.interfaces.IConfigurateHelper;
import io.github.nucleuspowered.nucleus.services.interfaces.IMessageProviderService;
import io.github.nucleuspowered.nucleus.services.interfaces.IReloadableService;
import io.github.nucleuspowered.nucleus.util.Action;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.hocon.HoconConfigurationLoader;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandMapping;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.plugin.PluginContainer;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Singleton;

@SuppressWarnings({"UnstableApiUsage", "rawtypes"})
@Singleton
public class CommandMetadataService implements ICommandMetadataService, IReloadableService.Reloadable {

    private static final String ROOT_ALIASES = "root level aliases";
    private static final String ENABLED = "enabled";
    private static final TypeToken<Map<String, Boolean>> MAP_TYPE_TOKEN = new TypeToken<Map<String, Boolean>>() {};

    private final Map<String, String> commandremap = new HashMap<>();
    private final Path commandsFile;
    private final PluginContainer pluginContainer;
    private final IConfigurateHelper configurateHelper;
    private final IMessageProviderService messageProviderService;
    private final Map<String, CommandMetadata> commandMetadataMap = new HashMap<>();
    private final Map<CommandControl, List<String>> controlToAliases = new HashMap<>();
    private final BiMap<Class<? extends ICommandExecutor>, CommandControl> controlToExecutorClass = HashBiMap.create();

    private CommentedConfigurationNode commandsConfConfigNode;
    private boolean registrationComplete = false;
    private List<ICommandInterceptor> interceptors = new ArrayList<>();
    private List<String> registeredAliases = new ArrayList<>();

    @Inject
    public CommandMetadataService(@ConfigDirectory Path configDirectory,
            IReloadableService reloadableService,
            IMessageProviderService messageProviderService,
            IConfigurateHelper helper,
            PluginContainer pluginContainer) {
        reloadableService.registerReloadable(this);
        this.configurateHelper = helper;
        this.messageProviderService = messageProviderService;
        this.commandsFile = configDirectory.resolve("commands.conf");
        this.pluginContainer = pluginContainer;
    }

    private String getKey(Command command) {
        return getKey(new LinkedHashSet<>(), new StringBuilder(), command).toString().toLowerCase();
    }

    private StringBuilder getKey(
            LinkedHashSet<Class<? extends ICommandExecutor>> traversed,
            StringBuilder stringBuilder,
            Command command) {
        if (command.parentCommand() != ICommandExecutor.class) {
            if (!traversed.add(command.parentCommand())) {
                List<String> elements = new ArrayList<>();
                for (Class<?> c : traversed) {
                    elements.add(c.getName());
                }
                throw new IllegalStateException("Circularity detected: " + System.lineSeparator() +
                        String.join(System.lineSeparator(), elements));
            }

            getKey(traversed, stringBuilder, command.parentCommand().getAnnotation(Command.class)).append(".");
        }

        return stringBuilder.append(command.aliases()[0]);
    }

    @Override
    public void registerCommand(
            String id,
            String name,
            Command command,
            Class<? extends ICommandExecutor<?>> associatedContext
    ) {
        Preconditions.checkState(!this.registrationComplete, "Registration has completed.");
        String key = getKey(command);
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
    public void completeRegistrationPhase(final INucleusServiceCollection serviceCollection) {
        Preconditions.checkState(!this.registrationComplete, "Registration has completed.");
        this.registrationComplete = true;
        load();

        Map<Class<? extends ICommandExecutor<?>>, String> metadataStringMap = this.commandMetadataMap.entrySet()
                .stream()
                .collect(Collectors.toMap(
                        entry -> entry.getValue().getExecutor(),
                        Map.Entry::getKey
                ));

        Map<CommandMetadata, CommandControl> commands = new HashMap<>();
        // this.commandMetadataMap.values().forEach(metadata -> execToMeta.put(metadata.getExecutor(), metadata));

        Map<Class<? extends ICommandExecutor>, Map<String, CommandMetadata>> toRegister = new HashMap<>();
        toRegister.put(ICommandExecutor.class, new HashMap<>());

        // We need aliases out
        mergeAliases();

        for (CommandMetadata metadata : this.commandMetadataMap.values()) {
            // Only do this if it's enabled.
            CommentedConfigurationNode commandNode = this.commandsConfConfigNode.getNode(metadata.getCommandKey());
            if (commandNode.getNode(ENABLED).getBoolean(false)) {
                // Get the aliases
                try {
                    Map<String, Boolean> m = commandNode
                            .getNode(ROOT_ALIASES)
                            .getValue(MAP_TYPE_TOKEN);
                    if (m != null) {
                            m.entrySet()
                                .stream()
                                .filter(Map.Entry::getValue)
                                .map(Map.Entry::getKey)
                                .forEach(x -> toRegister.computeIfAbsent(
                                        metadata.getCommandAnnotation().parentCommand(), y -> new HashMap<>())
                                            .put(x, metadata));
                    }
                } catch (ObjectMappingException e) {
                    e.printStackTrace();
                }

                if (!metadata.isRoot()) {
                    String prefix =
                            metadataStringMap.get(metadata.getCommandAnnotation().parentCommand()).replace(".", " ");
                    for (String x : metadata.getAtLevelAliases()) {
                        toRegister.computeIfAbsent(
                                metadata.getCommandAnnotation().parentCommand(),
                                y -> new HashMap<>()).put(prefix + " " + x, metadata);
                    }
                }
            }
        }

        // Now for mappings
        Set<String> toRemove = new HashSet<>();
        for (Map.Entry<String, String> entry : this.commandremap.entrySet()) {
            CommentedConfigurationNode node = this.commandsConfConfigNode.getNode(entry.getKey()).getNode("enabled");
            if (node.isVirtual()) {
                node.setValue(true).setComment(serviceCollection.messageProvider().getMessageString("config.enabled"));
            } else if (!node.getBoolean(true)) {
                // remove from mapping
                toRemove.add(entry.getKey());
            }
        }

        toRemove.forEach(this.commandremap::remove);
        // save();

        // use aliases to register commands.
        register(toRegister, commands, ICommandExecutor.class, null, serviceCollection);

        // Okay, now we've created our commands, time to update command conf with the modifiers.
        mergeModifierDefaults();
        save();

        // Now set the data.
        setupData();
    }

    private <T extends ICommandExecutor> void register(
            Map<Class<? extends ICommandExecutor>, Map<String, CommandMetadata>> toStart,
            Map<CommandMetadata, CommandControl> commands,
            Class<T> keyToCheck,
            @Nullable CommandControl parentControl,
            INucleusServiceCollection collection) {

        for (Map.Entry<String, CommandMetadata> entry : toStart.get(keyToCheck).entrySet()) {
            CommandControl control = commands.computeIfAbsent(entry.getValue(), mm -> construct(parentControl, mm, collection));
            this.controlToExecutorClass.putIfAbsent(entry.getValue().getExecutor(), control);
            Class<? extends ICommandExecutor> currentKey = entry.getValue().getExecutor();
            boolean hasKey = toStart.containsKey(currentKey);
            if (hasKey) {
                // register entries with this executor.
                register(toStart, commands, entry.getValue().getExecutor(), control, collection);
            }

            // actual parent
            if (parentControl == null || !entry.getKey().contains(" ")) {
                this.controlToAliases.computeIfAbsent(control, c -> new ArrayList<>()).add(entry.getKey());
            } else {
                String key = entry.getKey().substring(entry.getKey().lastIndexOf(" ") + 1);
                parentControl.attach(key, control);
            }

        }

        // Now we register all root commands as necessary.
        if (parentControl == null) {
            for (Map.Entry<CommandControl, List<String>> aliases : this.controlToAliases.entrySet()) {
                // Ensure that the first entry in the list is the one specified first
                for (final String alias : new HashSet<>(aliases.getValue())) {
                    try {
                        Sponge.getCommandManager().register(collection.pluginContainer(), aliases.getKey(), alias)
                                .ifPresent(x -> this.registeredAliases.addAll(x.getAllAliases()));
                    } catch (final IllegalArgumentException ex) {
                        collection.logger().error("Could not register command {}, alias already in use by another Nucleus command.", alias);
                    }
                }

            }

            commands.values().forEach(x -> x.completeRegistration(collection));
        }
    }

    private void mergeAliases() {
        CommentedConfigurationNode toMerge = this.configurateHelper.createNode();
        this.commandMetadataMap.values().forEach(metadata -> {
            CommentedConfigurationNode node = toMerge.getNode(metadata.getCommandKey());
            String messageKey = metadata.getCommandAnnotation().commandDescriptionKey() + ".desc";
            if (this.messageProviderService.hasKey(messageKey)) {
                node.setComment(this.messageProviderService.getMessageString(messageKey));
            }
            node.getNode(ENABLED).setComment(this.messageProviderService.getMessageString("config.enabled")).setValue(true);
            CommentedConfigurationNode al = node.getNode(ROOT_ALIASES);
            for (String a : metadata.getRootAliases()) {
                al.getNode(a).setValue(!metadata.getDisabledByDefaultRootAliases().contains(a));
            }
            if (!al.isVirtual()) {
                al.setComment(this.messageProviderService.getMessageString("config.rootaliases"));
            }
        });

        this.commandsConfConfigNode.mergeValuesFrom(toMerge);
    }

    private void mergeModifierDefaults() {
        CommentedConfigurationNode toMerge = this.configurateHelper.createNode();
        this.controlToAliases.keySet().forEach(control -> {
            CommentedConfigurationNode node = toMerge.getNode(control.getCommandKey());
            if (!control.isModifierKeyRedirected()) { // if redirected, another command will deal with this.
                for (Map.Entry<CommandModifier, ICommandModifier> modifier : control.getCommandModifiers().entrySet()) {
                    if (modifier.getKey().useFrom() == ICommandExecutor.class) {
                        modifier.getValue().getDefaultNode(node, this.messageProviderService);
                    }
                }
            }
        });

        this.commandsConfConfigNode.mergeValuesFrom(toMerge);
    }

    private void setupData() {
        List<Action> postponeAction = new ArrayList<>();
        this.controlToAliases.keySet().forEach(control -> {
            CommentedConfigurationNode node;
            if (control.isModifierKeyRedirected()) {
                node = this.commandsConfConfigNode.getNode(control.getMetadata().getCommandAnnotation().modifierOverride());
            } else {
                node = this.commandsConfConfigNode.getNode(control.getCommandKey());
            }
            for (Map.Entry<CommandModifier, ICommandModifier> modifier : control.getCommandModifiers().entrySet()) {
                if (modifier.getKey().useFrom() != ICommandExecutor.class) {
                    final CommandControl useFromControl = this.controlToExecutorClass.get(modifier.getKey().useFrom());
                    postponeAction.add(() ->
                            modifier.getValue().setValueFromOther(useFromControl.getCommandModifiersConfig(), control.getCommandModifiersConfig()));
                }
                modifier.getValue().setDataFromNode(control.getCommandModifiersConfig(), node);
            }
        });

        postponeAction.forEach(Action::action);
    }

    private CommandControl construct(@Nullable CommandControl parent, CommandMetadata metadata, INucleusServiceCollection serviceCollection) {
        ICommandExecutor<?> executor = serviceCollection.injector().getInstance(metadata.getExecutor());
        return new CommandControl(
                executor,
                parent,
                metadata,
                serviceCollection
        );
    }

    @Override public void addMapping(String newCommand, String remapped) {
        if (this.commandremap.containsKey(newCommand.toLowerCase())) {
            throw new IllegalArgumentException("command already in use");
        }

        this.commandremap.put(newCommand.toLowerCase(), remapped);
    }

    @Override public void activate() {
        for (Map.Entry<String, String> entry : this.commandremap.entrySet()) {
            if (!Sponge.getCommandManager().get(entry.getKey()).isPresent()) {
                Sponge.getCommandManager().get(entry.getValue()).ifPresent(x ->
                        Sponge.getCommandManager().register(this.pluginContainer, x.getCallable(), entry.getKey()));
            }
        }
    }

    @Override public void deactivate() {
        for (Map.Entry<String, String> entry : this.commandremap.entrySet()) {
            Optional<? extends CommandMapping> mappingOptional = Sponge.getCommandManager().get(entry.getKey());
            if (mappingOptional.isPresent() &&
                    Sponge.getCommandManager().getOwner(mappingOptional.get()).map(x -> x.getId().equals(NucleusPluginInfo.ID)).orElse(false)) {
                Sponge.getCommandManager().removeMapping(mappingOptional.get());
            }
        }
    }

    @Override public boolean isNucleusCommand(String command) {
        return this.registrationComplete && this.registeredAliases.contains(command.toLowerCase());
    }

    @Override public Optional<CommandControl> getControl(Class<? extends ICommandExecutor<? extends CommandSource>> executorClass) {
        return Optional.ofNullable(this.controlToExecutorClass.get(executorClass));
    }

    @Override public Collection<CommandControl> getCommands() {
        return this.controlToAliases.keySet();
    }

    @Override public void registerInterceptor(ICommandInterceptor impl) {
        this.interceptors.add(impl);
    }

    @Override public Collection<ICommandInterceptor> interceptors() {
        return ImmutableList.copyOf(this.interceptors);
    }

    @Override public void onReload(INucleusServiceCollection serviceCollection) {
        // reload the file.
        load();
        setupData();
    }

    private void load() {
        try {
            this.commandsConfConfigNode = HoconConfigurationLoader
                    .builder()
                    .setPath(this.commandsFile)
                    .build()
                    .load();
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    private void save() {
        try {
            HoconConfigurationLoader
                    .builder()
                    .setPath(this.commandsFile)
                    .build()
                    .save(this.commandsConfConfigNode);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

}
