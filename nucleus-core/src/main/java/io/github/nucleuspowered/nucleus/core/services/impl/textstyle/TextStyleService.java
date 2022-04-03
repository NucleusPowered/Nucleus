/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.core.services.impl.textstyle;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.github.nucleuspowered.nucleus.core.services.interfaces.IMessageProviderService;
import io.github.nucleuspowered.nucleus.core.services.interfaces.IPermissionService;
import io.github.nucleuspowered.nucleus.core.services.interfaces.ITextStyleService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.apache.logging.log4j.Logger;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.api.service.permission.Subject;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Singleton
public class TextStyleService implements ITextStyleService {

    private final Pattern colours = Pattern.compile(".*?(?<colour>(&[0-9a-flmnrok])+)$");
    private final Pattern urlParser =
            Pattern.compile("(?<first>(^|\\s))(?<reset>&r)?(?<colour>(&[0-9a-flmnrok])+)?"
                            + "(?<options>\\{[a-z]+?})?(?<url>(http(s)?://)?([A-Za-z0-9-]+\\.)+[A-Za-z0-9]{2,}\\S*)",
                    Pattern.CASE_INSENSITIVE);

    private final static TextFormat EMPTY = ITextStyleService.EMPTY;

    private final Logger logger;
    private final IPermissionService permissionService;
    private final IMessageProviderService messageProviderService;

    // I want these to be fixed names, no Sponge impl should change these.
    private final BiMap<TextColor, String> colourToPermissionSuffix;
    private final BiMap<Character, TextColor> idToColour;
    private final BiMap<TextDecoration, String> styleToPerms;
    private final BiMap<Character, TextDecoration> idToStyle;

    private final Style resetStyle;

    @Inject
    public TextStyleService(
            final IPermissionService permissionService,
            final IMessageProviderService messageProviderService,
            final Logger logger) {
        this.permissionService = permissionService;
        this.messageProviderService = messageProviderService;
        this.logger = logger;

        final Map<Character, TextDecoration> its = new HashMap<>();
        its.put('l', TextDecoration.BOLD);
        its.put('o', TextDecoration.ITALIC);
        its.put('n', TextDecoration.UNDERLINED);
        its.put('m', TextDecoration.STRIKETHROUGH);
        its.put('k', TextDecoration.OBFUSCATED);
        this.idToStyle = HashBiMap.create(its);

        final Map<TextDecoration, String> stp = new HashMap<>();
        stp.put(TextDecoration.BOLD, "bold");
        stp.put(TextDecoration.ITALIC, "italic");
        stp.put(TextDecoration.UNDERLINED, "underline");
        stp.put(TextDecoration.STRIKETHROUGH, "strikethrough");
        stp.put(TextDecoration.OBFUSCATED, "obfuscated");
        this.styleToPerms = HashBiMap.create(stp);

        final Map<Character, TextColor> itc = new HashMap<>();
        itc.put('0', NamedTextColor.BLACK);
        itc.put('1', NamedTextColor.DARK_BLUE);
        itc.put('2', NamedTextColor.DARK_GREEN);
        itc.put('3', NamedTextColor.DARK_AQUA);
        itc.put('4', NamedTextColor.DARK_RED);
        itc.put('5', NamedTextColor.DARK_PURPLE);
        itc.put('6', NamedTextColor.GOLD);
        itc.put('7', NamedTextColor.GRAY);
        itc.put('8', NamedTextColor.DARK_GRAY);
        itc.put('9', NamedTextColor.BLUE);
        itc.put('a', NamedTextColor.GREEN);
        itc.put('b', NamedTextColor.AQUA);
        itc.put('c', NamedTextColor.RED);
        itc.put('d', NamedTextColor.LIGHT_PURPLE);
        itc.put('e', NamedTextColor.YELLOW);
        itc.put('f', NamedTextColor.WHITE);
        this.idToColour = HashBiMap.create(itc);

        final Map<TextColor, String> ctps = new HashMap<>();
        ctps.put(NamedTextColor.AQUA, "aqua");
        ctps.put(NamedTextColor.BLACK, "black");
        ctps.put(NamedTextColor.BLUE, "blue");
        ctps.put(NamedTextColor.DARK_AQUA, "dark_aqua");
        ctps.put(NamedTextColor.DARK_BLUE, "dark_blue");
        ctps.put(NamedTextColor.DARK_GRAY, "dark_gray");
        ctps.put(NamedTextColor.DARK_GREEN, "dark_green");
        ctps.put(NamedTextColor.DARK_PURPLE, "dark_purple");
        ctps.put(NamedTextColor.DARK_RED, "dark_red");
        ctps.put(NamedTextColor.GOLD, "gold");
        ctps.put(NamedTextColor.GRAY, "gray");
        ctps.put(NamedTextColor.GREEN, "green");
        ctps.put(NamedTextColor.LIGHT_PURPLE, "light_purple");
        ctps.put(NamedTextColor.RED, "red");
        ctps.put(NamedTextColor.WHITE, "white");
        ctps.put(NamedTextColor.YELLOW, "yellow");
        this.colourToPermissionSuffix = HashBiMap.create(ctps);

        final Style.Builder sb = Style.style();
        for (final TextDecoration decoration : this.styleToPerms.keySet()) {
            sb.decoration(decoration, false);
        }
        this.resetStyle = sb.build();
    }

    @Override
    public Style getResetStyle() {
        return this.resetStyle;
    }

    @Override
    public Optional<String> getPermissionFor(String prefix, final TextColor colour) {
        if (!prefix.endsWith(".")) {
            prefix += ".";
        }

        final String r = this.colourToPermissionSuffix.get(colour);
        if (r == null) {
            // Use the hex string
            final String name = colour.asHexString().toLowerCase(Locale.ENGLISH);
            this.colourToPermissionSuffix.put(colour, name);
            return Optional.of(prefix + name);
        } else if (r.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(prefix + r);
    }

    @Override
    public List<String> getPermissionsFor(final String prefix, final TextDecoration style) {
        return this.getPermissionsFor(prefix, Style.style(style));
    }

    @Override
    public List<String> getPermissionsFor(String prefix, final Style style) {
        if (!prefix.endsWith(".")) {
            prefix += ".";
        }

        final List<String> builder = new ArrayList<>();
        for (final Map.Entry<TextDecoration, String> entry : this.styleToPerms.entrySet()) {
            if (style.hasDecoration(entry.getKey())) {
                builder.add(prefix + entry.getValue());
            }
        }
        return Collections.unmodifiableList(builder);
    }

    @Override
    public String stripPermissionless(final String permissionPrefixColour, final String permissionPrefixStyle, final Subject source, final String oldMessage) {
        return this.stripPermissionless(Collections.singletonList(permissionPrefixColour), permissionPrefixStyle, source, oldMessage);
    }

    @Override
    public String stripPermissionless(final String permissionPrefixColour, final String permissionPrefixColor, final String permissionPrefixStyle, final Subject source,
            final String oldMessage) {
        return this.stripPermissionless(Arrays.asList(permissionPrefixColour, permissionPrefixColor), permissionPrefixStyle, source, oldMessage);
    }

    private String stripPermissionless(final List<String> permissionPrefixColour, final String permissionPrefixStyle, final Subject source, final String oldMessage) {
        String message = oldMessage;
        if (message.contains("&")) {
            // Find the next
            final String p = this.getRegexForPermissionless(source, permissionPrefixColour, permissionPrefixStyle);
            if (p != null) {
                int oldlength;
                do {
                    oldlength = message.length();
                    message = Pattern.compile(p, Pattern.CASE_INSENSITIVE).matcher(p).replaceAll("");
                } while (oldlength != message.length());
            }
        }

        return message;
    }

    @Override
    public Collection<String> wouldStrip(final String permissionPrefixColour, final String permissionPrefixStyle, final Subject source, final String text) {
        return this.wouldStrip(Collections.singletonList(permissionPrefixColour), permissionPrefixStyle, source, text);
    }

    @Override
    public Collection<String> wouldStrip(final Collection<String> permissionPrefixColour, final String permissionPrefixStyle, final Subject source,
            final String text) {
        return this.wouldStrip(new ArrayList<>(permissionPrefixColour), permissionPrefixStyle, source, text);
    }

    private Collection<String> wouldStrip(final List<String> permissionPrefixColour,
            final String permissionPrefixStyle,
            final Subject source,
    final String oldMessage) {
        if (oldMessage.contains("&")) {
            // Find the next
            final String p = this.getRegexForPermissionless(source, permissionPrefixColour, permissionPrefixStyle);
            if (p != null) {
                // Scan the message for any of these tokens.
                final Pattern pattern = Pattern.compile(p);
                if (pattern.matcher(oldMessage).find()) {
                    final List<String> name = new ArrayList<>();
                    // We don't support these.
                    for (final char a : p.toCharArray()) {
                        if (a == '&' || a == '[' || a == ']') {
                            continue;
                        }
                        final TextColor textColor = this.idToColour.get(a);
                        if (textColor != null) {
                            name.add(textColor.toString());
                        } else {
                            final String nullableName = this.styleToPerms.get(this.idToStyle.get(a));
                            if (nullableName != null) {
                                name.add(nullableName);
                            }
                        }
                    }

                    return Collections.unmodifiableList(name);
                }
            }
        }

        return Collections.emptyList();
    }

    @Nullable
    private String getRegexForPermissionless(final Subject subject, final List<String> permissionPrefixColour, final String stylePrefix) {
        final String keys = this.getKeys(subject, permissionPrefixColour, stylePrefix);
        if (keys != null) {
            return "&[" + keys + "]";
        }

        return null;
    }

    @Nullable
    private String getKeys(final Subject subject, final List<String> permissionPrefixColour, final String stylePrefix) {
        final StringBuilder stringBuilder = new StringBuilder();
        for (final Map.Entry<TextColor, String> suffix : this.colourToPermissionSuffix.entrySet()) {
            if (permissionPrefixColour.stream().noneMatch(prefix -> {
                final String p = prefix.endsWith(".") ? prefix : prefix + ".";
                return this.permissionService.hasPermission(subject, p + suffix.getValue());
            })) {
                final Character c = this.idToColour.inverse().get(suffix.getKey());
                if (c != null) {
                    stringBuilder.append(c);
                }
            }
        }

        final String p;
        if (stylePrefix.endsWith(".")) {
            p = stylePrefix;
        } else {
            p = stylePrefix + ".";
        }

        for (final Map.Entry<TextDecoration, String> suffix : this.styleToPerms.entrySet()) {
            if (!this.permissionService.hasPermission(subject, p + suffix.getValue())) {
                final Character c = this.idToStyle.inverse().get(suffix.getKey());
                if (c != null) {
                    stringBuilder.append(c);
                }
            }
        }

        if (stringBuilder.length() > 0) {
            return stringBuilder.toString();
        }

        return null;
    }

    @Override
    public TextFormat getLastColourAndStyle(
            final Component text,
            @Nullable final TextFormat current,
            @Nullable final TextColor defaultColour,
            final Style defaultStyle) {
        final List<Component> texts = this.flatten(text);
        if (texts.isEmpty()) {
            return current == null ? new TextFormatImpl(defaultColour, defaultStyle) : current;
        }

        TextColor tc = null;
        final Style ts =  texts.get(texts.size() - 1).style();

        for (int i = texts.size() - 1; i > -1; i--) {
            // If we have both a TextComponent Colour and a TextComponent Style, then break out.
            tc = texts.get(i).color();
            if (tc != null) {
                break;
            }
        }

        if (tc == null) {
            tc = defaultColour;
        }

        if (current == null) {
            return new TextFormatImpl(tc, ts);
        }

        return new TextFormatImpl(tc != null ? tc : current.colour().orElse(null), ts);
    }

    private List<Component> flatten(final Component text) {
        final List<Component> texts = new ArrayList<>();
        texts.add(text);
        if (!text.children().isEmpty()) {
            text.children().forEach(x -> texts.addAll(this.flatten(x)));
        }

        return texts;
    }

    @Override
    public Optional<TextColor> getColourFromString(@Nullable final String s) {
        if (s == null || s.length() == 0) {
            return Optional.empty();
        }

        if (s.length() == 1) {
            return Optional.ofNullable(this.idToColour.getOrDefault(s.charAt(0), null));
        } else {
            return Optional.ofNullable(NamedTextColor.NAMES.value(s.toLowerCase()));
        }
    }

    @Override
    public Style getTextStyleFromString(@Nullable final String s) {
        if (s == null || s.length() == 0) {
            return Style.empty();
        }

        final Style.Builder ts = Style.style();
        for (final String split : s.split("\\s*,\\s*")) {
            final TextDecoration decoration;
            if (split.length() == 1) {
                decoration = this.idToStyle.get(split.charAt(0));
            } else {
                decoration = this.styleToPerms.inverse().get(split.toLowerCase());
            }
            if (decoration != null) {
                ts.apply(decoration);
            }
        }

        return ts.build();
    }

    @Override public Component addUrls(final String message) {
        return this.addUrls(message, false);
    }

    @Override public Component addUrls(final String message, final boolean replaceBlueUnderline) {
        if (message == null || message.isEmpty()) {
            return Component.empty();
        }

        final Matcher m = this.urlParser.matcher(message);
        if (!m.find()) {
            return LegacyComponentSerializer.legacyAmpersand().deserialize(message);
        }

        final List<Component> texts = new ArrayList<>();
        String remaining = message;
        ITextStyleService.TextFormat st = EMPTY;
        do {
            // We found a URL. We split on the URL that we have.
            final String[] textArray = remaining.split(this.urlParser.pattern(), 2);
            final TextComponent.Builder firstB = Component.text().style(st.style());
            if (st.colour().isPresent()) {
                firstB.color(st.colour().get());
            }
            final TextComponent first = firstB.append(LegacyComponentSerializer.legacyAmpersand().deserialize(textArray[0])).build();
            texts.add(first);

            // If we have more to do, shove it into the "remaining" variable.
            if (textArray.length == 2) {
                remaining = textArray[1];
            } else {
                remaining = null;
            }

            // Get the last colour & styles
            final String colourMatch = m.group("colour");
            if (replaceBlueUnderline) {
                st = new TextFormatImpl(NamedTextColor.BLUE, Style.style(TextDecoration.UNDERLINED));
            } else if (colourMatch != null && !colourMatch.isEmpty()) {

                // If there is a reset, explicitly do it.
                Style reset = Style.empty();
                if (m.group("reset") != null) {
                    reset = this.resetStyle;
                }

                st = this.getLastColourAndStyle(
                        Component.text().style(reset).append(
                                LegacyComponentSerializer.legacyAmpersand().deserialize(m.group("colour") + " ")).build(),
                        st);
            } else {
                st = this.getLastColourAndStyle(first, st);
            }

            // Build the URL
            final String whiteSpace = m.group("first");
            if (replaceBlueUnderline) {
                st = new TextFormatImpl(NamedTextColor.BLUE, Style.style(TextDecoration.UNDERLINED));
            } else {
                st = this.getLastColourAndStyle(first, st);
            }
            final String url = m.group("url");
            if (url.endsWith("&r")) {
                final String url2 = url.replaceAll("&r$", "");
                texts.add(this.getTextForUrl(url2, url2, whiteSpace, st, m.group("options")));
            } else {
                texts.add(this.getTextForUrl(url, url, whiteSpace, st, m.group("options")));
            }

            if (replaceBlueUnderline) {
                st = this.getLastColourAndStyle(first, st, NamedTextColor.WHITE, Style.empty());
            }
        } while (remaining != null && m.find());

        // Add the last bit.
        if (remaining != null) {
            texts.add(TextStyleService.create(null, st).append(LegacyComponentSerializer.legacyAmpersand().deserialize(remaining)).build());
        }

        // Join it all together.
        final TextComponent.Builder finalcomponent = Component.text();
        texts.forEach(finalcomponent::append);
        return finalcomponent.build();
    }

    @Override
    public Component getTextForUrl(
            final String toParse, final String msg, final String whiteSpace, final ITextStyleService.TextFormat st,
            @Nullable final String optionString) {
        try {
            final URL urlObj;
            if (!toParse.startsWith("http://") && !toParse.startsWith("https://")) {
                urlObj = new URL("http://" + toParse);
            } else {
                urlObj = new URL(toParse);
            }

            final TextComponent.Builder textBuilder = TextStyleService.create(msg, st).clickEvent(ClickEvent.openUrl(urlObj.toString()));
            if (optionString == null || !optionString.contains("h")) {
                textBuilder.hoverEvent(HoverEvent.showText(this.messageProviderService.getMessage("chat.url.click", toParse)));
            }

            if (!whiteSpace.isEmpty()) {
                return Component.text().content(whiteSpace).append(textBuilder.build()).build();
            }

            return textBuilder.build();
        } catch (final MalformedURLException e) {
            // URL parsing failed, just put the original text in here.
            this.logger.warn(this.messageProviderService.getMessageString("chat.url.malformed", toParse));
            e.printStackTrace();
            final TextComponent ret = TextStyleService.create(toParse, st).build();
            if (!whiteSpace.isEmpty()) {
                return Component.text().content(whiteSpace).append(ret).build();
            }

            return ret;
        }
    }

    @Override
    public Component oldLegacy(final String message) {
        final Matcher colourMatcher = this.colours.matcher(message);
        if (colourMatcher.matches()) {
            final TextComponent first = LegacyComponentSerializer.legacyAmpersand()
                    .deserialize(message.replace(colourMatcher.group("colour"), ""));
            final String match = colourMatcher.group("colour") + " ";
            final TextComponent t = LegacyComponentSerializer.legacyAmpersand().deserialize(match);
            return first.toBuilder().color(t.color()).style(first.style().merge(t.style())).build();
        }

        return LegacyComponentSerializer.legacyAmpersand().deserialize(message);
    }

    @Override
    public Component joinTextsWithColoursFlowing(final Component... texts) {
        final List<Component> result = new ArrayList<>();
        Component last = null;
        for (final Component n : texts) {
            if (last != null) {
                final TextFormat st = this.getLastColourAndStyle(last, null);
                final TextComponent.Builder builder = Component.text().append(n);
                st.colour().ifPresent(builder::color);
                builder.style(st.style());
                result.add(builder.build());
            } else {
                result.add(n);
            }

            last = n;
        }

        return Component.join(Component.empty(), result);
    }

    private static TextComponent.Builder create(@Nullable final String string, final TextFormat format) {
        final TextComponent.Builder builder;
        if (string == null) {
            builder = Component.text();
        } else {
            builder = Component.text().content(string);
        }
        builder.style(format.style());
        format.colour().ifPresent(builder::color);
        return builder;
    }

    public static class TextFormatImpl implements TextFormat {

        @Nullable private final TextColor colour;
        private final Style style;

        public TextFormatImpl(final TextColor colour, final Style style) {
            this.colour = colour;
            this.style = style;
        }

        @Override public Optional<TextColor> colour() {
            return Optional.ofNullable(this.colour);
        }

        @Override public Style style() {
            return this.style;
        }

        @Override public Component textOf() {
            final TextComponent.Builder builder = Component.text().content("");
            if (this.colour != null) {
                builder.color(this.colour);
            }
            builder.style(this.style);
            return builder.build();
        }

        @Override
        public TextComponent.Builder apply(final Component component) {
            final TextComponent.Builder builder = Component.text();
            if (this.colour != null) {
                builder.color(this.colour);
            }
            builder.style(this.style);
            builder.append(component);
            return builder;
        }
    }
}
