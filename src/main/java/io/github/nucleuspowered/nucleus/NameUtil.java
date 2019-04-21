/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import io.github.nucleuspowered.nucleus.internal.traits.InternalServiceManagerTrait;
import io.github.nucleuspowered.nucleus.modules.chat.config.ChatTemplateConfig;
import io.github.nucleuspowered.nucleus.modules.chat.util.TemplateUtil;
import io.github.nucleuspowered.nucleus.modules.nickname.services.NicknameService;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.service.user.UserStorageService;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.TextElement;
import org.spongepowered.api.text.action.TextActions;
import org.spongepowered.api.text.format.TextColor;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.text.format.TextStyle;
import org.spongepowered.api.text.format.TextStyles;
import org.spongepowered.api.text.serializer.TextSerializers;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

import javax.annotation.Nullable;

public class NameUtil implements InternalServiceManagerTrait {

    private final static Map<Character, TextColor> colourMap = Maps.newHashMap();
    private final static Map<Character, TextStyle> styleMap = Maps.newHashMap();
    private final static Map<String, TextStyle> styleMapFull = Maps.newHashMap();
    private final static Map<Character, String> styleKeyMap = Maps.newHashMap();

    private final static String OBFUSICATED = "OBFUSICATED";
    private final static String BOLD = "BOLD";
    private final static String STRIKETHROUGH = "STRIKETHROUGH";
    private final static String UNDERLINE = "UNDERLINE";
    private final static String ITALIC = "ITALIC";
    private final static String RESET = "RESET";

    static {
        colourMap.put('0', TextColors.BLACK);
        colourMap.put('1', TextColors.DARK_BLUE);
        colourMap.put('2', TextColors.DARK_GREEN);
        colourMap.put('3', TextColors.DARK_AQUA);
        colourMap.put('4', TextColors.DARK_RED);
        colourMap.put('5', TextColors.DARK_PURPLE);
        colourMap.put('6', TextColors.GOLD);
        colourMap.put('7', TextColors.GRAY);
        colourMap.put('8', TextColors.DARK_GRAY);
        colourMap.put('9', TextColors.BLUE);
        colourMap.put('a', TextColors.GREEN);
        colourMap.put('b', TextColors.AQUA);
        colourMap.put('c', TextColors.RED);
        colourMap.put('d', TextColors.LIGHT_PURPLE);
        colourMap.put('e', TextColors.YELLOW);
        colourMap.put('f', TextColors.WHITE);

        styleMapFull.put("OBFUSCATED", TextStyles.OBFUSCATED);
        styleMapFull.put("MAGIC", TextStyles.OBFUSCATED);
        styleMapFull.put("BOLD", TextStyles.BOLD);
        styleMapFull.put("STRIKETHROUGH", TextStyles.STRIKETHROUGH);
        styleMapFull.put("UNDERLINE", TextStyles.UNDERLINE);
        styleMapFull.put("ITALIC", TextStyles.ITALIC);
        styleMapFull.put("RESET", TextStyles.RESET);

        styleMap.put('k', TextStyles.OBFUSCATED);
        styleMap.put('l', TextStyles.BOLD);
        styleMap.put('m', TextStyles.STRIKETHROUGH);
        styleMap.put('n', TextStyles.UNDERLINE);
        styleMap.put('o', TextStyles.ITALIC);
        styleMap.put('r', TextStyles.RESET);

        styleKeyMap.put('k', OBFUSICATED);
        styleKeyMap.put('l', BOLD);
        styleKeyMap.put('m', STRIKETHROUGH);
        styleKeyMap.put('n', UNDERLINE);
        styleKeyMap.put('o', ITALIC);
        styleKeyMap.put('r', RESET);

    }

    public static ImmutableMap<Character, TextColor> getColours() {
        return ImmutableMap.copyOf(colourMap);
    }

    public static ImmutableMap<Character, TextStyle> getStyles() {
        return ImmutableMap.copyOf(styleMap);
    }

    public static ImmutableMap<Character, String> getStyleKeys() {
        return ImmutableMap.copyOf(styleKeyMap);
    }

    public Optional<Text> getName(UUID player) {
        return Sponge.getServiceManager().provideUnchecked(UserStorageService.class).get(player).map(this::getName);
    }

    public Text getNameOrConsole(@Nullable UUID player) {
        if (player == null || player == Util.consoleFakeUUID) {
            return Text.of(Sponge.getServer().getConsole().getName());
        }

        return Sponge.getServiceManager().provideUnchecked(UserStorageService.class)
                .get(player)
                .map(this::getName)
                .orElseGet(() -> Nucleus.getNucleus().getMessageProvider().getTextMessageWithFormat("standard.unknown"));
    }

     /**
     * Gets the display name from a {@link User} as Sponge sees it.
     *
     * @param player The {@link User} to get the data from.
     * @return The {@link Text}
     */
    public Text getName(User player) {
        Preconditions.checkNotNull(player);

        TextColor tc = getNameColour(player);
        TextStyle ts = getNameStyle(player);

        Text.Builder tb = getService(NicknameService.class)
                .map(service -> service.getNicknameWithPrefix(player).map(Text::toBuilder).orElse(null))
                .orElseGet(() ->
                        player.get(Keys.DISPLAY_NAME)
                                .map(Text::toBuilder).orElseGet(() -> Text.builder(player.getName())));

        tb.onHover(TextActions.showText(Nucleus.getNucleus().getMessageProvider().getTextMessageWithFormat("name.hover.ign", player.getName()))).build();
        if (tc != TextColors.NONE && tb.getColor() == TextColors.NONE) {
            List<Text> lt = tb.getChildren();
            if (lt.isEmpty() || lt.get(0).getColor().equals(TextColors.NONE)) {
                tb.color(tc);
            }
        }

        if (!ts.isEmpty()) {
            tb.style(ts);
        }
        return tb.build();
    }

    public String getSerialisedName(User player) {
        return TextSerializers.FORMATTING_CODE.serialize(getName(player));
    }

    public String getNameFromUUID(@Nullable UUID uuid) {
        if (uuid == null || Util.consoleFakeUUID.equals(uuid)) {
            return Sponge.getServer().getConsole().getName();
        }

        UserStorageService uss = Sponge.getServiceManager().provideUnchecked(UserStorageService.class);
        Optional<User> user = uss.get(uuid);
        return user.map(User::getName).orElseGet(() -> Nucleus.getNucleus().getMessageProvider().getMessageWithFormat("standard.unknown"));

    }

    public TextColor getColourFromString(@Nullable String s) {
        if (s == null || s.length() == 0) {
            return TextColors.NONE;
        }

        if (s.length() == 1) {
            return colourMap.getOrDefault(s.charAt(0), TextColors.NONE);
        } else {
            return Sponge.getRegistry().getType(TextColor.class, s.toUpperCase()).orElse(TextColors.NONE);
        }
    }

    public TextStyle getTextStyleFromString(@Nullable String s) {
        if (s == null || s.length() == 0) {
            return TextStyles.NONE;
        }

        TextStyle ts = TextStyles.NONE;
        for (String split : s.split("\\s*,\\s*")) {
            if (split.length() == 1) {
                ts = ts.and(styleMap.getOrDefault(split.charAt(0), TextStyles.NONE));
            } else {
                ts = ts.and(styleMapFull.getOrDefault(split.toUpperCase(), TextStyles.NONE));
            }
        }

        return ts;
    }

    private TextColor getNameColour(User player) {
        return getStyle(player, this::getColourFromString, x -> getColourFromString(x.getNamecolour()), TextColors.NONE,
                "namecolor", "namecolour");
    }

    private TextStyle getNameStyle(User player) {
        return getStyle(player, this::getTextStyleFromString, x -> getTextStyleFromString(x.getNamestyle()), TextStyles.NONE,
                "namestyle");
    }

    private <T extends TextElement> T getStyle(User player, Function<String, T> returnIfAvailable,
            Function<ChatTemplateConfig, T> fromTemplate, T def, String... options) {
        Optional<String> os = Util.getOptionFromSubject(player, options);
        if (os.isPresent()) {
            return returnIfAvailable.apply(os.get());
        }

        return getService(TemplateUtil.class).map(templateUtil -> fromTemplate.apply(templateUtil.getTemplateNow(player))).orElse(def);

    }
}
