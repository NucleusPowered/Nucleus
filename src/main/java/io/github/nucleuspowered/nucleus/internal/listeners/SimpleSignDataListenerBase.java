/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.internal.listeners;

import com.google.common.collect.Maps;
import io.github.nucleuspowered.nucleus.internal.permissions.PermissionInformation;
import io.github.nucleuspowered.nucleus.internal.permissions.SuggestedLevel;
import org.spongepowered.api.block.tileentity.Sign;
import org.spongepowered.api.data.manipulator.DataManipulator;
import org.spongepowered.api.entity.living.player.Player;

import java.util.Map;

/**
 * Specialised class for listening to sign changes when data is attached to them.
 * @param <D> The type of data.
 */
public abstract class SimpleSignDataListenerBase<D extends DataManipulator<?,?>> extends SignDataListenerBase<D> {

    protected abstract String breakPermission();

    public final Map<String, PermissionInformation> getPermissions() {
        Map<String, PermissionInformation> map = Maps.newHashMap();
        map.put(breakPermission(), new PermissionInformation(plugin.getMessageProvider()
                .getMessageWithFormat("permission.signdata.listener.generic.break", getDataClass().getSimpleName()), SuggestedLevel.ADMIN));
        getAdditionalPermissions(map);
        return map;
    }

    protected void getAdditionalPermissions(Map<String, PermissionInformation> additionalPermissions) { }

    protected final boolean onBreak(Sign sign, D data, Player player) {
        return player.hasPermission(player.getActiveContexts(), breakPermission());
    }
}
