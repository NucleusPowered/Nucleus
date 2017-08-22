/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.api.nucleusdata;

import org.spongepowered.api.text.Text;

import java.time.Duration;
import java.util.Optional;

public interface KitResult {

    /**
     * Gets the {@link KitResultType}.
     *
     * @return The result type
     */
    KitResultType getResultType();

    /**
     * Gets the result message.
     *
     * @return The result message {@link Text}, if any
     */
    Optional<Text> getMessage();

    /**
     * Gets the remaining cooldown.
     *
     * @return The remaining cooldown {@link Duration}
     */
    Optional<Duration> getRemainingCooldown();

    /**
     * A simple method to determine if the {@link KitResult} was a success.
     *
     * @return Whether the kit result was a success
     */
    default boolean successful() {
        return this.getResultType() == KitResultType.SUCCESS;
    }
}
