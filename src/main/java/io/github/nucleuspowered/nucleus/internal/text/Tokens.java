/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.internal.text;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import io.github.nucleuspowered.nucleus.Nucleus;
import io.github.nucleuspowered.nucleus.Util;
import io.github.nucleuspowered.nucleus.api.service.NucleusMessageTokenService;
import io.github.nucleuspowered.nucleus.internal.traits.MessageProviderTrait;
import io.github.nucleuspowered.nucleus.modules.core.services.UniqueUserService;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.source.ConsoleSource;
import org.spongepowered.api.command.source.RemoteSource;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.serializer.TextSerializers;
import org.spongepowered.api.world.Locatable;
import org.spongepowered.api.world.World;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.annotation.Nonnull;

public final class Tokens implements NucleusMessageTokenService.TokenParser, MessageProviderTrait {

    public static final Tokens INSTANCE = new Tokens();
    private final Map<String, Translator> translatorMap = Maps.newHashMap();

    private Tokens() {
        this.translatorMap
                .put("name", (p, v, m) -> Optional.of(Nucleus.getNucleus().getTextParsingUtils().addCommandToName(getFromVariableIfExists(p, v, m))));
        this.translatorMap.put("player", (p, v, m) -> Optional.of(Nucleus.getNucleus().getTextParsingUtils().addCommandToDisplayName(getFromVariableIfExists(p, v, m))));
        this.translatorMap.put("playername", (p, v, m) -> Optional.of(Nucleus.getNucleus().getTextParsingUtils().addCommandToDisplayName(getFromVariableIfExists(p, v, m))));

        this.translatorMap.put("prefix", (p, v, m) -> getTextFromOption(getFromVariableIfExists(p, v, m), "prefix"));
        this.translatorMap.put("suffix", (p, v, m) -> getTextFromOption(getFromVariableIfExists(p, v, m), "suffix"));

        this.translatorMap.put("playerdisplayname", (p, v, m) -> Optional.of(Nucleus.getNucleus().getTextParsingUtils().addCommandToDisplayName(getFromVariableIfExists(p, v, m))));
        this.translatorMap.put("displayname", (p, v, m) -> Optional.of(Nucleus.getNucleus().getTextParsingUtils().addCommandToDisplayName(getFromVariableIfExists(p, v, m))));

        this.translatorMap.put("maxplayers", (p, v, m) -> Optional.of(Text.of(Sponge.getServer().getMaxPlayers())));
        this.translatorMap.put("onlineplayers", (p, v, m) -> Optional.of(Text.of(Sponge.getServer().getOnlinePlayers().size())));
        this.translatorMap.put("currentworld", (p, v, m) -> Optional.of(Text.of(getWorld(getFromVariableIfExists(p, v, m)).getName())));
        this.translatorMap.put("time", (p, v, m) -> Optional.of(Text.of(Util
                .getTimeFromTicks(getWorld(getFromVariableIfExists(p, v, m)).getProperties().getWorldTime()))));

        this.translatorMap.put("uniquevisitor", (p, v, m) -> {
            long count = Nucleus.getNucleus().getInternalServiceManager().getServiceUnchecked(UniqueUserService.class).getUniqueUserCount();
            return Optional.of(Text.of(count));
        });

        this.translatorMap.put("ipaddress", (p, v, m) -> Optional.of(Text.of(p instanceof RemoteSource ?
            ((RemoteSource)p).getConnection().getAddress().getAddress().toString() :
            "localhost")));

        this.translatorMap.put("subject", (p, v, m) -> Optional.of(Text.of((p instanceof ConsoleSource ? "-" : p.getName()))));
    }

    @Nonnull @Override public Optional<Text> parse(String tokenInput, CommandSource source, Map<String, Object> variables) {
        // Token
        String[] split = tokenInput.split("\\|", 2);
        String var = "";
        if (split.length == 2) {
            var = split[1];
        }

        return this.translatorMap.getOrDefault(split[0].toLowerCase(), (p, v, m) -> Optional.empty()).get(source, var, variables);
    }

    public Set<String> getTokenNames() {
        return Sets.newHashSet(this.translatorMap.keySet());
    }

    public boolean register(String name, Translator translator, boolean primary) throws IllegalArgumentException {
        final String nameLower = name.toLowerCase();
        if (this.translatorMap.containsKey(nameLower)) {
            throw new IllegalArgumentException("Cannot register this");
        }

        this.translatorMap.put(nameLower, translator);
        return !primary || Nucleus.getNucleus().getMessageTokenService()
                .registerPrimaryToken(nameLower, Nucleus.getNucleus().getPluginContainer(), nameLower);

    }

    private static CommandSource getFromVariableIfExists(CommandSource source, String v, Map<String, Object> m) {
        if (m.containsKey(v) && m.get(v) instanceof CommandSource) {
            return (CommandSource)m.get(v);
        }

        return source;
    }

    private static World getWorld(CommandSource p) {
        World world;
        if (p instanceof Locatable) {
            world = ((Locatable) p).getWorld();
        } else {
            world = Sponge.getServer().getWorld(Sponge.getServer().getDefaultWorldName()).get();
        }

        return world;
    }

    private static Optional<Text> getTextFromOption(CommandSource cs, String option) {
        return Util.getOptionFromSubject(cs, option)
                .map(x -> x.isEmpty() ? null : TextSerializers.FORMATTING_CODE.deserialize(x));
    }

    @FunctionalInterface
    public interface Translator {

        Optional<Text> get(CommandSource source, String variableString, Map<String, Object> variables);
    }

    public static abstract class TrueFalseVariableTranslator implements Translator {

        protected abstract Optional<Text> getDefault();

        protected abstract boolean condition(CommandSource commandSource);

        public final Optional<Text> get(CommandSource source, String variableString, Map<String, Object> variables) {
            boolean res = condition(source);
            if (variableString != null && !variableString.isEmpty()) {
                if (variableString.contains("|")) {
                    String[] s = variableString.split("\\|", 2);
                    return Optional.of(res ?
                            TextSerializers.FORMATTING_CODE.deserialize(s[0]) :
                            TextSerializers.FORMATTING_CODE.deserialize(s[1]));
                }

                return Optional.ofNullable(res ?
                        TextSerializers.FORMATTING_CODE.deserialize(variableString) :
                        null);
            } else {
                return res ? getDefault() : Optional.empty();
            }
        }
    }
}
