/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import io.github.nucleuspowered.nucleus.modules.core.config.CoreConfigAdapter;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.source.LocatedSource;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.service.permission.option.OptionSubject;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.action.HoverAction;
import org.spongepowered.api.text.action.TextActions;
import org.spongepowered.api.text.format.TextColor;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.text.format.TextStyle;
import org.spongepowered.api.text.format.TextStyles;
import org.spongepowered.api.text.serializer.TextSerializers;
import org.spongepowered.api.world.World;

import java.net.MalformedURLException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.*;
import java.util.function.BiFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ChatUtil {

    private final Map<String, BiFunction<CommandSource, String, Text>> tokens;
    private final Map<String, BiFunction<CommandSource, String, Text>> serverTokens;

    private final Pattern playerTokenMatcher;
    private final Pattern playerTokenSplitter;

    private final Pattern serverTokenMatcher;
    private final Pattern serverTokenSplitter;

    private final Nucleus plugin;
    private final Pattern urlParser =
            Pattern.compile("(?<first>(^|\\s))(?<colour>(&[0-9a-flmnork])+)?(?<url>(http(s)?://)?([A-Za-z0-9]+\\.)+[A-Za-z0-9]{2,}\\S*)", Pattern.CASE_INSENSITIVE);

    private final String customPrefixPatten = "{{o:([a-zA-Z0-9_-]{1,15})(:s)?}}";
    private CoreConfigAdapter cca = null;

    public static final StyleTuple EMPTY = new StyleTuple(TextColors.NONE, TextStyles.NONE);

    public ChatUtil(Nucleus plugin) {
        tokens = createTokens();
        serverTokens = createServerTokens();

        Set<String> t = Sets.newHashSet(tokens.keySet());
        t.addAll(serverTokens.keySet());
        String m = getRegex(t);
        playerTokenMatcher = Pattern.compile(m, Pattern.CASE_INSENSITIVE);
        playerTokenSplitter = Pattern.compile(MessageFormat.format("(?<={0})|(?={0})", m), Pattern.CASE_INSENSITIVE);

        m = getRegex(serverTokens.keySet());
        serverTokenMatcher = Pattern.compile(m, Pattern.CASE_INSENSITIVE);
        serverTokenSplitter = Pattern.compile(MessageFormat.format("(?<={0})|(?={0})", m), Pattern.CASE_INSENSITIVE);

        this.plugin = plugin;
    }

    private String getRegex(Set<String> keys) {
        StringBuilder sb = new StringBuilder("(");
        keys.forEach(k -> sb.append(k.replaceAll("\\{\\{", "\\\\{\\\\{").replaceAll("\\}\\}", "\\\\}\\\\}")).append("|"));
        return sb.deleteCharAt(sb.length() - 1).append(")").toString();
    }

    public List<Text> getFromStrings(List<String> strings, CommandSource cs) {
        return strings.stream().map(x -> this.getServerMessageFromTemplate(x, cs, true))
                .map(x -> addUrlsToAmpersandFormattedString(TextSerializers.FORMATTING_CODE.serialize(x)))
                .collect(Collectors.toList());
    }

    @SafeVarargs
    public final Text getMessageFromTokens(String template, CommandSource cs, boolean trimTrailingSpace,
                                           boolean includePlayer, boolean includeServer, Map<String, BiFunction<CommandSource, String, Text>>... customTokens) {

        Map<String, BiFunction<CommandSource, String, Text>> map = Maps.newHashMap();
        if (includePlayer) {
            map.putAll(tokens);
        }

        if (includeServer) {
            map.putAll(serverTokens);
        }

        for (Map<String, BiFunction<CommandSource, String, Text>> customToken : customTokens) {
            map.putAll(customToken);
        }

        String m = getRegex(map.keySet());
        return getMessageFromTemplate(template, cs, trimTrailingSpace,
                Pattern.compile(MessageFormat.format("(?<={0})|(?={0})", m), Pattern.CASE_INSENSITIVE),
                Pattern.compile(m, Pattern.CASE_INSENSITIVE), map);
    }

    // String -> Text parser. Should split on all {{}} tags, but keep the tags in. We can then use the target map
    // to do the replacements!
    public Text getPlayerMessageFromTemplate(String template, CommandSource cs, boolean trimTrailingSpace) {
        return getMessageFromTemplate(template, cs, trimTrailingSpace, playerTokenSplitter, playerTokenMatcher, tokens, serverTokens);
    }

    public Text getServerMessageFromTemplate(String template, CommandSource cs, boolean trimTrailingSpace) {
        return getMessageFromTemplate(template, cs, trimTrailingSpace, serverTokenSplitter, serverTokenMatcher, serverTokens);
    }

    @SafeVarargs
    private final Text getMessageFromTemplate(String template, CommandSource cs, boolean trimTrailingSpace,
                                              Pattern splitter, Pattern matcher, Map<String, BiFunction<CommandSource, String, Text>>... tokensArray) {
        StyleTuple st = new StyleTuple(TextColors.WHITE, TextStyles.NONE);

        Map<String, BiFunction<CommandSource, String, Text>> tokens = Maps.newHashMap();
        for (Map<String, BiFunction<CommandSource, String, Text>> stringFunctionMap : tokensArray) {
            tokens.putAll(stringFunctionMap);
        }

        Text.Builder tb = Text.builder();
        for (String textElement : splitter.split(template)) {
            if (matcher.matcher(textElement).matches()) {

                // Bit hacky, but it allows the rest of the token system to work. If we get something beginning with
                // {{o: then we get the specific function out.
                String elementToUse = textElement.toLowerCase().startsWith("{{o:") ? this.customPrefixPatten : textElement.toLowerCase();

                // If we have a token, do the replacement as specified by the function
                Text message = Text.builder().color(st.colour).style(st.style)
                        .append(tokens.get(elementToUse).apply(cs, textElement)).build();
                if (!message.isEmpty()) {
                    trimTrailingSpace = false;
                    tb.append(message);
                }
            } else {
                if (trimTrailingSpace) {
                    textElement = textElement.replaceAll("^\\s+", "");
                }

                if (!textElement.isEmpty()) {
                    // Just convert the colour codes, but that's it.
                    Text r = TextSerializers.FORMATTING_CODE.deserialize(textElement);
                    tb.append(Text.of(st.colour, st.style, r));
                    st = getLastColourAndStyle(r, st);
                    trimTrailingSpace = false;
                }
            }
        }

        return tb.build();
    }

    public Text addUrlsToText(Text message) {
        return addUrlsToAmpersandFormattedString(TextSerializers.FORMATTING_CODE.serialize(message));
    }

    public Text addUrlsToAmpersandFormattedString(String message) {
        Preconditions.checkNotNull(message, "message");
        if (message.isEmpty()) {
            return Text.EMPTY;
        }

        Matcher m = urlParser.matcher(message);
        if (!m.find()) {
            return TextSerializers.FORMATTING_CODE.deserialize(message);
        }

        List<Text> texts = Lists.newArrayList();
        String remaining = message;
        StyleTuple st = ChatUtil.EMPTY;
        do {

            // We found a URL. We split on the URL that we have.
            String[] textArray = remaining.split(urlParser.pattern(), 2);
            Text first = Text.builder().color(st.colour).style(st.style)
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
            if (colourMatch != null && !colourMatch.isEmpty()) {
                first = TextSerializers.FORMATTING_CODE.deserialize(m.group("colour") + " ");
            }

            st = getLastColourAndStyle(first, st);

            // Build the URL
            String url = m.group("url");
            String toParse = TextSerializers.FORMATTING_CODE.stripCodes(url);
            String whiteSpace = m.group("first");
            if (!whiteSpace.isEmpty()) {
                url = String.join("", whiteSpace, url);
            }

            try {
                URL urlObj;
                if (!toParse.startsWith("http://") && !toParse.startsWith("https://")) {
                    urlObj = new URL("http://" + toParse);
                } else {
                    urlObj = new URL(toParse);
                }

                texts.add(Text.builder(url).color(st.colour).style(st.style)
                        .onHover(TextActions.showText(Util.getTextMessageWithFormat("chat.url.click", url)))
                        .onClick(TextActions.openUrl(urlObj))
                        .build());
            } catch (MalformedURLException e) {
                // URL parsing failed, just put the original text in here.
                initCoreConfigAdapter();

                plugin.getLogger().warn(plugin.getMessageProvider().getMessageWithFormat("chat.url.malformed", url));
                texts.add(Text.builder(url).color(st.colour).style(st.style).build());

                if (this.cca.getNodeOrDefault().isDebugmode()) {
                    e.printStackTrace();
                }
            }
        } while (remaining != null && m.find());

        // Add the last bit.
        if (remaining != null) {
            texts.add(Text.builder().color(st.colour).style(st.style)
                    .append(TextSerializers.FORMATTING_CODE.deserialize(remaining)).build());
        }

        // Join it all together.
        return Text.join(texts);
    }

    public StyleTuple getLastColourAndStyle(Text text, StyleTuple current) {
        List<Text> texts = flatten(text);
        TextColor tc = TextColors.NONE;
        TextStyle ts = TextStyles.NONE;
        for (int i = texts.size() - 1; i > -1; i--) {
            // If we have both a Text Colour and a Text Style, then break out.
            if (tc != TextColors.NONE && ts != TextStyles.NONE) {
                break;
            }
            
            if (tc == TextColors.NONE) {
                tc = texts.get(i).getColor();
            }

            if (ts == TextStyles.NONE) {
                ts = texts.get(i).getStyle();
            }
        }

        if (current == null) {
            return new StyleTuple(tc, ts);
        }

        return new StyleTuple(tc != TextColors.NONE ? tc : current.colour, ts != TextStyles.NONE ? ts : current.style);
    }

    private List<Text> flatten(Text text) {
        List<Text> texts = Lists.newArrayList(text);
        if (!text.getChildren().isEmpty()) {
            text.getChildren().forEach(x -> texts.addAll(flatten(x)));
        }

        return texts;
    }

    private Map<String, BiFunction<CommandSource, String, Text>> createTokens() {
        Map<String, BiFunction<CommandSource, String, Text>> t = new HashMap<>();

        // Token to get the option after "o:".
        t.put("{{o:([a-zA-Z0-9_-]{1,15})(:s)?}}", (p, g) -> {
            boolean addSpace = g.contains(":s");
            Text option = getTextFromOption(p, g.substring(4, g.length() - (addSpace ? 4 : 2)));
            if (addSpace && !option.isEmpty()) {
                return Text.of(option, " ");
            }

            return option;
        });

        t.put("{{name}}", (p, g) -> this.addCommandToName(p));
        t.put("{{prefix}}", (p, g) -> getTextFromOption(p, "prefix"));
        t.put("{{suffix}}", (p, g) -> getTextFromOption(p, "suffix"));
        t.put("{{displayname}}", (p, g) -> this.addCommandToDisplayName(p));

        return t;
    }

    private Map<String, BiFunction<CommandSource, String, Text>> createServerTokens() {
        Map<String, BiFunction<CommandSource, String, Text>> t = new HashMap<>();

        t.put("{{maxplayers}}", (p, g) -> Text.of(Sponge.getServer().getMaxPlayers()));
        t.put("{{onlineplayers}}", (p, g) -> Text.of(Sponge.getServer().getOnlinePlayers().size()));
        t.put("{{currentworld}}", (p, g) -> Text.of(getWorld(p).getName()));
        t.put("{{time}}", (p, g) -> Text.of(String.valueOf(Util.getTimeFromTicks(getWorld(p).getProperties().getWorldTime()))));

        return t;
    }

    private World getWorld(CommandSource p) {
        World world;
        if (p instanceof LocatedSource) {
            world = ((LocatedSource) p).getWorld();
        } else {
            world = Sponge.getServer().getWorld(Sponge.getServer().getDefaultWorldName()).get();
        }

        return world;
    }

    private Text getTextFromOption(CommandSource cs, String option) {
        if (cs instanceof Player) {
            Optional<OptionSubject> oos = Util.getSubject((Player)cs);
            if (oos.isPresent()) {
                Optional<String> optionString = oos.get().getOption(option);
                if (optionString.isPresent()) {
                    return TextSerializers.FORMATTING_CODE.deserialize(optionString.get());
                }
            }
        }

        return Text.EMPTY;
    }

    public Text addCommandToName(CommandSource p) {
        Text.Builder text = Text.builder(p.getName());
        if (p instanceof User) {
            return addCommandToNameInternal(text, (User)p);
        }

        return text.build();
    }

    public Text addCommandToDisplayName(CommandSource p) {
        Text name = getName(p);
        if (p instanceof User) {
            return addCommandToNameInternal(name, (User)p);
        }

        return name;
    }

    private Text addCommandToNameInternal(Text name, User user) {
        return addCommandToNameInternal(name.toBuilder(), user);
    }

    private Text addCommandToNameInternal(Text.Builder name, User user) {
        initCoreConfigAdapter();
        String cmd = cca.getNodeOrDefault().getCommandOnNameClick();
        if (cmd == null || cmd.isEmpty()) {
            return name.build();
        }

        if (!cmd.startsWith("/")) {
            cmd = "/" + cmd;
        }

        if (!cmd.endsWith(" ")) {
            cmd = cmd + " ";
        }

        final String commandToRun = cmd.replaceAll("\\{\\{player\\}\\}", user.getName());
        Optional<HoverAction<?>> ha = name.getHoverAction();
        Text.Builder hoverAction;
        if (ha.isPresent() && (ha.get() instanceof HoverAction.ShowText)) {
            HoverAction.ShowText h = (HoverAction.ShowText)ha.get();
            hoverAction = h.getResult().toBuilder();
            hoverAction.append(Text.NEW_LINE);
        } else {
            hoverAction = Text.builder();
        }

        hoverAction.append(Util.getTextMessageWithFormat("name.hover.command", commandToRun));
        return name.onClick(TextActions.suggestCommand(commandToRun)).onHover(TextActions.showText(hoverAction.toText())).build();
    }

    private Text getName(CommandSource cs) {
        if (cs instanceof Player) {
            return NameUtil.getName((Player)cs);
        }

        return Text.of(cs.getName());
    }

    private void initCoreConfigAdapter() {
        if (this.cca == null) {
            this.cca = plugin.getInjector().getInstance(CoreConfigAdapter.class);
        }
    }

    public static final class StyleTuple {
        public final TextColor colour;
        public final TextStyle style;

        StyleTuple(TextColor colour, TextStyle style) {
            this.colour = colour;
            this.style = style;
        }
    }
}
