/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.api;

import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.cause.EventContextKey;
import org.spongepowered.api.util.generator.dummy.DummyObjectProvider;

/**
 * Contexts that may appear in the {@link Cause} of some events.
 */
public class EventContexts {

    private EventContexts() {}

    /**
     * A context that indicates whether the Nucleus chat events will perform its own formatting.
     *
     * <p>
     *     For the ID, see {@link Identifiers#SHOULD_FORMAT_CHANNEL}
     * </p>
     */
    public static final EventContextKey<Boolean> SHOULD_FORMAT_CHANNEL =
            DummyObjectProvider.createExtendedFor(EventContextKey.class, "SHOULD_FORMAT_CHANNEL");

    /**
     * A context that indicates whether a teleport is a jailing action.
     *
     * <p>
     *     For the ID, see {@link Identifiers#IS_JAILING_ACTION}
     * </p>
     */
    public static final EventContextKey<Boolean> IS_JAILING_ACTION =
            DummyObjectProvider.createExtendedFor(EventContextKey.class, "IS_JAILING_ACTION");

    /**
     * A context that indicates whether teleports should ignore the fact someone is jailed.
     *
     * <p>
     *     For the ID, see {@link Identifiers#BYPASS_JAILING_RESTRICTION }
     * </p>
     */
    public static final EventContextKey<Boolean> BYPASS_JAILING_RESTRICTION =
            DummyObjectProvider.createExtendedFor(EventContextKey.class, "BYPASS_JAILING_RESTRICTION ");


    public static class Identifiers {

        private Identifiers() {}

        /**
         * ID for {@link EventContexts#SHOULD_FORMAT_CHANNEL}
         */
        public static final String SHOULD_FORMAT_CHANNEL = "nucleus:should_format_channel";

        /**
         * ID for {@link EventContexts#IS_JAILING_ACTION}
         */
        public static final String IS_JAILING_ACTION = "nucleus:is_jailing_action";

        /**
         * ID for {@link EventContexts#BYPASS_JAILING_RESTRICTION}
         */
        public static final String BYPASS_JAILING_RESTRICTION = "nucleus:bypass_jailing_restriction";
    }

}
