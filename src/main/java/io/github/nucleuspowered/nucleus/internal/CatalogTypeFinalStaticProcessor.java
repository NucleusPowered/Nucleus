/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.internal;

import io.github.nucleuspowered.nucleus.api.EventContexts;
import org.spongepowered.api.CatalogType;
import org.spongepowered.api.event.cause.EventContextKey;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Locale;
import java.util.Map;

public class CatalogTypeFinalStaticProcessor {

    private CatalogTypeFinalStaticProcessor() {}

    private static final int MODIFIER_REQS = Modifier.STATIC | Modifier.FINAL;

    public static void setFinalStaticFields(
            Class<?> clazz,
            Map<String, ? extends CatalogType> entries) throws Exception {
        for (Field x : clazz.getFields()) { // must be public
            if ((x.getModifiers() & MODIFIER_REQS) == MODIFIER_REQS) {
                String f = "nucleus:" + x.getName().toLowerCase(Locale.ENGLISH);
                CatalogType entry = entries.get(f);
                if (entry != null) {
                    // register
                    setFinalStatic(x, entry);
                }
            }
        }
    }

    private static void setFinalStatic(Field field, Object entry) throws Exception {
        field.setAccessible(true);

        Field modifiersField = Field.class.getDeclaredField("modifiers");
        modifiersField.setAccessible(true);
        modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);
        field.set(null, entry);
    }

    public static void setEventContexts() throws Exception {
        EventContextKey<Boolean> shouldFormatChannel = EventContextKey.builder(Boolean.class)
                .id(EventContexts.Identifiers.SHOULD_FORMAT_CHANNEL)
                .name("Nucleus - Context to indicate whether a chat message should be formatted.")
                .build();
        setFinalStatic(EventContexts.class.getDeclaredField("SHOULD_FORMAT_CHANNEL"), shouldFormatChannel);

        EventContextKey<Boolean> isJailingAction = EventContextKey.builder(Boolean.class)
                .id(EventContexts.Identifiers.IS_JAILING_ACTION)
                .name("Nucleus - Context to indicate whether a teleport is a jailing teleport.")
                .build();
        setFinalStatic(EventContexts.class.getDeclaredField("IS_JAILING_ACTION"), shouldFormatChannel);

        EventContextKey<Boolean> bypassJailingRestriction = EventContextKey.builder(Boolean.class)
                .id(EventContexts.Identifiers.BYPASS_JAILING_RESTRICTION)
                .name("Nucleus - Context to indicate whether the Nucleus system should ignore jailed status when teleporting a player.")
                .build();
        setFinalStatic(EventContexts.class.getDeclaredField("BYPASS_JAILING_RESTRICTION"), shouldFormatChannel);
    }

}
