/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.services.impl.textstyle;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import io.github.nucleuspowered.nucleus.modules.core.config.CoreConfig;
import io.github.nucleuspowered.nucleus.services.INucleusServiceCollection;
import io.github.nucleuspowered.nucleus.services.interfaces.IMessageProviderService;
import io.github.nucleuspowered.nucleus.services.interfaces.IPermissionService;
import io.github.nucleuspowered.nucleus.services.interfaces.IReloadableService;
import io.github.nucleuspowered.nucleus.services.interfaces.ITextStyleService;
import org.slf4j.Logger;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.TextElement;
import org.spongepowered.api.text.TextRepresentable;
import org.spongepowered.api.text.action.TextActions;
import org.spongepowered.api.text.format.TextColor;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.text.format.TextStyle;
import org.spongepowered.api.text.format.TextStyles;
import org.spongepowered.api.text.serializer.TextSerializers;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class TextStyleService implements ITextStyleService, IReloadableService.Reloadable {

    private final Pattern COLOURS = Pattern.compile(".*?(?<colour>(&[0-9a-flmnrok])+)$");
    private final Pattern URL_PARSER =
            Pattern.compile("(?<first>(^|\\s))(?<reset>&r)?(?<colour>(&[0-9a-flmnrok])+)?"
                            + "(?<options>\\{[a-z]+?})?(?<url>(http(s)?://)?([A-Za-z0-9-]+\\.)+[A-Za-z0-9]{2,}\\S*)",
                    Pattern.CASE_INSENSITIVE);

    private final static TextFormat EMPTY = new TextFormatImpl(TextColors.NONE, TextStyles.NONE);

    private final Logger logger;
    private final IPermissionService permissionService;
    private final IMessageProviderService messageProviderService;

    // I want these to be fixed names, no Sponge impl should change these.
    private final BiMap<TextColor, String> colourToPermissionSuffix =
            HashBiMap.create(ImmutableMap.<TextColor, String>builder()
                .put(TextColors.AQUA, "aqua")
                .put(TextColors.BLACK, "black")
                .put(TextColors.BLUE, "blue")
                .put(TextColors.DARK_AQUA, "dark_aqua")
                .put(TextColors.DARK_BLUE, "dark_blue")
                .put(TextColors.DARK_GRAY, "dark_gray")
                .put(TextColors.DARK_GREEN, "dark_green")
                .put(TextColors.DARK_PURPLE, "dark_purple")
                .put(TextColors.DARK_RED, "dark_red")
                .put(TextColors.GOLD, "gold")
                .put(TextColors.GRAY, "gray")
                .put(TextColors.GREEN, "green")
                .put(TextColors.LIGHT_PURPLE, "light_purple")
                .put(TextColors.NONE, "")
                .put(TextColors.RED, "red")
                .put(TextColors.RESET, "reset")
                .put(TextColors.WHITE, "white")
                .put(TextColors.YELLOW, "yellow")
                .build());

    private final BiMap<Character, TextColor> idToColour =
            HashBiMap.create(
                ImmutableMap.<Character, TextColor>builder()
                        .put('0', TextColors.BLACK)
                        .put('1', TextColors.DARK_BLUE)
                        .put('2', TextColors.DARK_GREEN)
                        .put('3', TextColors.DARK_AQUA)
                        .put('4', TextColors.DARK_RED)
                        .put('5', TextColors.DARK_PURPLE)
                        .put('6', TextColors.GOLD)
                        .put('7', TextColors.GRAY)
                        .put('8', TextColors.DARK_GRAY)
                        .put('9', TextColors.BLUE)
                        .put('a', TextColors.GREEN)
                        .put('b', TextColors.AQUA)
                        .put('c', TextColors.RED)
                        .put('d', TextColors.LIGHT_PURPLE)
                        .put('e', TextColors.YELLOW)
                        .put('f', TextColors.WHITE)
                        .put('r', TextColors.RESET)
                        .build()
            );

    private final HashBiMap<TextStyle, String> styleToPerms =
            HashBiMap.create(
            ImmutableMap.<TextStyle, String>builder()
                    .put(TextStyles.BOLD, "bold")
                    .put(TextStyles.ITALIC, "italic")
                    .put(TextStyles.UNDERLINE, "underline")
                    .put(TextStyles.STRIKETHROUGH, "strikethrough")
                    .put(TextStyles.OBFUSCATED, "obfuscated")
                    .build());
    private final BiMap<Character, TextStyle> idToStyle =
            HashBiMap.create(
                    ImmutableMap.<Character, TextStyle>builder()
                            .put('l', TextStyles.BOLD)
                            .put('o', TextStyles.ITALIC)
                            .put('n', TextStyles.UNDERLINE)
                            .put('m', TextStyles.STRIKETHROUGH)
                            .put('k', TextStyles.OBFUSCATED)
                            .build()
            );

    @Inject
    public TextStyleService(
            IPermissionService permissionService,
            IMessageProviderService messageProviderService,
            Logger logger) {
        this.permissionService = permissionService;
        this.messageProviderService = messageProviderService;
        this.logger = logger;
    }

    @Override
    public Optional<String> getPermissionFor(String prefix, TextColor colour) {
        if (!prefix.endsWith(".")) {
            prefix += ".";
        }

        String r = this.colourToPermissionSuffix.get(colour);
        if (r == null) {
            // We have a colour based on a mod, I guess?
            String[] n = colour.getId().split(":", 2);
            String name = ((n.length == 2) ? n[1] : n[0]).toLowerCase(Locale.ENGLISH);
            this.colourToPermissionSuffix.put(colour, name);
            return Optional.of(prefix + name);
        } else if (r.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(prefix + r);
    }

    @Override
    public List<String> getPermissionsFor(String prefix, TextStyle style) {
        if (!prefix.endsWith(".")) {
            prefix += ".";
        }

        String p = this.styleToPerms.get(style);
        if (p != null) {
            return ImmutableList.of(prefix + p);
        }

        ImmutableList.Builder<String> builder = ImmutableList.builder();
        addStylePermIf(
                style.hasStrikethrough().orElse(false),
                prefix,
                "strikethrough",
                builder);
        addStylePermIf(
                style.hasUnderline().orElse(false),
                prefix,
                "underline",
                builder);
        addStylePermIf(
                style.isBold().orElse(false),
                prefix,
                "bold",
                builder);
        addStylePermIf(
                style.isItalic().orElse(false),
                prefix,
                "italic",
                builder);
        addStylePermIf(
                style.isObfuscated().orElse(false),
                prefix,
                "obfuscated",
                builder);

        return builder.build();
    }

    @Override
    public String stripPermissionless(String permissionPrefixColour, String permissionPrefixStyle, Subject source, final String oldMessage) {
        return stripPermissionless(Collections.singletonList(permissionPrefixColour), permissionPrefixStyle, source, oldMessage);
    }

    @Override
    public String stripPermissionless(String permissionPrefixColour, String permissionPrefixColor, String permissionPrefixStyle, Subject source,
            final String oldMessage) {
        return stripPermissionless(Arrays.asList(permissionPrefixColour, permissionPrefixColor), permissionPrefixStyle, source, oldMessage);
    }

    private String stripPermissionless(List<String> permissionPrefixColour, String permissionPrefixStyle, Subject source, final String oldMessage) {
        String message = oldMessage;
        if (message.contains("&")) {
            // Find the next
            String p = getRegexForPermissionless(source, permissionPrefixColour, permissionPrefixStyle);
            if (p != null) {
                int oldlength;
                do {
                    oldlength = message.length();
                    message = message.replaceAll(p, "");
                } while (oldlength != message.length());
            }
        }

        return message;
    }

    @Override public Collection<String> wouldStrip(String permissionPrefixColour, String permissionPrefixStyle, Subject source, String text) {
        return wouldStrip(Collections.singletonList(permissionPrefixColour), permissionPrefixStyle, source, text);
    }

    private Collection<String> wouldStrip(List<String> permissionPrefixColour,
            String permissionPrefixStyle,
            Subject source,
            final String oldMessage) {
        if (oldMessage.contains("&")) {
            // Find the next
            String p = getKeys(source, permissionPrefixColour, permissionPrefixStyle);
            if (p != null) {
                ImmutableList.Builder<String> name = ImmutableList.builder();
                // We don't support these.
                for (char a : p.toCharArray()) {
                    TextColor textColor = this.idToColour.get(a);
                    if (textColor != null) {
                        name.add(textColor.getName());
                    } else {
                        //noinspection ConstantConditions
                        name.add(this.styleToPerms.get(this.idToStyle.get(a)));
                    }
                }

                return name.build();
            }
        }

        return ImmutableList.of();
    }

    private static void addStylePermIf(boolean condition, String prefix, String suffix, ImmutableList.Builder<String> builder) {
        if (condition) {
            builder.add(prefix + suffix);
        }
    }

    @Nullable
    private String getRegexForPermissionless(Subject subject, List<String> permissionPrefixColour, String stylePrefix) {
        String keys = getKeys(subject, permissionPrefixColour, stylePrefix);
        if (keys != null) {
            return "&[" + keys + "]";
        }

        return null;
    }

    @Nullable
    private String getKeys(Subject subject, List<String> permissionPrefixColour, String stylePrefix) {
        StringBuilder stringBuilder = new StringBuilder();
        for (Map.Entry<TextColor, String> suffix : this.colourToPermissionSuffix.entrySet()) {
            if (permissionPrefixColour.stream().noneMatch(prefix -> {
                final String p = prefix.endsWith(".") ? prefix : prefix + ".";
                return this.permissionService.hasPermission(subject, p + suffix.getValue());
            })) {
                Character c = this.idToColour.inverse().get(suffix.getKey());
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

        for (Map.Entry<TextStyle, String> suffix : this.styleToPerms.entrySet()) {
            if (!this.permissionService.hasPermission(subject, p + suffix.getValue())) {
                Character c = this.idToStyle.inverse().get(suffix.getKey());
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
    public TextFormat getLastColourAndStyle(TextRepresentable text,
            @Nullable TextFormat current,
            TextColor defaultColour,
            TextStyle defaultStyle) {
        List<Text> texts = flatten(text.toText());
        if (texts.isEmpty()) {
            return current == null ? new TextFormatImpl(defaultColour, defaultStyle) : current;
        }

        TextColor tc = TextColors.NONE;
        TextStyle ts =  texts.get(texts.size() - 1).getStyle();

        for (int i = texts.size() - 1; i > -1; i--) {
            // If we have both a Text Colour and a Text Style, then break out.
            tc = texts.get(i).getColor();
            if (tc != TextColors.NONE) {
                break;
            }
        }

        if (tc == TextColors.NONE) {
            tc = defaultColour;
        }

        if (current == null) {
            return new TextFormatImpl(tc, ts);
        }

        return new TextFormatImpl(tc != TextColors.NONE ? tc : current.colour(), ts);
    }

    private List<Text> flatten(Text text) {
        List<Text> texts = Lists.newArrayList(text);
        if (!text.getChildren().isEmpty()) {
            text.getChildren().forEach(x -> texts.addAll(flatten(x)));
        }

        return texts;
    }

    @Override
    public TextColor getColourFromString(@Nullable String s) {
        if (s == null || s.length() == 0) {
            return TextColors.NONE;
        }

        if (s.length() == 1) {
            return this.idToColour.getOrDefault(s.charAt(0), TextColors.NONE);
        } else {
            return Sponge.getRegistry().getType(TextColor.class, s.toUpperCase()).orElse(TextColors.NONE);
        }
    }

    @Override
    public TextStyle getTextStyleFromString(@Nullable String s) {
        if (s == null || s.length() == 0) {
            return TextStyles.NONE;
        }

        TextStyle ts = TextStyles.NONE;
        for (String split : s.split("\\s*,\\s*")) {
            if (split.length() == 1) {
                ts = ts.and(this.idToStyle.getOrDefault(split.charAt(0), TextStyles.NONE));
            } else {
                ts = ts.and(this.styleToPerms.inverse().getOrDefault(split.toLowerCase(), TextStyles.NONE));
            }
        }

        return ts;
    }

    @Override public Text addUrls(String message) {
        return addUrls(message, false);
    }

    @Override public Text addUrls(String message, boolean replaceBlueUnderline) {
        if (message == null || message.isEmpty()) {
            return Text.EMPTY;
        }

        Matcher m = URL_PARSER.matcher(message);
        if (!m.find()) {
            return TextSerializers.FORMATTING_CODE.deserialize(message);
        }

        List<TextElement> texts = Lists.newArrayList();
        String remaining = message;
        ITextStyleService.TextFormat st = EMPTY;
        do {
            // We found a URL. We split on the URL that we have.
            String[] textArray = remaining.split(URL_PARSER.pattern(), 2);
            Text first = Text.builder().color(st.colour()).style(st.style())
                    .append(TextSerializers.FORMATTING_CODE.deserialize(textArray[0])).build();

            // Add this text to the list regardless.
            texts.add(first);

            // If we have more to do, shove it into the "remaining" variable.
            if (textArray.length == 2) {
                remaining = textArray[1];
            } else {
                remaining = null;
            }

            // Get the last colour & styles
            String colourMatch = m.group("colour");
            if (replaceBlueUnderline) {
                st = new TextFormatImpl(TextColors.BLUE, TextStyles.UNDERLINE);
            } else if (colourMatch != null && !colourMatch.isEmpty()) {

                // If there is a reset, explicitly do it.
                TextStyle reset = TextStyles.NONE;
                if (m.group("reset") != null) {
                    reset = TextStyles.RESET;
                }

                st = getLastColourAndStyle(Text.of(reset, TextSerializers.FORMATTING_CODE.deserialize(m.group("colour") + " ")), st);
            } else {
                st = getLastColourAndStyle(first, st);
            }

            // Build the URL
            String whiteSpace = m.group("first");
            if (replaceBlueUnderline) {
                st = new TextFormatImpl(TextColors.BLUE, TextStyles.UNDERLINE);
            } else {
                st = getLastColourAndStyle(first, st);
            }
            String url = m.group("url");
            if (url.endsWith("&r")) {
                String url2 = url.replaceAll("&r$", "");
                texts.add(getTextForUrl(url2, url2, whiteSpace, st, m.group("options")));
            } else {
                texts.add(getTextForUrl(url, url, whiteSpace, st, m.group("options")));
            }

            if (replaceBlueUnderline) {
                st = getLastColourAndStyle(first, st, TextColors.WHITE, TextStyles.NONE);
            }
        } while (remaining != null && m.find());

        // Add the last bit.
        if (remaining != null) {
            texts.add(
                    Text.builder().color(st.colour()).style(st.style()).append(TextSerializers.FORMATTING_CODE.deserialize(remaining)).build());
        }

        // Join it all together.
        //noinspection SuspiciousToArrayCall,ToArrayCallWithZeroLengthArrayArgument
        return Text.of((Object[]) texts.toArray(new TextElement[texts.size()]));
    }

    @Override public Text getTextForUrl(String url, String msg, String whiteSpace, ITextStyleService.TextFormat st, @Nullable String optionString) {
        String toParse = TextSerializers.FORMATTING_CODE.stripCodes(url);

        try {
            URL urlObj;
            if (!toParse.startsWith("http://") && !toParse.startsWith("https://")) {
                urlObj = new URL("http://" + toParse);
            } else {
                urlObj = new URL(toParse);
            }

            Text.Builder textBuilder = Text.builder(msg).color(st.colour()).style(st.style()).onClick(TextActions.openUrl(urlObj));
            if (optionString == null || !optionString.contains("h")) {
                textBuilder.onHover(TextActions.showText(this.messageProviderService.getMessage("chat.url.click", url)));
            }

            if (!whiteSpace.isEmpty()) {
                return Text.builder(whiteSpace).append(textBuilder.build()).build();
            }

            return textBuilder.build();
        } catch (MalformedURLException e) {
            // URL parsing failed, just put the original text in here.
            this.logger.warn(this.messageProviderService.getMessageString("chat.url.malformed", url));
            e.printStackTrace();
            Text ret = Text.builder(url).color(st.colour()).style(st.style()).build();
            if (!whiteSpace.isEmpty()) {
                return Text.builder(whiteSpace).append(ret).build();
            }

            return ret;
        }
    }

    @Override public Text oldLegacy(String message) {
        Matcher colourMatcher = COLOURS.matcher(message);
        if (colourMatcher.matches()) {
            Text first = TextSerializers.FORMATTING_CODE.deserialize(message.replace(colourMatcher.group("colour"), ""));
            String match = colourMatcher.group("colour") + " ";
            Text t = TextSerializers.FORMATTING_CODE.deserialize(match);
            return Text.of(first, t.getColor(), first.getStyle().and(t.getStyle()));
        }

        return TextSerializers.FORMATTING_CODE.deserialize(message);
    }

    @Override public Text joinTextsWithColoursFlowing(Text... texts) {
        List<Text> result = Lists.newArrayList();
        Text last = null;
        for (Text n : texts) {
            if (last != null) {
                TextFormat st = getLastColourAndStyle(last, null);
                result.add(Text.of(st.colour(), st.style(), n));
            } else {
                result.add(n);
            }

            last = n;
        }

        return Text.join(result);
    }


    @Override public void onReload(INucleusServiceCollection serviceCollection) {
        String commandNameOnClick = serviceCollection.moduleDataProvider().getModuleConfig(CoreConfig.class).getCommandOnNameClick();
    }

    public static class TextFormatImpl implements TextFormat {

        private final TextColor colour;
        private final TextStyle style;

        TextFormatImpl(TextColor colour, TextStyle style) {
            this.colour = colour;
            this.style = style;
        }

        @Override public TextColor colour() {
            return this.colour;
        }

        @Override public TextStyle style() {
            return this.style;
        }

        @Override public Text textOf() {
            Text.Builder tb = Text.builder();
            if (this.colour != TextColors.NONE) {
                tb.color(this.colour);
            }

            tb.style(this.style);
            return tb.toText();
        }
    }
}
