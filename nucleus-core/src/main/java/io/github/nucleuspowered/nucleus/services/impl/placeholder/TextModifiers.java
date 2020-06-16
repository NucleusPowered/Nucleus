/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.services.impl.placeholder;

import org.spongepowered.api.text.Text;

import java.util.function.Function;

public enum TextModifiers implements Function<Text, Text> {

    SPACE_AFTER("s") {
        @Override
        public Text apply(Text text) {
            return Text.of(text, " ");
        }
    },
    SPACE_BEFORE("p") {
        @Override
        public Text apply(Text text) {
            return Text.of(" ", text);
        }
    };

    private final String key;

    TextModifiers(String p) {
        this.key = p;
    }

    public String getKey() {
        return this.key;
    }
}
