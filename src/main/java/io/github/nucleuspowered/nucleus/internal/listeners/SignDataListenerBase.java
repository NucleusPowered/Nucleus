/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.internal.listeners;

import com.google.common.collect.Maps;
import com.google.inject.Inject;
import io.github.nucleuspowered.nucleus.NucleusPlugin;
import io.github.nucleuspowered.nucleus.internal.permissions.PermissionInformation;
import org.spongepowered.api.block.tileentity.Sign;
import org.spongepowered.api.data.manipulator.DataManipulator;
import org.spongepowered.api.entity.living.player.Player;

import java.util.Map;
import java.util.Optional;

/**
 * Specialised class for listening to sign changes when data is attached to them.
 * @param <D> The type of data.
 */
public abstract class SignDataListenerBase<D extends DataManipulator<?,?>> {

    @Inject
    protected NucleusPlugin plugin;

    public Map<String, PermissionInformation> getPermissions() {
        return Maps.newHashMap();
    }

    // Type Erasure necessitates this.
    protected abstract Class<D> getDataClass();

    public final boolean onBreak(Sign sign, Player player) {
        Optional<D> data = sign.get(getDataClass());
        return !data.isPresent() || onBreak(sign, data.get(), player);
    }

    public final boolean onInteract(Sign sign, Player player) {
        Optional<D> data = sign.get(getDataClass());
        return !data.isPresent() || onInteract(sign, data.get(), player);
    }

    protected abstract boolean onBreak(Sign sign, D data, Player player);

    protected abstract boolean onInteract(Sign sign, D data, Player player);
}
