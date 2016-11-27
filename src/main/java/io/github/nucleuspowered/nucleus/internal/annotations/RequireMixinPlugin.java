/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.internal.annotations;

import io.github.nucleuspowered.nucleus.internal.MixinConfigProxy;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.function.Predicate;

/**
 * Indicates that the Nucleus Mixin plugin is required for this command/listener/event to be loaded.
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireMixinPlugin {

    /**
     * Returns whether to document this requirement in DocGen.
     *
     * @return {@code true} if so.
     */
    boolean document() default true;

    /**
     * Returns whether the console is notified on startup that this class may be skipped.
     *
     * @return {@code true} if so.
     */
    boolean notifyOnLoad() default true;

    /**
     * Sets whether this command should only be loaded when the Mixin Companion plugin is present, not, or in both caes.
     * Defaults to both.
     *
     * @return {@link MixinLoad} that defines whether to load the command or not.
     */
    MixinLoad value();

    /**
     * A class that contains a predicate that determines whether to load the class, if {@link MixinLoad#MIXIN_ONLY} is present.
     *
     * @return The {@link Class} of the {@link Predicate}. It must have a public no-args constructor.
     */
    Class<? extends Predicate<MixinConfigProxy>> loadWhen() default TruePredicate.class;

    enum MixinLoad {
        BOTH,
        NO_MIXIN,
        MIXIN_ONLY
    }

    class TruePredicate implements Predicate<MixinConfigProxy> {

        @Override public boolean test(MixinConfigProxy mixinConfigProxy) {
            return true;
        }
    }
}
