/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.docgen.module.service;

import io.github.nucleuspowered.docgen.NucleusDocgenPlugin;
import io.github.nucleuspowered.nucleus.core.core.docgen.CommandDoc;
import io.github.nucleuspowered.nucleus.core.core.docgen.EssentialsDoc;
import io.github.nucleuspowered.nucleus.core.core.docgen.PermissionDoc;
import io.github.nucleuspowered.nucleus.core.core.docgen.TokenDoc;
import io.github.nucleuspowered.nucleus.core.scaffold.command.annotation.Command;
import io.github.nucleuspowered.nucleus.core.scaffold.command.annotation.CommandModifier;
import io.github.nucleuspowered.nucleus.core.scaffold.command.annotation.EssentialsEquivalent;
import io.github.nucleuspowered.nucleus.core.scaffold.command.control.CommandControl;
import io.github.nucleuspowered.nucleus.core.scaffold.command.control.CommandMetadata;
import io.github.nucleuspowered.nucleus.core.scaffold.command.modifier.CommandModifiers;
import io.github.nucleuspowered.nucleus.core.services.INucleusServiceCollection;
import io.github.nucleuspowered.nucleus.core.services.interfaces.ICommandMetadataService;
import io.github.nucleuspowered.nucleus.core.services.interfaces.IConfigurateHelper;
import io.github.nucleuspowered.nucleus.core.services.interfaces.IMessageProviderService;
import io.github.nucleuspowered.nucleus.core.services.interfaces.IPermissionService;
import io.github.nucleuspowered.nucleus.core.services.interfaces.data.SuggestedLevel;
import io.github.nucleuspowered.nucleus.core.util.AdventureUtils;
import io.leangen.geantyref.TypeToken;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandCause;
import org.spongepowered.api.event.CauseStackManager;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.hocon.HoconConfigurationLoader;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.yaml.NodeStyle;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class DocumentationGenerationService {

    public static final class Holder {
        public final static DocumentationGenerationService INSTANCE = new DocumentationGenerationService();
    }

    private final static TypeToken<List<CommandDoc>> COMMAND_DOC_LIST_TYPE_TOKEN = new TypeToken<List<CommandDoc>>() {};
    private final static TypeToken<List<PermissionDoc>> PERMISSION_DOC_LIST_TYPE_TOKEN = new TypeToken<List<PermissionDoc>>() {};
    private final static TypeToken<List<TokenDoc>> TOKEN_DOC_LIST_TYPE_TOKEN = new TypeToken<List<TokenDoc>>() {};
    private final static TypeToken<List<EssentialsDoc>> ESSENTIALS_DOC_LIST_TYPE_TOKEN = new TypeToken<List<EssentialsDoc>>() {};

    public void generate(final Path directory, final INucleusServiceCollection serviceCollection) throws IOException {
        try (final CauseStackManager.StackFrame stackFrame = Sponge.server().causeStackManager().pushCauseFrame()) {
            stackFrame.pushCause(Sponge.systemSubject());
            final CommandCause cause = CommandCause.create();

            final ICommandMetadataService commandMetadataService = serviceCollection.commandMetadataService();
            final IPermissionService permissionService = serviceCollection.permissionService();
            final IMessageProviderService messageProviderService = serviceCollection.messageProvider();
            final Collection<CommandControl> commands = commandMetadataService.getCommandsAndSubcommands();

            final List<EssentialsDoc> essentialsDocs = new ArrayList<>();
            final List<CommandDoc> lcd = this.getFilterAndSort(
                    commands,
                    control -> !control.getMetadata().getModuleid().equals(NucleusDocgenPlugin.MODULE_ID),
                    (CommandControl first, CommandControl second) -> {
                        final int m = first.getMetadata().getModuleid().compareToIgnoreCase(second.getMetadata().getModuleid());
                        if (m == 0) {
                            return first.getCommandKey().compareToIgnoreCase(second.getCommandKey());
                        }

                        return m;
                    },
                    control -> {
                        try (final CauseStackManager.StackFrame ignored = Sponge.server().causeStackManager().pushCauseFrame()) {
                            final CommandMetadata metadata = control.getMetadata();
                            final CommandDoc commandDoc = new CommandDoc();
                            final String cmdPath = metadata.getCommandKey().replaceAll("\\.", " ");
                            commandDoc.setCommandName(cmdPath);
                            commandDoc.setModule(metadata.getModuleid());

                            if (metadata.isRoot()) {
                                commandDoc.setAliases(String.join(", ", metadata.getAliases()));
                            } else {
                                final String key = metadata.getCommandKey()
                                        .replaceAll("\\.[a-z]+$", " ")
                                        .replaceAll("\\.", " ");
                                commandDoc.setAliases(key + Arrays.stream(metadata.getAliases())
                                        .filter(x -> !x.startsWith("#"))
                                        .map(x -> x.replace("^$", ""))
                                        .collect(Collectors.joining(", " + key)));
                                if (!metadata.getRootAliases().isEmpty()) {
                                    commandDoc.setRootAliases(String.join(", ", metadata.getRootAliases()));
                                }
                            }

                            final Set<PermissionDoc> permissionDocs = new HashSet<>();
                            final Command annotation = metadata.getCommandAnnotation();
                            for (final CommandModifier modifier : annotation.modifiers()) {
                                switch (modifier.value()) {
                                    case CommandModifiers.HAS_COOLDOWN:
                                        commandDoc.setCooldown(true);
                                        break;
                                    case CommandModifiers.HAS_COST:
                                        commandDoc.setCost(true);
                                        break;
                                    case CommandModifiers.HAS_WARMUP:
                                        commandDoc.setWarmup(true);
                                        break;
                                }
                                this.getPermissionDoc(permissionService, modifier.exemptPermission(), messageProviderService).ifPresent(permissionDocs::add);
                            }

                            for (final String perm : metadata.getCommandAnnotation().associatedPermissions()) {
                                this.getPermissionDoc(permissionService, perm, messageProviderService).ifPresent(permissionDocs::add);
                            }

                            final EssentialsEquivalent essentialsEquivalent = metadata.getEssentialsEquivalent();
                            if (essentialsEquivalent != null) {
                                final List<String> eqiv = Arrays.asList(essentialsEquivalent.value());
                                commandDoc.setEssentialsEquivalents(eqiv);
                                commandDoc.setEssNotes(essentialsEquivalent.notes());
                                commandDoc.setExactEssEquiv(essentialsEquivalent.isExact());
                                essentialsDocs.add(
                                        new EssentialsDoc()
                                                .setExact(essentialsEquivalent.isExact())
                                                .setNotes(essentialsEquivalent.notes())
                                                .setEssentialsCommands(eqiv)
                                                .setNucleusEquiv(metadata.getRootAliases())
                                );
                            }

                            final String[] base = metadata.getCommandAnnotation().basePermission();
                            SuggestedLevel level = SuggestedLevel.USER;
                            if (base.length > 0) {
                                commandDoc.setPermissionbase(base[0]);
                                for (final String permission : base) {
                                    final Optional<IPermissionService.Metadata> pm =
                                            serviceCollection.permissionService().getMetadataFor(permission);
                                    if (pm.isPresent()) {
                                        if (pm.get().getSuggestedLevel().compareTo(level) > 0) {
                                            level = pm.get().getSuggestedLevel();
                                        }
                                        permissionDocs.add(this.getFor(messageProviderService, pm.get()));
                                    }
                                }
                            }

                            commandDoc.setDefaultLevel(level.getRole());
                            commandDoc.setOneLineDescription(control.getShortDescription(cause)
                                    .map(AdventureUtils::getContent).orElse("No description provided"));
                            commandDoc.setExtendedDescription(control.getExtendedDescription(cause)
                                    .map(AdventureUtils::getContent)
                                    .orElse(null));

                            commandDoc.setUsageString(AdventureUtils.getContent(control.getUsage(CommandCause.create())));
                            commandDoc.setPermissions(new ArrayList<>(permissionDocs));
                            commandDoc.setContext(control.getContext().getValue());

                            return commandDoc;
                        }
                    });

            final List<PermissionDoc> permdocs = permissionService.getAllMetadata()
                    .stream()
                    .filter(x -> !x.getModuleId().equals(NucleusDocgenPlugin.MODULE_ID))
                    .map(x -> this.getFor(messageProviderService, x))
                    .filter(x -> x.getPermission() != null)
                    .sorted(Comparator.comparing(PermissionDoc::getPermission))
                    .collect(Collectors.toList());

            final List<TokenDoc> tokenDocs = serviceCollection
                    .placeholderService()
                    .getNucleusParsers()
                    .entrySet()
                    .stream()
                    .filter(x -> x.getValue().isDocument())
                    .filter(x -> {
                        if (!messageProviderService.hasKey("nucleus.token." + x.getValue().getToken().toLowerCase())) {
                            serviceCollection.logger().warn("Could not find message key for nucleus.token.{}", x.getValue().getToken().toLowerCase());
                            return false;
                        }
                        return true;
                    })
                    .map(x -> new TokenDoc()
                            .setId("nucleus:" + x.getKey())
                            .setName(x.getValue().getToken())
                            .setDescription(messageProviderService.getMessageString("nucleus.token." + x.getValue().getToken().toLowerCase())))
                    .collect(Collectors.toList());

            final YamlConfigurationLoader configurationLoader = YamlConfigurationLoader.builder()
                    .path(directory.resolve("commands.yml"))
                    .nodeStyle(NodeStyle.BLOCK)
                    .build();

            // Now do all the saving
            // Config files
            final Map<String, Class<?>> configs = serviceCollection
                    .configProvider()
                    .getModuleToConfigType();
            final ConfigurationNode configNode = configurationLoader.createNode();
            for (final Map.Entry<String, Class<?>> entry : configs.entrySet()) {
                try {
                    configNode.node(entry.getKey()).set(this.createConfigString(serviceCollection.configurateHelper(), entry.getValue().getDeclaredConstructor().newInstance()));
                } catch (final InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
                    e.printStackTrace();
                }
            }

            // Generate command file.
            final ConfigurationNode commandConfigurationNode = configurationLoader.createNode().set(COMMAND_DOC_LIST_TYPE_TOKEN, lcd);
            configurationLoader.save(commandConfigurationNode);

            final YamlConfigurationLoader permissionsConfigurationLoader = YamlConfigurationLoader.builder()
                    .path(directory.resolve("permissions.yml"))
                    .nodeStyle(NodeStyle.BLOCK)
                    .build();
            final ConfigurationNode permissionConfiguationNode = configurationLoader.createNode().set(PERMISSION_DOC_LIST_TYPE_TOKEN, permdocs);
            permissionsConfigurationLoader.save(permissionConfiguationNode);

            final YamlConfigurationLoader essentialsConfigurationLoader = YamlConfigurationLoader.builder()
                    .path(directory.resolve("essentials.yml"))
                    .nodeStyle(NodeStyle.BLOCK)
                    .build();
            final ConfigurationNode essentialsConfigurationNode = configurationLoader.createNode().set(ESSENTIALS_DOC_LIST_TYPE_TOKEN,
                    essentialsDocs);
            essentialsConfigurationLoader.save(essentialsConfigurationNode);

            final YamlConfigurationLoader configurationConfigurationLoader = YamlConfigurationLoader.builder()
                    .path(directory.resolve("conf.yml"))
                    .nodeStyle(NodeStyle.BLOCK)
                    .build();
            configurationConfigurationLoader.save(configNode);

            final YamlConfigurationLoader tokensConfigurationLoader = YamlConfigurationLoader.builder()
                    .path(directory.resolve("tokens.yml"))
                    .nodeStyle(NodeStyle.BLOCK)
                    .build();
            final ConfigurationNode tokensConfigNode = configurationLoader.createNode().set(TOKEN_DOC_LIST_TYPE_TOKEN, tokenDocs);
            tokensConfigurationLoader.save(tokensConfigNode);
        }
    }

    private <T, R> List<R> getFilterAndSort(
            final Collection<T> list,
            final Predicate<T> filter,
            final Comparator<T> comparator,
            final Function<T, R> mapper) {
        return list.stream().filter(filter).sorted(comparator).map(mapper).collect(Collectors.toList());
    }

    private Optional<PermissionDoc> getPermissionDoc(final IPermissionService permissionService, final String permission, final IMessageProviderService messageProviderService) {
        return permissionService.getMetadataFor(permission).map(x -> this.getFor(messageProviderService, x));
    }

    private PermissionDoc getFor(final IMessageProviderService messageProviderService, final IPermissionService.Metadata metadata) {
        return new PermissionDoc()
                .setDefaultLevel(metadata.getSuggestedLevel().getRole())
                .setDescription(metadata.getDescription(messageProviderService))
                .setPermission(metadata.getPermission())
                .setModule(metadata.getModuleId());
    }

    private String createConfigString(final IConfigurateHelper configurateHelper, final Object obj) throws IOException {
        try (final StringWriter sw = new StringWriter(); final BufferedWriter writer = new BufferedWriter(sw)) {
            final HoconConfigurationLoader hcl = HoconConfigurationLoader.builder()
                    .defaultOptions(configurateHelper.getOptions())
                    .sink(() -> writer)
                    .build();
            final CommentedConfigurationNode cn = hcl.createNode(configurateHelper.getOptions());
            this.applyToNode(obj.getClass(), obj, cn);
            hcl.save(cn);
            return sw.toString();
        }
    }

    private <T> void applyToNode(final Class<T> c, final Object object, final ConfigurationNode node) {
        try {
            node.set(TypeToken.get(c), c.cast(object));
        } catch (final SerializationException e) {
            e.printStackTrace();
        }
    }
}
