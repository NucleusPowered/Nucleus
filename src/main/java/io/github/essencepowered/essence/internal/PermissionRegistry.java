/*
 * This file is part of Essence, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.essencepowered.essence.internal;

import com.google.common.collect.Maps;
import io.github.essencepowered.essence.PluginInfo;
import io.github.essencepowered.essence.internal.permissions.PermissionInformation;
import io.github.essencepowered.essence.internal.permissions.SuggestedLevel;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class PermissionRegistry {

    public final static String PERMISSIONS_PREFIX = PluginInfo.ID + ".";
    private final Map<Class<? extends CommandBase>, CommandPermissionHandler> serviceRegistry = Maps.newHashMap();
    private final Map<String, PermissionInformation> otherPermissions = Maps.newHashMap();

    public Optional<CommandPermissionHandler> getService(Class<? extends CommandBase> command) {
        return Optional.ofNullable(serviceRegistry.get(command));
    }

    public void addHandler(Class<? extends CommandBase> cb, CommandPermissionHandler cph) {
        if (serviceRegistry.containsKey(cb)) {
            // Silently discard.
            return;
        }

        serviceRegistry.put(cb, cph);
    }

    public void registerOtherPermission(String otherPermission, PermissionInformation pi) {
        if (otherPermissions.containsKey(otherPermission)) {
            // Silently discard.
            return;
        }

        otherPermissions.put(otherPermission, pi);
    }

    public void registerOtherPermission(String otherPermission, String description, SuggestedLevel level) {
        this.registerOtherPermission(otherPermission, new PermissionInformation(description, level));
    }

    public Map<String, PermissionInformation> getPermissions() {
        Map<String, PermissionInformation> m = new HashMap<>();
        serviceRegistry.values().forEach(x -> m.putAll(x.getSuggestedPermissions()));
        m.putAll(otherPermissions);
        return m;
    }
}
