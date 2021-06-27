/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.core.services.impl.userprefs;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.github.nucleuspowered.nucleus.api.core.NucleusUserPreferenceService;
import io.github.nucleuspowered.nucleus.core.services.INucleusServiceCollection;
import io.github.nucleuspowered.nucleus.core.services.interfaces.IUserPreferenceService;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.registry.DefaultedRegistryType;
import org.spongepowered.api.registry.RegistryKey;
import org.spongepowered.api.registry.RegistryRoots;
import org.spongepowered.api.registry.RegistryType;
import org.spongepowered.api.registry.RegistryTypes;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Singleton
public class UserPreferenceService implements IUserPreferenceService {

    private final ResourceKey resourceKey;
    private final DefaultedRegistryType<PreferenceKey<?>> registryType;
    private final NucleusKeysProvider provider;
    private final INucleusServiceCollection serviceCollection;

    @Inject
    public UserPreferenceService(final INucleusServiceCollection serviceCollection) {
        this.serviceCollection = serviceCollection;
        this.resourceKey = ResourceKey.of(serviceCollection.pluginContainer(), "preference_service");
        this.registryType = RegistryType.of(RegistryRoots.SPONGE, this.resourceKey).asDefaultedType(() -> Sponge.game().registries());
        this.provider = new NucleusKeysProvider(this.registryType);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override
    public <T> void set(final UUID uuid, final NucleusUserPreferenceService.PreferenceKey<T> key, @Nullable final T value) {
        final PreferenceKeyImpl pki;
        if (Objects.requireNonNull(key) instanceof PreferenceKeyImpl) {
            pki = (PreferenceKeyImpl) key;
        } else {
            throw new IllegalArgumentException("Cannot have custom preference keys");
        }

        this.set(uuid, pki, value);
        ((PreferenceKeyImpl<T>) key).onSet(this.serviceCollection, uuid, value);
    }

    @Override
    public <T> void set(final UUID uuid, final PreferenceKeyImpl<T> key, @Nullable final T value) {
        this.serviceCollection
                .storageManager()
                .getUserService()
                .getOrNew(uuid)
                .thenAccept(x -> x.set(key, value));
    }

    @SuppressWarnings("rawtypes")
    @Override
    public Map<NucleusUserPreferenceService.PreferenceKey<?>, Object> get(final UUID user) {
        final Map<NucleusUserPreferenceService.PreferenceKey<?>, Object> ret = new HashMap<>();
        this.registryType.get().stream().forEach(key -> {
            if (((PreferenceKeyImpl) key).canAccess(this.serviceCollection, user)) {
                ret.put(key, this.get(user, key).orElse(null));
            }
        });

        return ret;
    }

    @Override
    public <T> Optional<T> get(final UUID uuid, final NucleusUserPreferenceService.PreferenceKey<T> key) {
        if (!this.registryType.get().findValueKey(key).isPresent()) {
            throw new IllegalArgumentException("Key is not registered.");
        }

        if (!(key instanceof PreferenceKeyImpl)) {
            throw new IllegalArgumentException("Custom preference keys are not supported.");
        }

        final PreferenceKeyImpl<T> prefKey = (PreferenceKeyImpl<T>) key;
        Optional<T> ot = Optional.empty();
        try {
            ot = this.serviceCollection
                    .storageManager()
                    .getUserService()
                    .getOnThread(uuid)
                    .map(x -> x.getOrDefault(prefKey));
        } catch (final ClassCastException e) {
            e.printStackTrace();
        }

        return ot;
    }

    @Override
    public <T> T getUnwrapped(final UUID uuid, final NucleusUserPreferenceService.PreferenceKey<T> key) {
        return this.get(uuid, key).orElse(null);
    }

    @Override
    public ResourceKey getRegistryResourceKey() {
        return this.resourceKey;
    }

    @Override
    public DefaultedRegistryType<PreferenceKey<?>> getRegistryResourceType() {
        return this.registryType;
    }

    @Override
    public NucleusKeysProvider keys() {
        return this.provider;
    }

    @Override
    public <T> Optional<T> getPreferenceFor(final UUID user, final NucleusUserPreferenceService.PreferenceKey<T> key) {
        return this.get(user, key);
    }

    @Override
    public <T> void setPreferenceFor(final UUID user, final NucleusUserPreferenceService.PreferenceKey<T> key, final T value) {
        this.set(user, key, value);
    }

    @Override
    public void removePreferenceFor(final UUID user, final NucleusUserPreferenceService.PreferenceKey<?> key) {
        this.set(user, key, null);
    }

    @SuppressWarnings("rawtypes")
    @Override
    public boolean canAccess(final UUID user, final PreferenceKey<?> key) {
        return ((PreferenceKeyImpl) key).canAccess(this.serviceCollection, user);
    }

    @SuppressWarnings("rawtypes")
    @Override
    public String getDescription(final PreferenceKey<?> key) {
        return ((PreferenceKeyImpl) key).getDescription(this.serviceCollection.messageProvider());
    }

}
