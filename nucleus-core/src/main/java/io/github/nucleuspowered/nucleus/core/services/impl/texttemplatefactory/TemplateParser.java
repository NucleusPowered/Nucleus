/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.core.services.impl.texttemplatefactory;

import com.google.inject.Inject;
import io.github.nucleuspowered.nucleus.core.services.INucleusServiceCollection;
import io.github.nucleuspowered.nucleus.core.services.impl.textstyle.TextStyleService;
import io.github.nucleuspowered.nucleus.core.services.interfaces.IMessageProviderService;
import io.github.nucleuspowered.nucleus.core.services.interfaces.INucleusTextTemplateFactory;
import io.github.nucleuspowered.nucleus.core.services.interfaces.ITextStyleService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.api.util.Nameable;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class TemplateParser implements INucleusTextTemplateFactory {

    private final INucleusServiceCollection serviceCollection;

    private static final Pattern pattern =
            Pattern.compile("(?<url>\\[[^\\[]+]\\(/[^)]*?)?(?<match>\\{\\{(?!subject)(?<name>[^\\s{}]+)}})"
                    + "(?<urltwo>[^(]*?\\))?");

    private final Pattern enhancedUrlParser =
            Pattern.compile("(?<first>(^|\\s))(?<reset>&r)?(?<colour>(&[0-9a-flmnrok])+)?"
                            + "((?<options>\\{[a-z]+?})?(?<url>(http(s)?://)?([A-Za-z0-9]+\\.)+[A-Za-z0-9-]{2,}\\S*)|"
                            + "(?<specialUrl>(\\[(?<msg>.+?)](?<optionssurl>\\{[a-z]+})?\\((?<sUrl>(http(s)?://)?([A-Za-z0-9-]+\\.)+[A-Za-z0-9]{2,}[^\\s)]*)\\)))|"
                            + "(?<specialCmd>(\\[(?<sMsg>.+?)](?<optionsscmd>\\{[a-z]+})?\\((?<sCmd>/.+?)\\))))",
                    Pattern.CASE_INSENSITIVE);

    private final NucleusTextTemplateImpl empty;

    @Inject
    public TemplateParser(final INucleusServiceCollection serviceCollection) {
        this.serviceCollection = serviceCollection;
        this.empty = new NucleusTextTemplateImpl(this.serviceCollection, Collections.emptyList(), null, null);
    }

    @Override
    public NucleusTextTemplateImpl empty() {
        return this.empty;
    }

    @Override
    public NucleusTextTemplateImpl createFromAmpersandString(final @Nullable String string) {
        return this.createFromAmpersandString(string, null, null);
    }

    @Override
    public NucleusTextTemplateImpl createFromAmpersandString(final @Nullable String string, final Component prefix, final Component suffix) {
        if (string == null || string.isEmpty()) {
            return NucleusTextTemplateImpl.empty();
        }
        final Matcher mat = pattern.matcher(string);
        final List<String> map = new ArrayList<>();

        final List<String> s = new ArrayList<>();
        Collections.addAll(s, pattern.split(string));
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
        final List<BiFunction<Object, Map<String, Function<Object, Optional<ComponentLike>>>, Component>> texts = new LinkedList<>();
        ITextStyleService.TextFormat textFormat = ITextStyleService.EMPTY;
        if (s.size() > 0) {
            textFormat = this.createTextTemplateFragmentWithLinks(s.get(0), texts, textFormat).orElse(textFormat);
        }

        for (int i = 0; i < map.size(); i++) {
            texts.add(new PlaceholderElement(textFormat.colour().orElse(null), textFormat.style(), map.get(i)));
            if (s.size() > i + 1) {
                textFormat = this.createTextTemplateFragmentWithLinks(s.get(i + 1), texts, textFormat).orElse(textFormat);
            }
        }

        return new NucleusTextTemplateImpl(this.serviceCollection, texts, prefix, suffix);
    }

    @Override
    public Optional<NucleusTextTemplateImpl> createFromAmpersandStringIgnoringExceptions(final String string) {
        try {
            return Optional.of(this.createFromAmpersandString(string));
        } catch (final Exception e) {
            this.serviceCollection.logger().warn("Could not parse \"{}\", ignoring.", string);
            if (this.serviceCollection.propertyHolder().debugMode()) {
                e.printStackTrace();
            }
            return Optional.empty();
        }
    }

    public Optional<ITextStyleService.TextFormat> createTextTemplateFragmentWithLinks(final String message,
            final List<BiFunction<Object, Map<String, Function<Object, Optional<ComponentLike>>>, Component>> elements,
            final ITextStyleService.TextFormat format) {
        Objects.requireNonNull(message, "message");
        if (message.isEmpty()) {
            return Optional.of(format);
        }

        final Matcher m = this.enhancedUrlParser.matcher(message);
        final ITextStyleService textStyleService = this.serviceCollection.textStyleService();
        if (!m.find()) {
            final Component component = textStyleService.oldLegacy(message);
            elements.add((a, b) -> component);
            return Optional.of(textStyleService.getLastColourAndStyle(component, format));
        }

        String remaining = message;
        ITextStyleService.TextFormat st = format;
        do {
            // We found a URL. We split on the URL that we have.
            final String[] textArray = remaining.split(this.enhancedUrlParser.pattern(), 2);
            final Component first = Component.text().color(st.colour().orElse(null)).style(st.style())
                    .append(textStyleService.oldLegacy(textArray[0])).build();

            // Add this text to the list regardless.
            elements.add((a, b) -> first);

            // If we have more to do, shove it into the "remaining" variable.
            if (textArray.length == 2) {
                remaining = textArray[1];
            } else {
                remaining = null;
            }

            // Get the last colour & styles
            final String colourMatch = m.group("colour");
            st = textStyleService.getLastColourAndStyle(first, st);
            Style style = st.style();
            @Nullable TextColor colour = st.colour().orElse(null);
            if (colourMatch != null && !colourMatch.isEmpty()) {

                // If there is a reset, explicitly do it.
                if (m.group("reset") != null) {
                    style = this.serviceCollection.textStyleService().getResetStyle();
                    colour = NamedTextColor.WHITE;
                }

                for (int idx = 1; idx < colourMatch.length(); idx = idx + 2) {
                    final String match = colourMatch.substring(idx, idx+1);
                    if (match.equals("r")) {
                        colour = NamedTextColor.WHITE;
                        style = this.serviceCollection.textStyleService().getResetStyle();
                    }
                    style = textStyleService.getTextStyleFromString(match).merge(style);
                    colour = textStyleService.getColourFromString(match).orElse(colour);
                }
                st = new TextStyleService.TextFormatImpl(colour, style);
            }

            // Build the URL
            final String whiteSpace = m.group("first");
            final ITextStyleService.TextFormat tf = st;
            if (m.group("url") != null) {
                final String url = m.group("url");
                elements.add((a, b) -> this.getTextForUrl(url, url, whiteSpace, tf, m.group("options")));
            } else if (m.group("specialUrl") != null) {
                final String url = m.group("sUrl");
                final String msg = m.group("msg");
                elements.add((a, b) -> this.getTextForUrl(url, msg, whiteSpace, tf, m.group("optionssurl")));
            } else {
                // Must be commands.
                final String cmd = m.group("sCmd");
                final String msg = m.group("sMsg");
                final String optionList = m.group("optionsscmd");

                if (cmd.contains("{{subject}}")) {
                    elements.add(new SubjectCommand(
                            st.colour().orElse(null),
                            st.style(),
                            msg,
                            cmd,
                            optionList,
                            whiteSpace
                    ));
                } else {
                    final Style s = st.style();
                    final @Nullable TextColor c = st.colour().orElse(null);
                    elements.add(
                            (a, b) -> Component.text().color(c).style(s).append(this.getCmd(msg, cmd, optionList, whiteSpace)).build());
                }
            }
        } while (remaining != null && m.find());

        // Add the last bit.
        if (remaining != null) {
            final TextComponent.Builder tb =
                    Component.text().color(st.colour().orElse(null)).style(st.style()).append(LegacyComponentSerializer.legacyAmpersand().deserialize(remaining));
            if (remaining.matches("^\\s+&r.*")) {
                tb.style(this.serviceCollection.textStyleService().getResetStyle());
            }

            final TextComponent t = tb.build();
            st = textStyleService.getLastColourAndStyle(t, st);
            elements.add((a, b) -> t);
        }
        return Optional.of(st);
    }

    private Component getTextForUrl(
            final String toParse, final String msg, final String whiteSpace, final ITextStyleService.TextFormat st, @Nullable final String optionString) {
        final IMessageProviderService messageProviderService = this.serviceCollection.messageProvider();

        try {
            final URL urlObj;
            if (!toParse.startsWith("http://") && !toParse.startsWith("https://")) {
                urlObj = new URL("http://" + toParse);
            } else {
                urlObj = new URL(toParse);
            }

            final TextComponent.Builder textBuilder =
                    Component.text().content(msg).color(st.colour().orElse(null))
                            .style(st.style())
                            .clickEvent(ClickEvent.openUrl(urlObj.toString()));
            if (optionString == null || !optionString.contains("h")) {
                textBuilder.hoverEvent(HoverEvent.showText(messageProviderService.getMessage("chat.url.click", toParse)));
            }

            if (!whiteSpace.isEmpty()) {
                return Component.text().content(whiteSpace).append(textBuilder.build()).build();
            }

            return textBuilder.build();
        } catch (final MalformedURLException e) {
            // URL parsing failed, just put the original text in here.
            this.serviceCollection.logger().warn(messageProviderService.getMessageString("chat.url.malformed", toParse));
            e.printStackTrace();

            final Component ret = Component.text().content(toParse).color(st.colour().orElse(null)).style(st.style()).build();
            if (!whiteSpace.isEmpty()) {
                return Component.text().content(whiteSpace).append(ret).build();
            }

            return ret;
        }
    }

    private Component getCmd(final String msg, final String cmd, @Nullable final String optionList, final String whiteSpace) {
        final TextComponent.Builder textBuilder = Component.text().content(msg)
                .clickEvent(ClickEvent.runCommand(cmd))
                .hoverEvent(this.setupHoverOnCmd(cmd, optionList));
        if (optionList != null && optionList.contains("s")) {
            textBuilder.clickEvent(ClickEvent.suggestCommand(cmd));
        }

        Component toAdd = textBuilder.build();
        if (!whiteSpace.isEmpty()) {
            toAdd = Component.join(Component.text(whiteSpace), toAdd);
        }

        return toAdd;
    }

    @Nullable
    private HoverEvent<?> setupHoverOnCmd(final String cmd, @Nullable final String optionList) {
        if (optionList != null) {
            if (optionList.contains("h")) {
                return null;
            }

            if (optionList.contains("s")) {
                return HoverEvent.showText(this.serviceCollection.messageProvider().getMessage("chat.command.clicksuggest", cmd));
            }
        }

        return HoverEvent.showText(this.serviceCollection.messageProvider().getMessage("chat.command.click", cmd));
    }

    private final class SubjectCommand implements BiFunction<Object, Map<String, Function<Object, Optional<ComponentLike>>>, Component> {

        @Nullable private final TextColor colour;
        private final Style style;
        private final String message;
        private final String cmd;
        private final String optionList;
        private final String whiteSpace;

        private SubjectCommand(@Nullable final TextColor colour, final Style style, final String message, final String cmd, final String optionList, final String whiteSpace) {
            this.colour = colour;
            this.style = style;
            this.message = message;
            this.cmd = cmd;
            this.optionList = optionList;
            this.whiteSpace = whiteSpace;
        }

        @Override
        public Component apply(final Object cs, final Map<String, Function<Object, Optional<ComponentLike>>> tokens) {
            final String name;
            if (cs instanceof Nameable) {
                name = ((Nameable) cs).name();
            } else {
                name = "";
            }
            final String command = this.cmd.replace("{{subject}}", name);
            return Component.text().color(this.colour).style(this.style)
                    .append(TemplateParser.this.getCmd(this.message, command, this.optionList, this.whiteSpace)).build();
        }

    }

    final class PlaceholderElement implements BiFunction<Object, Map<String, Function<Object, Optional<ComponentLike>>>, Component> {

        @Nullable private final TextColor colour;
        private final Style style;
        private final String key;

        private PlaceholderElement(@Nullable final TextColor colour, final Style style, final String key) {
            this.colour = colour;
            this.style = style;
            this.key = key.toLowerCase();
        }

        @Override
        public Component apply(final Object cs, final Map<String, Function<Object, Optional<ComponentLike>>> tokens) {
            final ComponentLike t;
            if (tokens != null && tokens.containsKey(this.key)) {
                t = tokens.get(this.key).apply(cs).orElse(Component.empty());
            } else {
                t = TemplateParser.this.serviceCollection.placeholderService().parse(cs, this.key);
            }

            return Component.text().color(this.colour).style(this.style).append(t).build();
        }

    }

}
