/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.services.interfaces;

import com.google.inject.ImplementedBy;
import io.github.nucleuspowered.nucleus.services.impl.textstyle.TextStyleService;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.TextRepresentable;
import org.spongepowered.api.text.format.TextColor;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.text.format.TextStyle;
import org.spongepowered.api.text.format.TextStyles;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import javax.annotation.Nullable;

/**
 * Provides methods for resolving text colour and style permissions.
 * Will depend on the {@link IPermissionService}
 */
@ImplementedBy(TextStyleService.class)
public interface ITextStyleService {

    TextFormat EMPTY = new TextFormat() {
        @Override public TextColor colour() {
            return TextColors.NONE;
        }

        @Override public TextStyle style() {
            return TextStyles.NONE;
        }

        @Override public Text textOf() {
            return Text.of();
        }
    };

    Optional<String> getPermissionFor(String prefix, TextColor colour);

    List<String> getPermissionsFor(String prefix, TextStyle colour);

    /**
     * Removes formating codes based on permission.
     *
     * @param permissionPrefixColour The prefix of the permission to check for text colours
     * @param permissionPrefixStyle The prefix of the permission to check for text styles
     * @param text The text to strip
     * @return The text with the formatting stripped.
     */
    String stripPermissionless(String permissionPrefixColour, String permissionPrefixStyle, Subject source, String text);

    String stripPermissionless(String permissionPrefixColour, String permissionPrefixColor, String permissionPrefixStyle, Subject source,
            String oldMessage);

    Collection<String> wouldStrip(Collection<String> permissionPrefixColour, String permissionPrefixStyle, Subject source, String text);

    default TextFormat getLastColourAndStyle(TextRepresentable text, @Nullable TextFormat current) {
        return getLastColourAndStyle(text, current, TextColors.NONE, TextStyles.NONE);
    }

    TextFormat getLastColourAndStyle(TextRepresentable text,
            @Nullable TextFormat current,
            TextColor defaultColour,
            TextStyle defaultStyle);

    TextColor getColourFromString(@Nullable String s);

    TextStyle getTextStyleFromString(@Nullable String s);

    Text addUrls(String message);

    Text addUrls(String message, boolean replaceBlueUnderline);

    Text getTextForUrl(String url, String msg, String whiteSpace, TextFormat st, @Nullable String optionString);

    Text oldLegacy(String message);

    Text joinTextsWithColoursFlowing(Text... texts);

    interface TextFormat {

        TextColor colour();

        TextStyle style();

        Text textOf();

    }

}
