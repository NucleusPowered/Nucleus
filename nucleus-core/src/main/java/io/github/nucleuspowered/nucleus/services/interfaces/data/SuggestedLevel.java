/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.services.interfaces.data;

import org.spongepowered.api.service.permission.PermissionDescription;

import javax.annotation.Nullable;

public enum SuggestedLevel {
    USER(null, "nucleus.user", PermissionDescription.ROLE_USER),
    MOD(USER, "nucleus.mod", PermissionDescription.ROLE_STAFF),
    ADMIN(MOD, "nucleus.admin", PermissionDescription.ROLE_ADMIN),
    OWNER(ADMIN, "nucleus.owner", "OWNER"),
    NONE(OWNER, null, "SPECIAL");

    @Nullable private final SuggestedLevel level;
    @Nullable private final String permission;
    @Nullable private final String role;

    SuggestedLevel(@Nullable SuggestedLevel level, @Nullable String permission, @Nullable String role) {
        this.level = level;
        this.permission = permission;
        this.role = role;
    }

    @Nullable
    public SuggestedLevel getLowerLevel() {
        return this.level;
    }

    @Nullable
    public String getPermission() {
        return this.permission;
    }

    @Nullable
    public String getSpongeRole() {
        return this.role;
    }
}
