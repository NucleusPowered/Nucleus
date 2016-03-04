/*
 * This file is part of Essence, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.essencepowered.essence.listeners;

import com.google.inject.Inject;
import io.github.essencepowered.essence.NameUtil;
import io.github.essencepowered.essence.api.PluginModule;
import io.github.essencepowered.essence.config.MainConfig;
import io.github.essencepowered.essence.internal.ListenerBase;
import io.github.essencepowered.essence.internal.annotations.Modules;
import io.github.essencepowered.essence.internal.services.datastore.UserConfigLoader;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.filter.cause.First;
import org.spongepowered.api.event.message.MessageChannelEvent;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.service.permission.option.OptionSubject;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.serializer.TextSerializers;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.regex.Pattern;

@Modules(PluginModule.CHAT)
public class ChatListener extends ListenerBase {
    private final Pattern p;
    private final Pattern match;

    private final Map<String, BiFunction<Player, Text, Text>> tokens;

    @Inject
    private MainConfig config;

    @Inject
    private UserConfigLoader loader;

    // Zero args for the injector
    public ChatListener() {
        tokens = createTokens();
        StringBuilder sb = new StringBuilder("(");
        tokens.forEach((k, v) -> sb.append(k.replaceAll("\\{", "\\\\{").replaceAll("\\}", "\\\\}")).append("|"));

        String m = sb.deleteCharAt(sb.length() - 1).append(")").toString();
        match = Pattern.compile(m, Pattern.CASE_INSENSITIVE);
        p = Pattern.compile(MessageFormat.format("(?<={0})|(?={0})", m), Pattern.CASE_INSENSITIVE);
    }

    private Map<String, BiFunction<Player, Text, Text>> createTokens() {
        Map<String, BiFunction<Player, Text, Text>> t = new HashMap<>();

        t.put("{{name}}", (p, te) -> Text.of(p.getName()));
        t.put("{{prefix}}", (p, te) -> getTextFromOption(p, "prefix"));
        t.put("{{suffix}}", (p, te) -> getTextFromOption(p, "suffix"));
        t.put("{{displayname}}", (p, te) -> NameUtil.getNameWithHover(p, loader));
        t.put("{{message}}", (p, te) -> te);
        return t;
    }

    // We do this first so that other plugins can alter it later if needs be.
    @Listener(order = Order.FIRST)
    public void onPlayerChat(MessageChannelEvent.Chat event, @First Player player) {
        if (!config.getModifyChat()) {
            return;
        }

        Text rawMessage = event.getRawMessage();

        // String -> Text parser. Should split on all {{}} tags, but keep the tags in. We can then use the target map
        // to do the replacements!
        String[] s = p.split(config.getChatTemplate());

        Text.Builder tb = Text.builder();

        boolean isEmpty = true;
        for (String textElement : s) {
            if (match.matcher(textElement).matches()) {
                // If we have a token, do the replacement as specified by the function
                Text message = tokens.get(textElement.toLowerCase()).apply(player, rawMessage);
                if (!message.isEmpty()) {
                    isEmpty = false;
                    tb.append(message);
                }
            } else {
                if (isEmpty) {
                    textElement = textElement.replaceAll("^\\s+", "");
                }

                if (!textElement.isEmpty()) {
                    // Just convert the colour codes, but that's it.
                    tb.append(TextSerializers.formattingCode('&').deserialize(textElement));
                    isEmpty = false;
                }
            }
        }

        event.setMessage(tb.build());
    }

    private Text getTextFromOption(Player player, String option) {
        Optional<OptionSubject> oos = getSubject(player);
        if (!oos.isPresent()) {
            return Text.EMPTY;
        }

        return TextSerializers.formattingCode('&').deserialize(oos.get().getOption(option).orElse(""));
    }

    private Optional<OptionSubject> getSubject(Player player) {
        Subject subject = player.getContainingCollection().get(player.getIdentifier());
        return subject instanceof OptionSubject ? Optional.of((OptionSubject) subject) : Optional.empty();
    }
}
