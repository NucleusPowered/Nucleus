/*
 * This file is part of Essence, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.essencepowered.essence.argumentparsers;

import io.github.essencepowered.essence.Util;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.ArgumentParseException;
import org.spongepowered.api.command.args.CommandArgs;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;

import javax.annotation.Nullable;
import java.util.List;
import java.util.stream.Collectors;

import static io.github.essencepowered.essence.PluginInfo.ERROR_MESSAGE_PREFIX;

public class PlayerConsoleArgument extends CommandElement {

    public PlayerConsoleArgument(@Nullable Text key) {
        super(key);
    }

    @Nullable
    @Override
    protected Object parseValue(CommandSource source, CommandArgs args) throws ArgumentParseException {
        String name = args.next();
        if (name.equals("-")) {
            return Sponge.getServer().getConsole();
        }

        return Sponge.getServer().getOnlinePlayers().stream().filter(x -> x.getName().equalsIgnoreCase(name)).findFirst().orElseThrow(() ->
            args.createError(Text.of(ERROR_MESSAGE_PREFIX, TextColors.RED, Util.getMessageWithFormat("args.playerconsole.noexist"))));
    }

    @Override
    public List<String> complete(CommandSource src, CommandArgs args, CommandContext context) {
        List<String> list = Sponge.getServer().getOnlinePlayers().stream().map(User::getName).collect(Collectors.toList());
        // Console.
        list.add("-");

        String a;
        try {
            a = args.peek();
        } catch (ArgumentParseException e) {
            return list;
        }

        return list.stream().filter(x -> x.toLowerCase().startsWith(a.toLowerCase())).collect(Collectors.toList());
    }
}
