/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.services.impl.cooldown;

import io.github.nucleuspowered.nucleus.services.interfaces.ICooldownService;
import org.spongepowered.api.util.Identifiable;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import com.google.inject.Singleton;

@Singleton
public class CooldownService implements ICooldownService {

    private final Map<DualKey, Instant> cooldowns = new HashMap<>();

    private Map<DualKey, Instant> cleanUp() {
        Instant now = Instant.now();
        Collection<DualKey> keys = this.cooldowns.entrySet()
                .stream()
                .filter(x -> x.getValue().isBefore(now))
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
        for (DualKey key : keys) {
            this.cooldowns.remove(key);
        }

        return this.cooldowns;
    }

    @Override public boolean hasCooldown(String key, Identifiable identifiable) {
        return cleanUp().containsKey(new DualKey(key, identifiable.getUniqueId()));
    }

    @Override public Optional<Duration> getCooldown(String key, Identifiable identifiable) {
        return Optional.ofNullable(cleanUp()
                .get(new DualKey(key, identifiable.getUniqueId())))
                .map(x -> Duration.between(Instant.now(), x));
    }

    @Override public void setCooldown(String key, Identifiable identifiable, Duration cooldownLength) {
        this.cooldowns.put(new DualKey(key, identifiable.getUniqueId()), Instant.now().plus(cooldownLength));
    }

    @Override public void clearCooldown(String key, Identifiable identifiable) {
        this.cooldowns.remove(new DualKey(key, identifiable.getUniqueId()));
    }

    private static class DualKey {

        private final String key;
        private final UUID uuid;

        private DualKey(String key, UUID uuid) {
            this.key = key;
            this.uuid = uuid;
        }

        @Override public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            DualKey dualKey = (DualKey) o;
            return Objects.equals(key, dualKey.key) &&
                    Objects.equals(uuid, dualKey.uuid);
        }

        @Override public int hashCode() {
            return Objects.hash(key, uuid.getLeastSignificantBits(), uuid.getMostSignificantBits());
        }
    }
}
