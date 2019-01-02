/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.internal.traits;

import io.github.nucleuspowered.nucleus.Nucleus;
import io.github.nucleuspowered.nucleus.internal.messages.MessageProvider;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.text.Text;

import java.util.Locale;
import java.util.function.Supplier;

public interface MessageProviderTrait {

    default MessageProvider getMessageProvider() {
        return Nucleus.getNucleus().getMessageProvider();
    }

    default String getMessageString(String key, String... replacements) {
        return getMessageProvider().getMessageWithFormat(key, replacements);
    }

    default Text getMessage(String key) {
        return getMessageProvider().getTextMessageWithFormat(key);
    }

    default Text getMessage(String key, String... replacements) {
        return getMessageProvider().getTextMessageWithFormat(key, replacements);
    }

    default Text getMessage(String key, Text... replacements) {
        return getMessageProvider().getTextMessageWithTextFormat(key, replacements);
    }

    default Text getMessage(String key, Object... replacements) {
        return getMessageProvider().getTextMessageWithFormat(key, replacements);
    }

    // for future releases
    default Text getMessageFor(Locale locale, String key) {
        return getMessage(key);
    }

    default Text getMessageFor(Locale locale, String key, Object... replacements) {
        return getMessage(key, replacements);
    }

    default Text getMessageFor(Locale locale, String key, Text... replacements) {
        return getMessage(key, replacements);
    }

    default Text getMessageFor(Locale locale, String key, String... replacements) {
        return getMessage(key, replacements);
    }

    default Text getMessageFor(CommandSource source, String key) {
        return getMessageFor(source.getLocale(), key);
    }

    default Text getMessageFor(CommandSource source, String key, Object... replacements) {
        return getMessageFor(source.getLocale(), key, replacements);
    }

    default Text getMessageFor(CommandSource source, String key, Text... replacements) {
        return getMessageFor(source.getLocale(), key, replacements);
    }

    default Text getMessageFor(CommandSource source, String key, String... replacements) {
        return getMessageFor(source.getLocale(), key, replacements);
    }

    default void sendMessageTo(CommandSource receiver, String key) {
        receiver.sendMessage(getMessageFor(receiver.getLocale(), key));
    }

    default void sendMessageTo(CommandSource receiver, String key, Object... replacements) {
        receiver.sendMessage(getMessageFor(receiver.getLocale(), key, replacements));
    }

    default void sendMessageTo(CommandSource receiver, String key, Text... replacements) {
        receiver.sendMessage(getMessageFor(receiver.getLocale(), key, replacements));
    }

    default void sendMessageTo(CommandSource receiver, String key, String... replacements) {
        receiver.sendMessage(getMessageFor(receiver.getLocale(), key, replacements));
    }

    default void sendMessageTo(Supplier<CommandSource> receiver, String key, String... replacements) {
        sendMessageTo(receiver.get(), key, replacements);
    }
}
