/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.listeners;

import com.google.inject.Inject;
import io.github.nucleuspowered.nucleus.NameUtil;
import io.github.nucleuspowered.nucleus.Util;
import io.github.nucleuspowered.nucleus.api.PluginModule;
import io.github.nucleuspowered.nucleus.config.MainConfig;
import io.github.nucleuspowered.nucleus.internal.ListenerBase;
import io.github.nucleuspowered.nucleus.internal.PermissionRegistry;
import io.github.nucleuspowered.nucleus.internal.annotations.Modules;
import io.github.nucleuspowered.nucleus.internal.permissions.PermissionInformation;
import io.github.nucleuspowered.nucleus.internal.permissions.SuggestedLevel;
import io.github.nucleuspowered.nucleus.internal.services.datastore.UserConfigLoader;
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
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Pattern;

// TODO: Revisit for Sponge API 4.0
@Modules(PluginModule.CHAT)
public class ChatListener extends ListenerBase {
    private final Pattern p;
    private final Pattern match;

    private final String prefix = PermissionRegistry.PERMISSIONS_PREFIX + "chat.";

    private final Map<String, BiFunction<Player, Text, Text>> tokens;
    private final Map<String[], Function<String, String>> replacements;

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

        replacements = createReplacements();
    }

    @Override
    protected Map<String, PermissionInformation> getPermissions() {
        Map<String, PermissionInformation> mp = new HashMap<>();
        mp.put(prefix + "color", new PermissionInformation(Util.getMessageWithFormat("permission.chat.color"), SuggestedLevel.ADMIN));
        mp.put(prefix + "colour", new PermissionInformation(Util.getMessageWithFormat("permission.chat.colour"), SuggestedLevel.ADMIN));
        mp.put(prefix + "style", new PermissionInformation(Util.getMessageWithFormat("permission.chat.style"), SuggestedLevel.ADMIN));
        mp.put(prefix + "magic", new PermissionInformation(Util.getMessageWithFormat("permission.chat.magic"), SuggestedLevel.ADMIN));
        // TODO: URLs.
        // mp.put(prefix + "url", new PermissionInformation(Util.getMessageWithFormat("permission.chat.urls"), SuggestedLevel.ADMIN));
        return super.getPermissions();
    }

    private Map<String, BiFunction<Player, Text, Text>> createTokens() {
        Map<String, BiFunction<Player, Text, Text>> t = new HashMap<>();

        t.put("{{name}}", (p, te) -> Text.of(p.getName()));
        t.put("{{prefix}}", (p, te) -> getTextFromOption(p, "prefix"));
        t.put("{{suffix}}", (p, te) -> getTextFromOption(p, "suffix"));
        t.put("{{displayname}}", (p, te) -> NameUtil.getNameWithHover(p, loader));
        t.put("{{message}}", this::useMessage);
        return t;
    }

    private Map<String[], Function<String, String>> createReplacements() {
        Map<String[], Function<String, String>> t = new HashMap<>();

        t.put(new String[] { prefix + "colour", prefix + "color" }, s -> s.replaceAll("&[0-9a-f]", ""));
        t.put(new String[] { prefix + "style" }, s -> s.replaceAll("&[lmno]", ""));
        t.put(new String[] { prefix + "magic" }, s -> s.replaceAll("&k", ""));

        return t;
    }

    private Text useMessage(Player player, Text rawMessage) {
        String m = rawMessage.toPlain();

        for (Map.Entry<String[],  Function<String, String>> r : replacements.entrySet()) {
            // If we don't have the required permission...
            if (Arrays.asList(r.getKey()).stream().noneMatch(player::hasPermission)) {
                // ...strip the codes.
                m = r.getValue().apply(m);
            }
        }

        return TextSerializers.formattingCode('&').deserialize(m);
    }

    // We do this first so that other plugins can alter it later if needs be.
    @Listener(order = Order.EARLY)
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
