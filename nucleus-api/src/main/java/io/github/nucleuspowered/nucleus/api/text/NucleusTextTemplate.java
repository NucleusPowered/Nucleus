/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.api.text;

import io.github.nucleuspowered.nucleus.api.placeholder.NucleusPlaceholderService;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.TextRepresentable;
import org.spongepowered.api.text.TextTemplate;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import javax.annotation.Nullable;

/**
 * Represents a wrapped {@link TextTemplate} that Nucleus uses to create texts from templates.
 */
public interface NucleusTextTemplate extends TextRepresentable {

    /**
     * Whether the text is empty.
     *
     * @return <code>true</code> if so.
     */
    boolean isEmpty();

    /**
     * Gets the static {@link Text} this message will be prefixed with, if any.
     *
     * @return The text
     */
    Optional<Text> getPrefix();

    /**
     * Gets the static {@link Text} this message will be suffixed with, if any.
     *
     * @return The text
     */
    Optional<Text> getSuffix();

    /**
     * Gets the underlying {@link TextTemplate}
     *
     * @return The {@link TextTemplate}
     */
    TextTemplate getTextTemplate();

    /**
     * Returns whether there are tokens to parse.
     *
     * @return <code>true</code> if there are tokens.
     */
    boolean containsTokens();

    /**
     * Gets the {@link Text} where the tokens have been parsed from the viewpoint of the supplied {@link CommandSource}. Any unknown tokens in
     * the parsed text will be left blank.
     *
     * @param source The {@link CommandSource} that will influence what is displayed by the tokens.
     * @return The parsed {@link Text}
     */
    Text getForCommandSource(CommandSource source);

    /**
     * Gets the {@link Text} where the tokens have been parsed from the viewpoint of the supplied {@link CommandSource}. Any unknown tokens in
     * the parsed text will be left blank.
     *
     * @param source The {@link CommandSource} that will influence what is displayed by the tokens.
     * @param sender The {@link CommandSource} that can be considered the <code>{{sender}}</code>
     * @return The parsed {@link Text}
     */
    Text getForCommandSource(CommandSource source, CommandSource sender);

    /**
     * Gets the {@link Text} where the tokens have been parsed from the viewpoint of the supplied {@link CommandSource}.
     *
     * <p>
     *     By supplying a token array, these token identifiers act as additional tokens that could be encountered, and will be used above standard
     *     tokens. This is useful for having a token in a specific context, such as "displayfrom", which might only be used in a message, and is
     *     not worth registering in a {@link NucleusPlaceholderService}. They must not contain the token start or end delimiters.
     * </p>
     *
     * @param source The {@link CommandSource} that will influence what is displayed by the tokens.
     * @param tokensArray The extra tokens that can be used to parse a text.
     * @return The parsed {@link Text}
     */
    Text getForCommandSource(CommandSource source, @Nullable Map<String, Function<CommandSource, Optional<Text>>> tokensArray);

}
