/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus;

import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;

/**
 * Contains general information about the plugin.
 *
 * <p>This mostly involves the values that are replaced by Blossom.</p>
 */
public final class PluginInfo {
    private PluginInfo() {}

    // This isn't going to change now - will break permissions if we have the token.
    public static final String ID = "nucleus";

    public static final String NAME = "@name@";
    public static final String VERSION = "@version@";
    public static final String GIT_HASH = "@gitHash@";

    public static final String MIXIN_VERSION = "@mixinversion@";

    // Preparing for 4.0.0 SpongeAPI
    public static final String DESCRIPTION = "@description@";
    public static final String URL = "@url@";

    public static final Text MESSAGE_PREFIX = Text.of(TextColors.GREEN, "[" + NAME + "] ");
    public static final Text ERROR_MESSAGE_PREFIX = Text.of(TextColors.RED, "[" + NAME + "] ");
}
