/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.argumentparsers;

import com.google.common.collect.ImmutableList;
import io.github.nucleuspowered.nucleus.Nucleus;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.ArgumentParseException;
import org.spongepowered.api.command.args.CommandArgs;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.text.Text;

import java.util.List;

import javax.annotation.Nullable;

/**
 * Takes an integer argument between "min" and "max".
 */
public class BoundedIntegerArgument extends CommandElement {

    private final int min;
    private final int max;

    public BoundedIntegerArgument(@Nullable Text key, int min, int max) {
        super(key);
        this.min = Math.min(min, max);
        this.max = Math.max(min, max);
    }

    @Nullable
    @Override
    protected Object parseValue(CommandSource source, CommandArgs args) throws ArgumentParseException {
        try {
            int value = Integer.parseInt(args.next());
            if (value > this.max || value < this.min) {
                throw args.createError(Nucleus.getNucleus().getMessageProvider().getTextMessageWithFormat("args.boundedinteger.outofbounds", String.valueOf(

                        this.min), String.valueOf(
                        this.max)));
            }

            return value;
        } catch (NumberFormatException e) {
            throw args.createError(Nucleus.getNucleus().getMessageProvider().getTextMessageWithFormat("args.boundedinteger.nonumber"));
        }
    }

    @Override
    public List<String> complete(CommandSource src, CommandArgs args, CommandContext context) {
        return ImmutableList.of();
    }

    @Override
    public Text getUsage(CommandSource src) {
        return Text.of(this.getKey(), String.format("(%s to %s)", this.min, this.max));
    }
}
