/*
 * This file is part of Essence, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.essencepowered.essence;

import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;

/**
 * Contains general information about the plugin.
 *
 * <p>This mostly involves the values that are replaced by Blossom.</p>
 */
public final class PluginInfo {

    private PluginInfo() {}

    public static final String ID = "@id@";
    public static final String NAME = "@name@";
    public static final String INFORMATIVE_VERSION = "@informativeVersion@";
    public static final String VERSION = "@version@";

    // Preparing for 4.0.0 SpongeAPI
    public static final String DESCRIPTION = "@description@";
    public static final String URL = "@url@";
    public static final String[] AUTHORS = {"HassanS6000", "dualspiral", "KingGoesGaming"};

    public static final Text MESSAGE_PREFIX = Text.of(TextColors.GREEN, "[" + NAME + "] ");
    public static final Text ERROR_MESSAGE_PREFIX = Text.of(TextColors.RED, "[" + NAME + "] ");
}
