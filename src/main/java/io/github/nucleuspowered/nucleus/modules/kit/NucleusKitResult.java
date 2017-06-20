/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.kit;

import io.github.nucleuspowered.nucleus.api.nucleusdata.KitResult;
import io.github.nucleuspowered.nucleus.api.nucleusdata.KitResultType;
import org.spongepowered.api.text.Text;

import java.time.Duration;
import java.util.Optional;

public class NucleusKitResult implements KitResult {

    private final KitResultType resultType;
    private final Text message;
    private final Duration remainingCooldown;

    public NucleusKitResult(KitResultType type) {
        resultType = type;
        message = null;
        remainingCooldown = null;
    }

    public NucleusKitResult(KitResultType type, Duration cooldown) {
        resultType = type;
        message = null;
        remainingCooldown = cooldown;
    }

    @Override public KitResultType getResultType() {
        return this.resultType;
    }

    @Override public Optional<Text> getMessage() {
        return Optional.ofNullable(message);
    }

    @Override public Optional<Duration> getRemainingCooldown() {
        return Optional.ofNullable(remainingCooldown);
    }
}
