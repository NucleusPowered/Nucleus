/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.services.impl.placeholder;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.github.nucleuspowered.nucleus.Util;
import io.github.nucleuspowered.nucleus.modules.core.services.UniqueUserService;
import io.github.nucleuspowered.nucleus.services.IInitService;
import io.github.nucleuspowered.nucleus.services.INucleusServiceCollection;
import io.github.nucleuspowered.nucleus.services.impl.placeholder.standard.NamePlaceholder;
import io.github.nucleuspowered.nucleus.services.impl.placeholder.standard.NamedOptionPlaceholder;
import io.github.nucleuspowered.nucleus.services.impl.placeholder.standard.OptionPlaceholder;
import io.github.nucleuspowered.nucleus.services.interfaces.IPermissionService;
import io.github.nucleuspowered.nucleus.services.interfaces.IPlaceholderService;
import io.github.nucleuspowered.nucleus.services.interfaces.IPlayerDisplayNameService;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.source.RemoteSource;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.TextRepresentable;
import org.spongepowered.api.text.placeholder.PlaceholderContext;
import org.spongepowered.api.text.placeholder.PlaceholderParser;
import org.spongepowered.api.text.placeholder.PlaceholderText;
import org.spongepowered.api.world.Locatable;
import org.spongepowered.api.world.World;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class PlaceholderService implements IPlaceholderService, IInitService {

    private static final Pattern SUFFIX_PATTERN = Pattern.compile(":([sp]+)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern SEPARATOR;
    private final PlaceholderParser optionParser;
    private final PlaceholderParser emptyParser;
    private final Map<String, PlaceholderMetadata> parsers = new HashMap<>();
    private final PluginContainer pluginContainer;

    static {
        SEPARATOR = buildModifiers();
    }

    private static Pattern buildModifiers() {
        StringBuilder builder = new StringBuilder(":([");
        for (TextModifiers m : TextModifiers.values()) {
            builder.append(m.getKey());
        }
        builder.append("]+)$");
        return Pattern.compile(builder.toString());
    }

    @Inject
    public PlaceholderService(INucleusServiceCollection serviceCollection) {
        this.pluginContainer = serviceCollection.pluginContainer();
        this.optionParser = new OptionPlaceholder(serviceCollection.permissionService());
        this.emptyParser = PlaceholderParser.builder().plugin(this.pluginContainer).id("empty").parser(p -> Text.EMPTY).name("Empty parser").build();
    }

    @Override
    public void init(final INucleusServiceCollection serviceCollection) {
        // player, variables, map?
        registerToken("empty", this.emptyParser, false);
        NamePlaceholder normalName = new NamePlaceholder(
                serviceCollection.playerDisplayNameService(),
                IPlayerDisplayNameService::addCommandToName,
                "nucleus:name",
                "Nucleus Name placeholder");
        registerToken("name", normalName);
        registerToken("playername", normalName);
        registerToken("subject", new NamePlaceholder(
                serviceCollection.playerDisplayNameService(),
                IPlayerDisplayNameService::addCommandToName,
                "nucleus:subject",
                "Nucleus subject (including console) placeholder",
                true));

        NamePlaceholder displayName = new NamePlaceholder(
                serviceCollection.playerDisplayNameService(),
                IPlayerDisplayNameService::getDisplayName,
                "nucleus:displayname",
                "Nucleus subject (including console) placeholder");
        registerToken("player", displayName);
        registerToken("playerdisplayname", displayName);
        registerToken("displayname", displayName);

        IPermissionService permissionService = serviceCollection.permissionService();
        registerToken("option", this.optionParser);
        registerToken("prefix", new NamedOptionPlaceholder(permissionService, "prefix"));
        registerToken("suffix", new NamedOptionPlaceholder(permissionService, "suffix"));

        registerToken("maxplayers", PlaceholderParser.builder()
                .plugin(this.pluginContainer)
                .id("maxplayers")
                .name("Nucleus Max Players parser")
                .parser(p -> Text.of(Sponge.getServer().getMaxPlayers()))
                .build());
        registerToken("onlineplayers", PlaceholderParser.builder()
                        .plugin(this.pluginContainer)
                        .id("onlineplayers")
                        .name("Nucleus Online Player Count parser")
                        .parser(p -> Text.of(Sponge.getServer().getOnlinePlayers().size()))
                        .build());
        registerToken("currentworld", PlaceholderParser.builder()
                .plugin(this.pluginContainer)
                .id("currentworld")
                .name("Nucleus Current World parser")
                .parser(placeholder -> Text.of(getWorld(placeholder)))
                .build());
        registerToken("time",
                PlaceholderParser.builder()
                        .plugin(this.pluginContainer)
                        .id("time")
                        .name("Nucleus world time parser")
                        .parser(placeholder ->
                                Text.of(Util.getTimeFromTicks(serviceCollection.messageProvider(), getWorld(placeholder).getProperties().getWorldTime())))
                        .build());

        registerToken("uniquevisitor",
                PlaceholderParser.builder()
                        .plugin(this.pluginContainer)
                        .id("uniquevisitor")
                        .name("Nucleus unique visitor parser")
                        .parser(placeholder -> Text.of(serviceCollection.getServiceUnchecked(UniqueUserService.class).getUniqueUserCount()))
                        .build());
        registerToken("ipaddress",
                PlaceholderParser.builder()
                        .plugin(this.pluginContainer)
                        .id("ipaddress")
                        .name("Nucleus IP Address parser")
                        .parser(placeholder -> placeholder.getAssociatedObject().filter(x -> x instanceof RemoteSource)
                                .map(x -> Text.of(((RemoteSource) x).getConnection().getAddress().getAddress().toString()))
                                .orElse(Text.of("localhost")))
                        .build());
    }

    @Override
    public Set<PlaceholderParser> getParsers() {
        return this.parsers.values().stream().map(PlaceholderMetadata::getParser).collect(Collectors.toSet());
    }

    @Override
    public TextRepresentable parse(@Nullable CommandSource commandSource, String input) {
        String token = input.toLowerCase().trim().replace("{{", "").replace("}}", "");
        final Matcher m = SUFFIX_PATTERN.matcher(token);
        final List<Function<Text, Text>> modifiersCollection;
        if (m.find(0)) {
            String match = m.group(1).toLowerCase();
            modifiersCollection = new ArrayList<>();
            for (TextModifiers modifier : TextModifiers.values()) {
                if (match.contains(modifier.getKey())) {
                    modifiersCollection.add(modifier);
                }
            }

            token = token.replaceAll(SUFFIX_PATTERN.pattern(), "");
        } else {
            modifiersCollection = ImmutableList.of();
        }

        final PlaceholderParser parser;
        final PlaceholderContext context;
        if (token.startsWith("o:")) {
            if (commandSource == null) {
                return Text.EMPTY;
            }
            // option
            parser = this.optionParser;
            context = contextForSubjectAndOption(commandSource, token.substring(2));
        } else {
            final String[] s = token.split("\\|", 2);
            final String tokenIn = s[0].toLowerCase();
            final String arg = s.length == 2 ? s[1] : null;
            context = PlaceholderContext.builder()
                    .setAssociatedObject(commandSource)
                    .setArgumentString(arg)
                    .build();
            parser = getParser(tokenIn).orElse(this.emptyParser);
        }

        return new NucleusPlaceholderText(context, parser, modifiersCollection);
    }

    @Override
    public void registerToken(String tokenName, PlaceholderParser parser) {
        registerToken(tokenName, parser, true);
    }

    @Override
    public void registerToken(String tokenName, PlaceholderParser parser, boolean document) {
        if (SEPARATOR.asPredicate().test(tokenName)) {
            // can't be registered.
            throw new IllegalArgumentException("Tokens must not contain |, :, _ or space characters.");
        }
        String token = tokenName.toLowerCase();
        if (!this.parsers.containsKey(token)) {
            this.parsers.put(token, new PlaceholderMetadata(token, parser, document));
        } else {
            throw new IllegalStateException("Token " + tokenName.toLowerCase() + " has already been registered.");
        }
    }

    @Override
    public Optional<PlaceholderParser> getParser(String token) {
        if (token.contains(":")) {
            return Sponge.getRegistry().getType(PlaceholderParser.class, token);
        }
        PlaceholderMetadata placeholderMetadata = this.parsers.get(SEPARATOR.split(token.toLowerCase(), 2)[0]);
        if (placeholderMetadata == null) {
            return Optional.empty();
        }
        return Optional.of(placeholderMetadata.getParser());
    }

    @Override
    public PlaceholderParser optionParser() {
        return this.optionParser;
    }

    private PlaceholderContext contextForSubjectAndOption(Subject subject, String option) {
        return PlaceholderContext.builder().setArgumentString(option).setAssociatedObject(subject).build();
    }

    @Override
    public PlaceholderText textForSubjectAndOption(Subject subject, String option) {
        return PlaceholderText.builder()
                .setParser(this.optionParser)
                .setContext(contextForSubjectAndOption(subject, option))
                .build();
    }

    @Override public Map<String, PlaceholderMetadata> getNucleusParsers() {
        return ImmutableMap.copyOf(this.parsers);
    }

    // --

    private static World getWorld(PlaceholderContext placeholder) {
        CommandSource p = placeholder.getAssociatedObject().filter(x -> x instanceof CommandSource).map(x -> (CommandSource) x)
                        .orElseGet(Sponge.getServer()::getConsole);
        World world;
        if (p instanceof Locatable) {
            world = ((Locatable) p).getWorld();
        } else {
            world = Sponge.getServer().getWorld(Sponge.getServer().getDefaultWorldName()).get();
        }

        return world;
    }

}
