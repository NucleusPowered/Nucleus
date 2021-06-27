/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.core.services.interfaces;

import com.google.inject.ImplementedBy;
import io.github.nucleuspowered.nucleus.api.core.NucleusUserPreferenceService;
import io.github.nucleuspowered.nucleus.core.services.impl.userprefs.NucleusKeysProvider;
import io.github.nucleuspowered.nucleus.core.services.impl.userprefs.PreferenceKeyImpl;
import io.github.nucleuspowered.nucleus.core.services.impl.userprefs.UserPreferenceService;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@ImplementedBy(UserPreferenceService.class)
public interface IUserPreferenceService extends NucleusUserPreferenceService {

    <T> void set(UUID uuid, PreferenceKey<T> key, @Nullable T value);

    <T> void set(UUID uuid, PreferenceKeyImpl<T> key, @Nullable T value);

    Map<PreferenceKey<?>, Object> get(UUID user);

    <T> Optional<T> get(UUID uuid, PreferenceKey<T> key);

    <T> T getUnwrapped(UUID uuid, PreferenceKey<T> key);

    @Override NucleusKeysProvider keys();

}
