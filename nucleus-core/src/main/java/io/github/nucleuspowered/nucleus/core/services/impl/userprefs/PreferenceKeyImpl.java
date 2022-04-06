/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.core.services.impl.userprefs;

import io.github.nucleuspowered.nucleus.api.core.NucleusUserPreferenceService;
import io.github.nucleuspowered.nucleus.core.services.INucleusServiceCollection;
import io.github.nucleuspowered.nucleus.core.services.impl.storage.dataobjects.IUserDataObject;
import io.github.nucleuspowered.nucleus.core.services.impl.storage.dataobjects.keyed.impl.ScalarDataKey;
import io.github.nucleuspowered.nucleus.core.services.interfaces.IMessageProviderService;
import io.github.nucleuspowered.nucleus.core.util.functional.TriConsumer;
import io.github.nucleuspowered.nucleus.core.services.impl.storage.dataobjects.keyed.impl.AbstractDataKey;
import io.leangen.geantyref.TypeToken;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.api.ResourceKey;

import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiPredicate;

public class PreferenceKeyImpl<T> extends ScalarDataKey<T, IUserDataObject> implements NucleusUserPreferenceService.PreferenceKey<T> {

    private final ResourceKey key;
    @Nullable private final T def;
    private final Class<T> clazz;
    private final BiPredicate<INucleusServiceCollection, UUID> canAccess;
    private final String descriptionKey;
    private final TriConsumer<INucleusServiceCollection, UUID, T> onSet;

    PreferenceKeyImpl(
            final ResourceKey key,
            @Nullable final T def,
            final Class<T> clazz,
            final String permission,
            final String descriptionKey) {
        this(key, def, clazz, permission, descriptionKey, (s, u, t) -> {});
    }

    PreferenceKeyImpl(
            final ResourceKey key,
            @Nullable final T def,
            final Class<T> clazz,
            final String permission,
            final String descriptionKey,
            final TriConsumer<INucleusServiceCollection, UUID, T> onSet) {
        this(key,
                def,
                clazz,
                (serviceCollection, user) -> serviceCollection.permissionService().hasPermission(user, permission),
                descriptionKey,
                onSet);
    }

    PreferenceKeyImpl(
            final ResourceKey key,
            @Nullable final T def,
            final Class<T> clazz,
            final BiPredicate<INucleusServiceCollection, UUID> canAccess,
            final String descriptionKey) {
        this(key, def, clazz, canAccess, descriptionKey, (s, u, t) -> {});
    }

    PreferenceKeyImpl(
            final ResourceKey key,
            @Nullable final T def,
            final Class<T> clazz,
            final BiPredicate<INucleusServiceCollection, UUID> canAccess,
            final String descriptionKey,
            final TriConsumer<INucleusServiceCollection, UUID, T> onSet) {
        super(new String[] { "user-prefs", key.asString() }, TypeToken.get(clazz).getType(), IUserDataObject.class, def, null);
        this.key = key;
        this.def = def;
        this.clazz = clazz;
        this.canAccess = canAccess;
        this.descriptionKey = descriptionKey;
        this.onSet = onSet;
    }

    @Override
    public ResourceKey getKey() {
        return this.key;
    }

    public Optional<T> getDefaultValue() {
        return Optional.ofNullable(this.def);
    }

    public Class<T> getValueClass() {
        return this.clazz;
    }

    public boolean canAccess(final INucleusServiceCollection serviceCollection, final UUID user) {
        return this.canAccess.test(serviceCollection, user);
    }

    public String getDescription(final IMessageProviderService messageProviderService) {
        return messageProviderService.getMessageString(this.descriptionKey);
    }

    public String getDescriptionKey() {
        return this.descriptionKey;
    }

    public void onSet(final INucleusServiceCollection serviceCollection, final UUID uuid, final T value) {
        this.onSet.accept(serviceCollection, uuid, value);
    }

    public static class BooleanKey extends PreferenceKeyImpl<Boolean> {

        public BooleanKey(final ResourceKey key, @Nullable final Boolean def, final String permission, final String descriptionKey) {
            super(key, def, Boolean.class, permission, descriptionKey);
        }

        public BooleanKey(final ResourceKey key, @Nullable final Boolean def, final BiPredicate<INucleusServiceCollection, UUID> canAccess,
                final String descriptionKey) {
            super(key, def, Boolean.class, canAccess, descriptionKey);
        }
    }

    public static class LocaleKey extends PreferenceKeyImpl<Locale> {

        public LocaleKey(final ResourceKey key,
                @Nullable final Locale def,
                final String permission,
                final String descriptionKey,
                final TriConsumer<INucleusServiceCollection, UUID, Locale> serviceCollectionConsumer) {
            super(key, def, Locale.class, permission, descriptionKey, serviceCollectionConsumer);
        }

    }
}
