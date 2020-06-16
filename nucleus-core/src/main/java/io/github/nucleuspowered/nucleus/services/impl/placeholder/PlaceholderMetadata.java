/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.services.impl.placeholder;

import org.spongepowered.api.text.placeholder.PlaceholderParser;

public class PlaceholderMetadata {
    private final String token;
    private final PlaceholderParser parser;

    PlaceholderMetadata(String token, PlaceholderParser parser) {
        this.token = token;
        this.parser = parser;
    }

    public String getToken() {
        return this.token;
    }

    public PlaceholderParser getParser() {
        return this.parser;
    }
}
