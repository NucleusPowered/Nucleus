/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.services.impl.placeholder.standard;

import io.github.nucleuspowered.nucleus.services.interfaces.IPlayerDisplayNameService;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.source.ConsoleSource;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.placeholder.PlaceholderContext;
import org.spongepowered.api.text.placeholder.PlaceholderParser;

import java.util.Optional;
import java.util.function.BiFunction;

public class NamePlaceholder implements PlaceholderParser {

    private static final Text CONSOLE = Text.of("-");
    private final IPlayerDisplayNameService playerDisplayNameService;
    private final boolean consoleFilter;
    private final BiFunction<IPlayerDisplayNameService, CommandSource, Text> parser;
    private final String id;
    private final String name;

    public NamePlaceholder(IPlayerDisplayNameService playerDisplayNameService,
            BiFunction<IPlayerDisplayNameService, CommandSource, Text> parser,
            String id,
            String name) {
        this(playerDisplayNameService, parser, id, name, false);
    }

    public NamePlaceholder(IPlayerDisplayNameService playerDisplayNameService,
            BiFunction<IPlayerDisplayNameService, CommandSource, Text> parser,
            String id,
            String name,
            boolean consoleFilter) {
        this.playerDisplayNameService = playerDisplayNameService;
        this.parser = parser;
        this.consoleFilter = consoleFilter;
        this.id = id;
        this.name = name;
    }

    @Override
    public Text parse(PlaceholderContext placeholder) {
        Optional<CommandSource> commandSource = placeholder.getAssociatedObject().filter(x -> x instanceof CommandSource).map(x -> (CommandSource) x);
        if (commandSource.isPresent()) {
            if (this.consoleFilter && placeholder.getAssociatedObject().map(x -> x instanceof ConsoleSource).isPresent()) {
                return CONSOLE;
            } else {
                return this.parser.apply(this.playerDisplayNameService, commandSource.get());
            }
        }
        return Text.EMPTY;
    }

    @Override
    public String getId() {
        return this.id;
    }

    @Override
    public String getName() {
        return this.name;
    }
}
