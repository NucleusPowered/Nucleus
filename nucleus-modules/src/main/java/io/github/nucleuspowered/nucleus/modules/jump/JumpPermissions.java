/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.jump;

import io.github.nucleuspowered.nucleus.annotationprocessor.RegisterPermissions;
import io.github.nucleuspowered.nucleus.services.interfaces.annotation.PermissionMetadata;
import io.github.nucleuspowered.nucleus.services.interfaces.data.SuggestedLevel;

@RegisterPermissions
public class JumpPermissions {
    private JumpPermissions() {
        throw new AssertionError("Nope");
    }

    @PermissionMetadata(descriptionKey = "permission.base", replacements = { "jump" }, level = SuggestedLevel.ADMIN)
    public static final String BASE_JUMP = "nucleus.jump.base";

    @PermissionMetadata(descriptionKey = "permission.exempt.cooldown", replacements = { "jump" }, level = SuggestedLevel.ADMIN)
    public static final String EXEMPT_COOLDOWN_JUMP = "nucleus.jump.exempt.cooldown";

    @PermissionMetadata(descriptionKey = "permission.exempt.cost", replacements = { "jump" }, level = SuggestedLevel.ADMIN)
    public static final String EXEMPT_COST_JUMP = "nucleus.jump.exempt.cost";

    @PermissionMetadata(descriptionKey = "permission.exempt.warmup", replacements = { "jump" }, level = SuggestedLevel.ADMIN)
    public static final String EXEMPT_WARMUP_JUMP = "nucleus.jump.exempt.warmup";

    @PermissionMetadata(descriptionKey = "permission.base", replacements = { "thru" }, level = SuggestedLevel.ADMIN)
    public static final String BASE_THRU = "nucleus.thru.base";

    @PermissionMetadata(descriptionKey = "permission.exempt.cooldown", replacements = { "thru" }, level = SuggestedLevel.ADMIN)
    public static final String EXEMPT_COOLDOWN_THRU = "nucleus.thru.exempt.cooldown";

    @PermissionMetadata(descriptionKey = "permission.exempt.cost", replacements = { "thru" }, level = SuggestedLevel.ADMIN)
    public static final String EXEMPT_COST_THRU = "nucleus.thru.exempt.cost";

    @PermissionMetadata(descriptionKey = "permission.exempt.warmup", replacements = { "thru" }, level = SuggestedLevel.ADMIN)
    public static final String EXEMPT_WARMUP_THRU = "nucleus.thru.exempt.warmup";

    @PermissionMetadata(descriptionKey = "permission.base", replacements = { "top" }, level = SuggestedLevel.ADMIN)
    public static final String BASE_TOP = "nucleus.top.base";

    @PermissionMetadata(descriptionKey = "permission.exempt.cooldown", replacements = { "top" }, level = SuggestedLevel.ADMIN)
    public static final String EXEMPT_COOLDOWN_TOP = "nucleus.top.exempt.cooldown";

    @PermissionMetadata(descriptionKey = "permission.exempt.cost", replacements = { "top" }, level = SuggestedLevel.ADMIN)
    public static final String EXEMPT_COST_TOP = "nucleus.top.exempt.cost";

    @PermissionMetadata(descriptionKey = "permission.exempt.warmup", replacements = { "top" }, level = SuggestedLevel.ADMIN)
    public static final String EXEMPT_WARMUP_TOP = "nucleus.top.exempt.warmup";

    @PermissionMetadata(descriptionKey = "permission.others", replacements = { "top" }, level = SuggestedLevel.ADMIN)
    public static final String OTHERS_TOP = "nucleus.top.others";

    @PermissionMetadata(descriptionKey = "permission.base", replacements = { "unstuck" }, level = SuggestedLevel.ADMIN)
    public static final String BASE_UNSTUCK = "nucleus.unstuck.base";

    @PermissionMetadata(descriptionKey = "permission.exempt.cooldown", replacements = { "unstuck" }, level = SuggestedLevel.ADMIN)
    public static final String EXEMPT_COOLDOWN_UNSTUCK = "nucleus.unstuck.exempt.cooldown";

    @PermissionMetadata(descriptionKey = "permission.exempt.cost", replacements = { "unstuck" }, level = SuggestedLevel.ADMIN)
    public static final String EXEMPT_COST_UNSTUCK = "nucleus.unstuck.exempt.cost";

    @PermissionMetadata(descriptionKey = "permission.exempt.warmup", replacements = { "unstuck" }, level = SuggestedLevel.ADMIN)
    public static final String EXEMPT_WARMUP_UNSTUCK = "nucleus.unstuck.exempt.warmup";

}
