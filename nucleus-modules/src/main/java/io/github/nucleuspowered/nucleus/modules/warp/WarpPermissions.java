/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.warp;

import io.github.nucleuspowered.nucleus.annotationprocessor.RegisterPermissions;
import io.github.nucleuspowered.nucleus.services.interfaces.annotation.PermissionMetadata;
import io.github.nucleuspowered.nucleus.services.interfaces.data.SuggestedLevel;

@RegisterPermissions
public class WarpPermissions {
    private WarpPermissions() {
        throw new AssertionError("Nope");
    }

    @PermissionMetadata(descriptionKey = "permission.base", replacements = { "warp" }, level = SuggestedLevel.USER)
    public static final String BASE_WARP = "nucleus.warp.base";

    @PermissionMetadata(descriptionKey = "permission.base", replacements = { "warp category" }, level = SuggestedLevel.ADMIN)
    public static final String BASE_WARP_CATEGORY = "nucleus.warp.category.base";

    @PermissionMetadata(descriptionKey = "permission.base", replacements = { "category description" }, level = SuggestedLevel.ADMIN)
    public static final String BASE_CATEGORY_DESCRIPTION = "nucleus.warp.category.description.base";

    @PermissionMetadata(descriptionKey = "permission.base", replacements = { "category displayname" }, level = SuggestedLevel.ADMIN)
    public static final String BASE_CATEGORY_DISPLAYNAME = "nucleus.warp.category.displayname.base";

    @PermissionMetadata(descriptionKey = "permission.base", replacements = { "category list" }, level = SuggestedLevel.ADMIN)
    public static final String BASE_CATEGORY_LIST = "nucleus.warp.category.list.base";

    @PermissionMetadata(descriptionKey = "permission.base", replacements = { "warp cost" }, level = SuggestedLevel.ADMIN)
    public static final String BASE_WARP_COST = "nucleus.warp.cost.base";

    @PermissionMetadata(descriptionKey = "permission.base", replacements = { "warp delete" }, level = SuggestedLevel.ADMIN)
    public static final String BASE_WARP_DELETE = "nucleus.warp.delete.base";

    @PermissionMetadata(descriptionKey = "permission.exempt.cooldown", replacements = { "warp" }, level = SuggestedLevel.ADMIN)
    public static final String EXEMPT_COOLDOWN_WARP = "nucleus.warp.exempt.cooldown";

    @PermissionMetadata(descriptionKey = "permission.exempt.warmup", replacements = { "warp" }, level = SuggestedLevel.ADMIN)
    public static final String EXEMPT_WARMUP_WARP = "nucleus.warp.exempt.warmup";

    @PermissionMetadata(descriptionKey = "permission.exempt.cost", replacements = { "warp" }, level = SuggestedLevel.ADMIN)
    public static final String EXEMPT_COST_WARP = "nucleus.warp.exempt.cost";

    @PermissionMetadata(descriptionKey = "permission.base", replacements = { "warp list" }, level = SuggestedLevel.USER)
    public static final String BASE_WARP_LIST = "nucleus.warp.list.base";

    @PermissionMetadata(descriptionKey = "permission.others", replacements = { "warp" }, level = SuggestedLevel.ADMIN)
    public static final String OTHERS_WARP = "nucleus.warp.others";

    @PermissionMetadata(descriptionKey = "permission.base", replacements = { "warp set" }, level = SuggestedLevel.ADMIN)
    public static final String BASE_WARP_SET = "nucleus.warp.set.base";

    @PermissionMetadata(descriptionKey = "permission.exempt.cooldown", replacements = { "warp set" }, level = SuggestedLevel.ADMIN)
    public static final String EXEMPT_COOLDOWN_WARP_SET = "nucleus.warp.set.exempt.cooldown";

    @PermissionMetadata(descriptionKey = "permission.exempt.cost", replacements = { "warp set" }, level = SuggestedLevel.ADMIN)
    public static final String EXEMPT_COST_WARP_SET = "nucleus.warp.set.exempt.cost";

    @PermissionMetadata(descriptionKey = "permission.exempt.warmup", replacements = { "warp set" }, level = SuggestedLevel.ADMIN)
    public static final String EXEMPT_WARMUP_WARP_SET = "nucleus.warp.set.exempt.warmup";

    @PermissionMetadata(descriptionKey = "permission.base", replacements = { "warp setcategory" }, level = SuggestedLevel.ADMIN)
    public static final String BASE_WARP_SETCATEGORY = "nucleus.warp.setcategory.base";

    @PermissionMetadata(descriptionKey = "permission.base", replacements = { "warp setdescription" }, level = SuggestedLevel.ADMIN)
    public static final String BASE_WARP_SETDESCRIPTION = "nucleus.warp.setdescription.base";

    @PermissionMetadata(descriptionKey = "permissions.warps", level = SuggestedLevel.ADMIN, isPrefix = true)
    public static final String PERMISSIONS_WARPS = "nucleus.warps";

    public static String getWarpPermission(String warpName) {
        return PERMISSIONS_WARPS + "." + warpName.toLowerCase();
    }

}
