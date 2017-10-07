/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.argumentparsers;

import io.github.nucleuspowered.nucleus.Nucleus;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.ArgumentParseException;
import org.spongepowered.api.command.args.CommandArgs;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.world.storage.WorldProperties;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nullable;

/**
 * Allows Nucleus commands to get {@link WorldProperties} of disabled worlds.
 */
public class NucleusWorldPropertiesArgument extends CommandElement {

    private final Type type;

    public NucleusWorldPropertiesArgument(@Nullable Text key, Type type) {
        super(key);
        this.type = type;
    }

    @Nullable
    @Override
    protected Object parseValue(CommandSource source, CommandArgs args) throws ArgumentParseException {
        String next = args.next();
        Optional<WorldProperties> owp = getChoices().filter(x -> x.getWorldName().equalsIgnoreCase(next)).findFirst();
        if (owp.isPresent()) {
            return owp.get();
        }

        throw args.createError(Nucleus.getNucleus().getMessageProvider().getTextMessageWithFormat(type.key, next));
    }

    @Override public List<String> complete(CommandSource src, CommandArgs args, CommandContext context) {
        return getChoices().filter(x -> {
            try {
                return x.getWorldName().toLowerCase().startsWith(args.peek());
            } catch (ArgumentParseException e) {
                return true;
            }
        }).map(WorldProperties::getWorldName).collect(Collectors.toList());
    }

    private Stream<WorldProperties> getChoices() {
        return Sponge.getServer().getAllWorldProperties().stream().filter(type.predicate);
    }

    public enum Type {
        DISABLED_ONLY(x -> !x.isEnabled(), "args.worldproperties.noexistdisabled"),
        ENABLED_ONLY(WorldProperties::isEnabled, "args.worldproperties.noexist"),
        LOADED_ONLY(x -> Sponge.getServer().getWorld(x.getUniqueId()).isPresent(), "args.worldproperties.notloaded"),
        UNLOADED_ONLY(x -> !Sponge.getServer().getWorld(x.getUniqueId()).isPresent(), "args.worldproperties.loaded"),
        ALL(x -> true, "args.worldproperties.noexist");

        private final Predicate<WorldProperties> predicate;
        private final String key;

        Type(Predicate<WorldProperties> predicate, String key) {
            this.predicate = predicate;
            this.key = key;
        }
    }
}
