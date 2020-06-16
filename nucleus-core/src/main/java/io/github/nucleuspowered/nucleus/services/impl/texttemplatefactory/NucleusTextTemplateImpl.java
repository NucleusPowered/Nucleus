/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.services.impl.texttemplatefactory;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.reflect.TypeToken;
import io.github.nucleuspowered.nucleus.api.text.NucleusTextTemplate;
import io.github.nucleuspowered.nucleus.services.INucleusServiceCollection;
import io.github.nucleuspowered.nucleus.services.interfaces.IMessageProviderService;
import io.github.nucleuspowered.nucleus.services.interfaces.ITextStyleService;
import io.github.nucleuspowered.nucleus.util.JsonConfigurateStringHelper;
import io.github.nucleuspowered.nucleus.util.Tuples;
import ninja.leaping.configurate.ConfigurationOptions;
import ninja.leaping.configurate.SimpleConfigurationNode;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import ninja.leaping.configurate.objectmapping.serialize.TypeSerializer;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.TextRepresentable;
import org.spongepowered.api.text.TextTemplate;
import org.spongepowered.api.text.action.HoverAction;
import org.spongepowered.api.text.action.TextActions;
import org.spongepowered.api.text.format.TextStyle;
import org.spongepowered.api.text.format.TextStyles;
import org.spongepowered.api.text.serializer.TextSerializers;
import org.spongepowered.api.util.Tuple;
import org.spongepowered.api.util.annotation.NonnullByDefault;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nullable;

@NonnullByDefault
public abstract class NucleusTextTemplateImpl implements NucleusTextTemplate {

    @Nullable private final Text prefix;
    @Nullable private final Text suffix;
    private final String representation;
    private final TextTemplate textTemplate;
    private final Map<String, Function<CommandSource, Text>> tokenMap = Maps.newHashMap();
    final INucleusServiceCollection serviceCollection;

    private final Pattern enhancedUrlParser =
            Pattern.compile("(?<first>(^|\\s))(?<reset>&r)?(?<colour>(&[0-9a-flmnrok])+)?"
                            + "((?<options>\\{[a-z]+?})?(?<url>(http(s)?://)?([A-Za-z0-9]+\\.)+[A-Za-z0-9-]{2,}\\S*)|"
                            + "(?<specialUrl>(\\[(?<msg>.+?)](?<optionssurl>\\{[a-z]+})?\\((?<sUrl>(http(s)?://)?([A-Za-z0-9-]+\\.)+[A-Za-z0-9]{2,}[^\\s)]*)\\)))|"
                            + "(?<specialCmd>(\\[(?<sMsg>.+?)](?<optionsscmd>\\{[a-z]+})?\\((?<sCmd>/.+?)\\))))",
                    Pattern.CASE_INSENSITIVE);

    public NucleusTextTemplateImpl(String representation,
            @Nullable Text prefix,
            @Nullable Text suffix,
            INucleusServiceCollection serviceCollection
    ) {
        this.serviceCollection = serviceCollection;
        this.representation = representation;
        Tuple<TextTemplate, Map<String, Function<CommandSource, Text>>> t = parse(representation);
        this.textTemplate = t.getFirst();

        this.tokenMap.putAll(t.getSecond());
        this.prefix = prefix;
        this.suffix = suffix;
    }

    public NucleusTextTemplateImpl(String representation, INucleusServiceCollection serviceCollection) {
        this(representation, null, null, serviceCollection);
    }

    @Override public boolean isEmpty() {
        return false;
    }

    @Override public Optional<Text> getPrefix() {
        return Optional.ofNullable(this.prefix);
    }

    @Override public Optional<Text> getSuffix() {
        return Optional.ofNullable(this.suffix);
    }

    public String getRepresentation() {
        return this.representation;
    }

    @Override public TextTemplate getTextTemplate() {
        return this.textTemplate;
    }

    abstract Tuple<TextTemplate, Map<String, Function<CommandSource, Text>>> parse(String parser);

    @Override public boolean containsTokens() {
        return !this.textTemplate.getArguments().isEmpty();
    }

    @Override
    public Text getForCommandSource(CommandSource source) {
        return getForCommandSource(source, ImmutableMap.of());
    }

    @Override
    public Text getForCommandSource(CommandSource source, CommandSource sender) {
        Optional<Text> s =
                Optional.of(this.serviceCollection.placeholderService().parse(sender, "displayname").toText());
        return getForCommandSource(source,
                ImmutableMap.<String, Function<CommandSource, Optional<Text>>>builder()
                        .put("sender", se -> s)
                        .build());
    }

    @Override @SuppressWarnings("SameParameterValue")
    public Text getForCommandSource(CommandSource source,
            @Nullable Map<String, Function<CommandSource, Optional<Text>>> tokensArray) {

        Map<String, TextTemplate.Arg> tokens = this.textTemplate.getArguments();
        Map<String, TextRepresentable> finalArgs = Maps.newHashMap();

        tokens.forEach((k, v) -> {
            String key = k.toLowerCase();

            TextRepresentable t;
            if (this.tokenMap.containsKey(key)) {
                t = this.tokenMap.get(key).apply(source);
            } else if (tokensArray != null && tokensArray.containsKey(key)) {
                t = tokensArray.get(key).apply(source).orElse(null);
            } else {
                t = this.serviceCollection.placeholderService().parse(source, key);
            }

            if (t != null) {
                finalArgs.put(k, t);
            }
        });

        Text.Builder builder = Text.builder();
        ITextStyleService.TextFormat st = null;
        if (this.prefix != null) {
            builder.append(this.prefix);
            st = this.serviceCollection.textStyleService().getLastColourAndStyle(this.prefix, null);
        }

        Text finalText = this.textTemplate.apply(finalArgs).build();

        // Don't append text if there is no text to append!
        if (!finalText.isEmpty()) {
            if (st == null) {
                builder.append(finalText);
            } else {
                builder.append(Text.builder().color(st.colour()).style(st.style()).append(finalText).build());
            }
        }

        if (this.suffix != null) {
            builder.append(this.suffix);
        }

        return builder.build();
    }

    public Text toText() {
        return this.textTemplate.toText();
    }

    Tuples.NullableTuple<List<TextRepresentable>, Map<String, Function<CommandSource, Text>>> createTextTemplateFragmentWithLinks(String message) {
        Preconditions.checkNotNull(message, "message");
        if (message.isEmpty()) {
            return new Tuples.NullableTuple<>(Lists.newArrayList(Text.EMPTY), null);
        }

        Matcher m = this.enhancedUrlParser.matcher(message);
        ITextStyleService textStyleService = this.serviceCollection.textStyleService();
        if (!m.find()) {
            return new Tuples.NullableTuple<>(Lists.newArrayList(textStyleService.oldLegacy(message)), null);
        }

        Map<String, Function<CommandSource, Text>> args = Maps.newHashMap();
        List<TextRepresentable> texts = Lists.newArrayList();
        String remaining = message;
        ITextStyleService.TextFormat st = ITextStyleService.EMPTY;
        do {
            // We found a URL. We split on the URL that we have.
            String[] textArray = remaining.split(this.enhancedUrlParser.pattern(), 2);
            TextRepresentable first = Text.builder().color(st.colour()).style(st.style())
                    .append(textStyleService.oldLegacy(textArray[0])).build();

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

                // If there is a reset, explicitly do it.
                TextStyle reset = TextStyles.NONE;
                if (m.group("reset") != null) {
                    reset = TextStyles.RESET;
                }

                first = Text.of(reset, textStyleService.oldLegacy(m.group("colour")));
            }

            st = textStyleService.getLastColourAndStyle(first, st);

            // Build the URL
            String whiteSpace = m.group("first");
            if (m.group("url") != null) {
                String url = m.group("url");
                texts.add(getTextForUrl(url, url, whiteSpace, st, m.group("options")));
            } else if (m.group("specialUrl") != null) {
                String url = m.group("sUrl");
                String msg = m.group("msg");
                texts.add(getTextForUrl(url, msg, whiteSpace, st, m.group("optionssurl")));
            } else {
                // Must be commands.
                String cmd = m.group("sCmd");
                String msg = m.group("sMsg");
                String optionList = m.group("optionsscmd");

                if (cmd.contains("{{subject}}")) {
                    String arg = UUID.randomUUID().toString();
                    args.put(arg, cs -> {
                        String command = cmd.replace("{{subject}}", cs.getName());
                        return getCmd(msg, command, optionList, whiteSpace);
                    });

                    texts.add(TextTemplate.arg(arg).color(st.colour()).style(st.style()).build());
                } else {
                    texts.add(Text.of(st.colour(), st.style(), getCmd(msg, cmd, optionList, whiteSpace)));
                }
            }
        } while (remaining != null && m.find());

        // Add the last bit.
        if (remaining != null) {
            Text.Builder tb = Text.builder().color(st.colour()).style(st.style()).append(TextSerializers.FORMATTING_CODE.deserialize(remaining));
            if (remaining.matches("^\\s+&r.*")) {
                tb.style(TextStyles.RESET);
            }

            texts.add(tb.build());
        }

        // Return the list.
        return new Tuples.NullableTuple<>(texts, args);
    }

    private Text getCmd(String msg, String cmd, @org.checkerframework.checker.nullness.qual.Nullable String optionList, String whiteSpace) {
        Text.Builder textBuilder = Text.builder(msg)
                .onClick(TextActions.runCommand(cmd))
                .onHover(setupHoverOnCmd(cmd, optionList));
        if (optionList != null && optionList.contains("s")) {
            textBuilder.onClick(TextActions.suggestCommand(cmd));
        }

        Text toAdd = textBuilder.build();
        if (!whiteSpace.isEmpty()) {
            toAdd = Text.join(Text.of(whiteSpace), toAdd);
        }

        return toAdd;
    }

    @Nullable
    private HoverAction<?> setupHoverOnCmd(String cmd, @Nullable String optionList) {
        if (optionList != null) {
            if (optionList.contains("h")) {
                return null;
            }

            if (optionList.contains("s")) {
                return TextActions.showText(this.serviceCollection.messageProvider().getMessage("chat.command.clicksuggest", cmd));
            }
        }

        return TextActions.showText(this.serviceCollection.messageProvider().getMessage("chat.command.click", cmd));
    }

    private Text getTextForUrl(String url, String msg, String whiteSpace, ITextStyleService.TextFormat st, @Nullable String optionString) {
        String toParse = TextSerializers.FORMATTING_CODE.stripCodes(url);
        IMessageProviderService messageProviderService = this.serviceCollection.messageProvider();

        try {
            URL urlObj;
            if (!toParse.startsWith("http://") && !toParse.startsWith("https://")) {
                urlObj = new URL("http://" + toParse);
            } else {
                urlObj = new URL(toParse);
            }

            Text.Builder textBuilder = Text.builder(msg).color(st.colour()).style(st.style()).onClick(TextActions.openUrl(urlObj));
            if (optionString == null || !optionString.contains("h")) {
                textBuilder.onHover(TextActions.showText(messageProviderService.getMessage("chat.url.click", url)));
            }

            if (!whiteSpace.isEmpty()) {
                return Text.builder(whiteSpace).append(textBuilder.build()).build();
            }

            return textBuilder.build();
        } catch (MalformedURLException e) {
            // URL parsing failed, just put the original text in here.
            this.serviceCollection.logger().warn(messageProviderService.getMessageString("chat.url.malformed", url));
            e.printStackTrace();
            
            Text ret = Text.builder(url).color(st.colour()).style(st.style()).build();
            if (!whiteSpace.isEmpty()) {
                return Text.builder(whiteSpace).append(ret).build();
            }

            return ret;
        }
    }

    /**
     * Creates a {@link TextTemplate} from an Ampersand encoded string.
     */
    static class Ampersand extends NucleusTextTemplateImpl {

        private static final Pattern pattern =
            Pattern.compile("(?<url>\\[[^\\[]+]\\(/[^)]*?)?(?<match>\\{\\{(?!subject)(?<name>[^\\s{}]+)}})"
                    + "(?<urltwo>[^(]*?\\))?");

        Ampersand(String representation, INucleusServiceCollection serviceCollection) {
            super(representation, serviceCollection);
        }

        Ampersand(String representation, @Nullable Text prefix, @Nullable Text suffix, INucleusServiceCollection serviceCollection) {
            super(representation, prefix, suffix, serviceCollection);
        }

        @Override Tuple<TextTemplate, Map<String, Function<CommandSource, Text>>> parse(final String string) {
            // regex!
            Matcher mat = pattern.matcher(string);
            List<String> map = Lists.newArrayList();

            List<String> s = Lists.newArrayList(pattern.split(string));
            int index = 0;

            while (mat.find()) {
                if (mat.group("url") != null && mat.group("urltwo") != null) {
                    String toUpdate = s.get(index);
                    toUpdate = toUpdate + mat.group();
                    if (s.size() < index + 1) {
                        toUpdate += s.get(index + 1);
                        s.remove(index + 1);
                        s.set(index, toUpdate);
                    }
                } else {
                    String out = mat.group("url");
                    if (out != null) {
                        if (s.isEmpty()) {
                            s.add(out);
                        } else {
                            s.set(index, s.get(index) + out);
                        }
                    }

                    index++;
                    out = mat.group("urltwo");
                    if (out != null) {
                        if (s.size() <= index) {
                            s.add(out);
                        } else {
                            s.set(index, out + s.get(index));
                        }
                    }

                    map.add(mat.group("name").toLowerCase());
                }
            }

            // Generic hell.
            ArrayDeque<TextRepresentable> texts = new ArrayDeque<>();
            Map<String, Function<CommandSource, Text>> tokens = Maps.newHashMap();

            // This condition only occurs if you _just_ use the token. Otherwise, you get a part either side - so it's either 0 or 2.
            if (s.size() > 0) {
                createTextTemplateFragmentWithLinks(s.get(0)).mapIfPresent(texts::addAll, tokens::putAll);
            }

            for (int i = 0; i < map.size(); i++) {
                TextTemplate.Arg.Builder arg = TextTemplate.arg(map.get(i)).optional();
                TextRepresentable r = texts.peekLast();
                ITextStyleService.TextFormat style = null;
                if (r != null) {
                    // Create the argument
                    style = this.serviceCollection.textStyleService().getLastColourAndStyle(r, null);
                    arg.color(style.colour()).style(style.style());
                }

                texts.add(arg.build());
                if (s.size() > i + 1) {
                    Tuples.NullableTuple<List<TextRepresentable>, Map<String, Function<CommandSource, Text>>> tt =
                        createTextTemplateFragmentWithLinks(s.get(i + 1));
                    if (style != null && tt.getFirst().isPresent()) {
                        texts.push(style.textOf());
                    }

                    createTextTemplateFragmentWithLinks(s.get(i + 1)).mapIfPresent(texts::addAll, tokens::putAll);
                }
            }

            return Tuple.of(TextTemplate.of(texts.toArray(new Object[0])), tokens);
        }
    }

    @SuppressWarnings("UnstableApiUsage")
    static class Json extends NucleusTextTemplateImpl {

        @Nullable private static TypeSerializer<TextTemplate> textTemplateTypeSerializer = null;

        @NonNull private static TypeSerializer<TextTemplate> getSerialiser() {
            if (textTemplateTypeSerializer == null) {
                textTemplateTypeSerializer = ConfigurationOptions.defaults().getSerializers().get(TypeToken.of(TextTemplate.class));
            }
            return textTemplateTypeSerializer;
        }

        Json(String representation, @Nullable Text prefix, @Nullable Text suffix, INucleusServiceCollection serviceCollection) {
            super(representation, prefix, suffix, serviceCollection);
        }

        Json(String representation, INucleusServiceCollection serviceCollection) {
            super(representation, serviceCollection);
        }

        Json(TextTemplate textTemplate, INucleusServiceCollection serviceCollection) {
            super(JsonConfigurateStringHelper.getJsonStringFrom(textTemplate), serviceCollection);
        }

        @Override
        Tuple<TextTemplate, Map<String, Function<CommandSource, Text>>> parse(String parser) {
            try {
                return Tuple.of(
                        getSerialiser().deserialize(
                                TypeToken.of(TextTemplate.class),
                                JsonConfigurateStringHelper.getNodeFromJson(parser)
                                        .orElseGet(() -> SimpleConfigurationNode.root().setValue(parser))),
                        Maps.newHashMap());
            } catch (ObjectMappingException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static class Empty extends NucleusTextTemplateImpl {

        public static NucleusTextTemplateImpl INSTANCE;

        Empty(INucleusServiceCollection serviceCollection) {
            super("", serviceCollection);
            INSTANCE = this;
        }

        @Override Tuple<TextTemplate, Map<String, Function<CommandSource, Text>>> parse(String parser) {
            return Tuple.of(TextTemplate.EMPTY, Maps.newHashMap());
        }

        @Override public boolean isEmpty() {
            return true;
        }
    }
}
