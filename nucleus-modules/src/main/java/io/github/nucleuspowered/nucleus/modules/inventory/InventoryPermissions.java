/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.inventory;

import io.github.nucleuspowered.nucleus.annotationprocessor.RegisterPermissions;
import io.github.nucleuspowered.nucleus.services.interfaces.annotation.PermissionMetadata;
import io.github.nucleuspowered.nucleus.services.interfaces.data.SuggestedLevel;

@RegisterPermissions
public class InventoryPermissions {
    private InventoryPermissions() {
        throw new AssertionError("Nope");
    }

    @PermissionMetadata(descriptionKey = "permission.base", replacements = { "clear" }, level = SuggestedLevel.ADMIN)
    public static final String BASE_CLEAR = "nucleus.clear.base";

    @PermissionMetadata(descriptionKey = "permission.others", replacements = { "clear" }, level = SuggestedLevel.ADMIN)
    public static final String OTHERS_CLEAR = "nucleus.clear.others";

    @PermissionMetadata(descriptionKey = "permission.base", replacements = { "enderchest" }, level = SuggestedLevel.ADMIN)
    public static final String BASE_ENDERCHEST = "nucleus.enderchest.base";

    @PermissionMetadata(descriptionKey = "permission.exempt.cooldown", replacements = { "enderchest" }, level = SuggestedLevel.ADMIN)
    public static final String EXEMPT_COOLDOWN_ENDERCHEST = "nucleus.enderchest.exempt.cooldown";

    @PermissionMetadata(descriptionKey = "permission.exempt.cost", replacements = { "enderchest" }, level = SuggestedLevel.ADMIN)
    public static final String EXEMPT_COST_ENDERCHEST = "nucleus.enderchest.exempt.cost";

    @PermissionMetadata(descriptionKey = "permission.enderchest.exempt.modify", level = SuggestedLevel.ADMIN)
    public static final String ENDERCHEST_EXEMPT_MODIFY = "nucleus.enderchest.exempt.modify";

    @PermissionMetadata(descriptionKey = "permission.enderchest.exempt.inspect", level = SuggestedLevel.ADMIN)
    public static final String ENDERCHEST_EXEMPT_INSPECT = "nucleus.enderchest.exempt.target";

    @PermissionMetadata(descriptionKey = "permission.exempt.warmup", replacements = { "enderchest" }, level = SuggestedLevel.ADMIN)
    public static final String EXEMPT_WARMUP_ENDERCHEST = "nucleus.enderchest.exempt.warmup";

    @PermissionMetadata(descriptionKey = "permission.enderchest.modify", level = SuggestedLevel.ADMIN)
    public static final String ENDERCHEST_MODIFY = "nucleus.enderchest.modify";

    @PermissionMetadata(descriptionKey = "permission.enderchest.offline", level = SuggestedLevel.ADMIN)
    public static final String ENDERCHEST_OFFLINE = "nucleus.enderchest.offline";

    @PermissionMetadata(descriptionKey = "permission.others", replacements = { "enderchest" }, level = SuggestedLevel.ADMIN)
    public static final String OTHERS_ENDERCHEST = "nucleus.enderchest.others";

    @PermissionMetadata(descriptionKey = "permission.inventory.keep", level = SuggestedLevel.NONE)
    public static final String INVENTORY_KEEP = "nucleus.inventory.keepondeath";

    @PermissionMetadata(descriptionKey = "permission.base", replacements = { "invsee" }, level = SuggestedLevel.ADMIN)
    public static final String BASE_INVSEE = "nucleus.invsee.base";

    @PermissionMetadata(descriptionKey = "permission.invsee.exempt.interact", level = SuggestedLevel.ADMIN)
    public static final String INVSEE_EXEMPT_INTERACT = "nucleus.invsee.exempt.interact";

    @PermissionMetadata(descriptionKey = "permission.invsee.exempt.inspect", level = SuggestedLevel.ADMIN)
    public static final String INVSEE_EXEMPT_INSPECT = "nucleus.invsee.exempt.target";

    @PermissionMetadata(descriptionKey = "permission.invsee.modify", level = SuggestedLevel.ADMIN)
    public static final String INVSEE_MODIFY = "nucleus.invsee.modify";

    @PermissionMetadata(descriptionKey = "permission.invsee.offline", level = SuggestedLevel.ADMIN)
    public static final String INVSEE_OFFLINE = "nucleus.invsee.offline";

}
