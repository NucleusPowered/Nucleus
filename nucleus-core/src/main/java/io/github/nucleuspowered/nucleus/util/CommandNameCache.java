/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.util;

import com.google.common.collect.ImmutableSet;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandMapping;
import org.spongepowered.api.command.CommandSource;

import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.WeakHashMap;

import com.google.inject.Singleton;

@Singleton
public class CommandNameCache {

    public static final CommandNameCache INSTANCE = new CommandNameCache();

    private final Map<CommandMapping, ImmutableSet<String>> mappingSetMap = new WeakHashMap<>();

    private CommandNameCache() {}

    public Set<String> getFromCommandAndSource(String command, CommandSource source) {
        Optional<? extends CommandMapping> oc = Sponge.getCommandManager().get(command, source);
        return oc.map(CommandNameCache.INSTANCE::getLowercase).orElseGet(HashSet::new);
    }

    public Set<String> getLowercase(CommandMapping mapping) {
        return new HashSet<>(this.mappingSetMap
                .computeIfAbsent(mapping, x -> x.getAllAliases().stream().map(String::toLowerCase).collect(ImmutableSet.toImmutableSet())));
    }
}
