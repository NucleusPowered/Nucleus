/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.services.impl.docgen;

import com.google.common.reflect.TypeToken;
import io.github.nucleuspowered.nucleus.modules.core.docgen.CommandDoc;
import io.github.nucleuspowered.nucleus.modules.core.docgen.EssentialsDoc;
import io.github.nucleuspowered.nucleus.modules.core.docgen.PermissionDoc;
import io.github.nucleuspowered.nucleus.modules.core.docgen.TokenDoc;
import io.github.nucleuspowered.nucleus.scaffold.command.annotation.Command;
import io.github.nucleuspowered.nucleus.scaffold.command.annotation.CommandModifier;
import io.github.nucleuspowered.nucleus.scaffold.command.annotation.EssentialsEquivalent;
import io.github.nucleuspowered.nucleus.scaffold.command.control.CommandControl;
import io.github.nucleuspowered.nucleus.scaffold.command.control.CommandMetadata;
import io.github.nucleuspowered.nucleus.scaffold.command.modifier.CommandModifiers;
import io.github.nucleuspowered.nucleus.services.INucleusServiceCollection;
import io.github.nucleuspowered.nucleus.services.impl.placeholder.PlaceholderMetadata;
import io.github.nucleuspowered.nucleus.services.interfaces.ICommandMetadataService;
import io.github.nucleuspowered.nucleus.services.interfaces.IDocumentationGenerationService;
import io.github.nucleuspowered.nucleus.services.interfaces.IMessageProviderService;
import io.github.nucleuspowered.nucleus.services.interfaces.IPermissionService;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.ConfigurationOptions;
import ninja.leaping.configurate.SimpleConfigurationNode;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.hocon.HoconConfigurationLoader;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import ninja.leaping.configurate.yaml.YAMLConfigurationLoader;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.text.Text;
import org.yaml.snakeyaml.DumperOptions;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.StringWriter;
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
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class DocumentationGenerationService implements IDocumentationGenerationService {

    private final static TypeToken<List<CommandDoc>> COMMAND_DOC_LIST_TYPE_TOKEN = new TypeToken<List<CommandDoc>>() {};
    private final static TypeToken<List<PermissionDoc>> PERMISSION_DOC_LIST_TYPE_TOKEN = new TypeToken<List<PermissionDoc>>() {};
    private final static TypeToken<List<TokenDoc>> TOKEN_DOC_LIST_TYPE_TOKEN = new TypeToken<List<TokenDoc>>() {};
    private final static TypeToken<List<EssentialsDoc>> ESSENTIALS_DOC_LIST_TYPE_TOKEN = new TypeToken<List<EssentialsDoc>>() {};

    private final INucleusServiceCollection serviceCollection;

    @Inject
    public DocumentationGenerationService(INucleusServiceCollection serviceCollection) {
        this.serviceCollection = serviceCollection;
    }

    @Override
    public void generate(Path directory) throws IOException, ObjectMappingException {
        ICommandMetadataService commandMetadataService = this.serviceCollection.commandMetadataService();
        IPermissionService permissionService = this.serviceCollection.permissionService();
        IMessageProviderService messageProviderService = this.serviceCollection.messageProvider();
        Collection<CommandControl> commands = commandMetadataService.getCommands();

        final List<EssentialsDoc> essentialsDocs = new ArrayList<>();
        List<CommandDoc> lcd = getAndSort(
                commands,
                (CommandControl first, CommandControl second) -> {
                    int m = first.getMetadata().getModuleid().compareToIgnoreCase(second.getMetadata().getModuleid());
                    if (m == 0) {
                        return first.getCommandKey().compareToIgnoreCase(second.getCommandKey());
                    }

                    return m;
                },
                control -> {
                    CommandMetadata metadata = control.getMetadata();
                    CommandDoc commandDoc = new CommandDoc();
                    String cmdPath = metadata.getCommandKey().replaceAll("\\.", " ");
                    commandDoc.setCommandName(cmdPath);

                    if (metadata.isRoot()) {
                        commandDoc.setAliases(String.join(", ", commandDoc.getAliases()));
                    } else {
                        String key = metadata.getCommandKey().replaceAll("\\.[a-z]+$", "").replaceAll("\\.", " ");
                        commandDoc.setAliases(key + String.join(", " + key, commandDoc.getAliases()));
                        if (!metadata.getRootAliases().isEmpty()) {
                            commandDoc.setRootAliases(String.join(", ", commandDoc.getRootAliases()));
                        }
                    }

                    Set<PermissionDoc> permissionDocs = new HashSet<>();
                    Command annotation = metadata.getCommandAnnotation();
                    for (CommandModifier modifier : annotation.modifiers()) {
                        if (modifier.value().equals(CommandModifiers.HAS_COOLDOWN)) {
                            commandDoc.setCooldown(true);
                            getPermissionDoc(modifier.exemptPermission()).ifPresent(permissionDocs::add);
                        } else if (modifier.value().equals(CommandModifiers.HAS_COST)) {
                            commandDoc.setCost(true);
                            getPermissionDoc(modifier.exemptPermission()).ifPresent(permissionDocs::add);
                        } else if (modifier.value().equals(CommandModifiers.HAS_WARMUP)) {
                            commandDoc.setWarmup(true);
                            getPermissionDoc(modifier.exemptPermission()).ifPresent(permissionDocs::add);
                        }
                    }

                    EssentialsEquivalent essentialsEquivalent = metadata.getEssentialsEquivalent();
                    if (essentialsEquivalent != null) {
                        List<String> eqiv = Arrays.asList(essentialsEquivalent.value());
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

                    String[] base = metadata.getCommandAnnotation().basePermission();
                    if (base.length > 0) {
                        commandDoc.setPermissionbase(base[0]);
                        for (String permission : base) {
                            getPermissionDoc(permission).ifPresent(permissionDocs::add);
                        }
                    }

                    commandDoc.setOneLineDescription(control.getShortDescription(Sponge.getServer().getConsole())
                            .map(Text::toPlain).orElse("No description provided"));
                    commandDoc.setExtendedDescription(control.getHelp(Sponge.getServer().getConsole()).map(Text::toPlain).orElse(null));
                    commandDoc.setUsageString(control.getUsage(Sponge.getServer().getConsole()).toPlain());
                    commandDoc.setPermissions(new ArrayList<>(permissionDocs));
                    commandDoc.setSimpleUsage(control.getUsageText(Sponge.getServer().getConsole()).toPlain());
                    commandDoc.setContext(control.getContext().getValue());

                    return commandDoc;
                });

        List<PermissionDoc> permdocs = permissionService.getAllMetadata()
                .stream()
                .map(this::getFor)
                .filter(x -> x.getPermission() != null)
                .sorted(Comparator.comparing(PermissionDoc::getPermission))
                .collect(Collectors.toList());

        List<TokenDoc> tokenDocs = this.serviceCollection
                .placeholderService()
                .getNucleusParsers()
                .values()
                .stream()
                .filter(PlaceholderMetadata::isDocument)
                .filter(x -> {
                    if (!messageProviderService.hasKey("nucleus.token." + x.getToken().toLowerCase())) {
                        this.serviceCollection.logger().warn("Could not find message key for nucleus.token.{}", x.getToken().toLowerCase());
                        return false;
                    }
                    return true;
                })
                .map(x -> new TokenDoc()
                        .setId(x.getParser().getId())
                        .setName(x.getToken())
                        .setDescription(messageProviderService.getMessageString("nucleus.token." + x.getToken().toLowerCase())))
                .collect(Collectors.toList());

        // Now do all the saving
        // Config files
        Map<String, Class<?>> configs = this.serviceCollection
                .moduleDataProvider()
                .getModuleToConfigType();
        ConfigurationNode configNode = SimpleConfigurationNode.root();
        for (Map.Entry<String, Class<?>> entry : configs.entrySet()) {
            try {
                configNode.getNode(entry.getKey()).setValue(createConfigString(entry.getValue().newInstance()));
            } catch (InstantiationException | IllegalAccessException e) {
                e.printStackTrace();
            }
        }

        // Generate command file.
        YAMLConfigurationLoader configurationLoader = YAMLConfigurationLoader.builder()
                .setPath(directory.resolve("commands.yml"))
                .setFlowStyle(DumperOptions.FlowStyle.BLOCK).build();
        ConfigurationNode commandConfigurationNode = SimpleConfigurationNode.root().setValue(COMMAND_DOC_LIST_TYPE_TOKEN, lcd);
        configurationLoader.save(commandConfigurationNode);

        YAMLConfigurationLoader permissionsConfigurationLoader = YAMLConfigurationLoader.builder()
                .setPath(directory.resolve("permissions.yml"))
                .setFlowStyle(DumperOptions.FlowStyle.BLOCK).build();
        ConfigurationNode permissionConfiguationNode = SimpleConfigurationNode.root().setValue(PERMISSION_DOC_LIST_TYPE_TOKEN, permdocs);
        permissionsConfigurationLoader.save(permissionConfiguationNode);

        YAMLConfigurationLoader essentialsConfigurationLoader = YAMLConfigurationLoader.builder()
                .setPath(directory.resolve("essentials.yml"))
                .setFlowStyle(DumperOptions.FlowStyle.BLOCK).build();
        ConfigurationNode essentialsConfigurationNode = SimpleConfigurationNode.root().setValue(ESSENTIALS_DOC_LIST_TYPE_TOKEN, essentialsDocs);
        essentialsConfigurationLoader.save(essentialsConfigurationNode);

        YAMLConfigurationLoader configurationConfigurationLoader = YAMLConfigurationLoader.builder()
                .setPath(directory.resolve("conf.yml"))
                .setFlowStyle(DumperOptions.FlowStyle.BLOCK).build();
        configurationConfigurationLoader.save(configNode);

        YAMLConfigurationLoader tokensConfigurationLoader = YAMLConfigurationLoader.builder()
                .setPath(directory.resolve("tokens.yml"))
                .setFlowStyle(DumperOptions.FlowStyle.BLOCK).build();
        ConfigurationNode tokensConfigNode = SimpleConfigurationNode.root().setValue(TOKEN_DOC_LIST_TYPE_TOKEN, tokenDocs);
        tokensConfigurationLoader.save(tokensConfigNode);

    }

    private <T, R> List<R> getAndSort(
            Collection<T> list,
            Comparator<T> comparator,
            Function<T, R> mapper) {
        return list.stream().sorted(comparator).map(mapper).collect(Collectors.toList());
    }

    private Optional<PermissionDoc> getPermissionDoc(String permission) {
        return this.serviceCollection.permissionService().getMetadataFor(permission).map(this::getFor);
    }

    private PermissionDoc getFor(IPermissionService.Metadata metadata) {
        return new PermissionDoc()
                .setDefaultLevel(metadata.getSuggestedLevel().getRole())
                .setDescription(metadata.getDescription(this.serviceCollection.messageProvider()))
                .setPermission(metadata.getPermission())
                .setModule(metadata.getModuleId());
    }

    private String createConfigString(Object obj) throws IOException {
        try (StringWriter sw = new StringWriter(); BufferedWriter writer = new BufferedWriter(sw)) {
            HoconConfigurationLoader hcl = HoconConfigurationLoader.builder()
                    .setDefaultOptions(this.serviceCollection.configurateHelper().setOptions(ConfigurationOptions.defaults()))
                    .setSink(() -> writer)
                    .build();
            CommentedConfigurationNode cn = hcl.createEmptyNode(this.serviceCollection.configurateHelper().setOptions(hcl.getDefaultOptions()));
            applyToNode(obj.getClass(), obj, cn);
            hcl.save(cn);
            return sw.toString();
        }
    }

    private <T> void applyToNode(Class<T> c, Object object, ConfigurationNode node) {
        try {
            node.setValue(TypeToken.of(c), c.cast(object));
        } catch (ObjectMappingException e) {
            e.printStackTrace();
        }
    }
}
