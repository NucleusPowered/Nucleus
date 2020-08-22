/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.fun;

import io.github.nucleuspowered.nucleus.annotationprocessor.RegisterPermissions;
import io.github.nucleuspowered.nucleus.services.interfaces.annotation.PermissionMetadata;
import io.github.nucleuspowered.nucleus.services.interfaces.data.SuggestedLevel;

@RegisterPermissions
public class FunPermissions {
    private FunPermissions() {
        throw new AssertionError("Nope");
    }

    @PermissionMetadata(descriptionKey = "permission.base", replacements = { "hat" }, level = SuggestedLevel.ADMIN)
    public static final String BASE_HAT = "nucleus.hat.base";

    @PermissionMetadata(descriptionKey = "permission.others", replacements = { "hat" }, level = SuggestedLevel.ADMIN)
    public static final String OTHERS_HAT = "nucleus.hat.others";

    @PermissionMetadata(descriptionKey = "permission.base", replacements = { "ignite" }, level = SuggestedLevel.ADMIN)
    public static final String BASE_IGNITE = "nucleus.ignite.base";

    @PermissionMetadata(descriptionKey = "permission.exempt.cooldown", replacements = { "ignite" }, level = SuggestedLevel.ADMIN)
    public static final String EXEMPT_COOLDOWN_IGNITE = "nucleus.ignite.exempt.cooldown";

    @PermissionMetadata(descriptionKey = "permission.exempt.cost", replacements = { "ignite" }, level = SuggestedLevel.ADMIN)
    public static final String EXEMPT_COST_IGNITE = "nucleus.ignite.exempt.cost";

    @PermissionMetadata(descriptionKey = "permission.exempt.warmup", replacements = { "ignite" }, level = SuggestedLevel.ADMIN)
    public static final String EXEMPT_WARMUP_IGNITE = "nucleus.ignite.exempt.warmup";

    @PermissionMetadata(descriptionKey = "permission.others", replacements = { "ignite" }, level = SuggestedLevel.ADMIN)
    public static final String OTHERS_IGNITE = "nucleus.ignite.others";

    @PermissionMetadata(descriptionKey = "permission.base", replacements = { "kittycannon" }, level = SuggestedLevel.ADMIN)
    public static final String BASE_KITTYCANNON = "nucleus.kittycannon.base";

    @PermissionMetadata(descriptionKey = "permission.kittycannon.break", level = SuggestedLevel.ADMIN)
    public static final String KITTYCANNON_BREAK = "nucleus.kittycannon.break";

    @PermissionMetadata(descriptionKey = "permission.kittycannon.damage", level = SuggestedLevel.ADMIN)
    public static final String KITTYCANNON_DAMAGE = "nucleus.kittycannon.damage";

    @PermissionMetadata(descriptionKey = "permission.exempt.cooldown", replacements = { "kittycannon" }, level = SuggestedLevel.ADMIN)
    public static final String EXEMPT_COOLDOWN_KITTYCANNON = "nucleus.kittycannon.exempt.cooldown";

    @PermissionMetadata(descriptionKey = "permission.exempt.cost", replacements = { "kittycannon" }, level = SuggestedLevel.ADMIN)
    public static final String EXEMPT_COST_KITTYCANNON = "nucleus.kittycannon.exempt.cost";

    @PermissionMetadata(descriptionKey = "permission.exempt.warmup", replacements = { "kittycannon" }, level = SuggestedLevel.ADMIN)
    public static final String EXEMPT_WARMUP_KITTYCANNON = "nucleus.kittycannon.exempt.warmup";

    @PermissionMetadata(descriptionKey = "permission.kittycannon.fire", level = SuggestedLevel.ADMIN)
    public static final String KITTYCANNON_FIRE = "nucleus.kittycannon.fire";

    @PermissionMetadata(descriptionKey = "permission.others", replacements = { "kittycannon" }, level = SuggestedLevel.ADMIN)
    public static final String OTHERS_KITTYCANNON = "nucleus.kittycannon.others";

    @PermissionMetadata(descriptionKey = "permission.base", replacements = { "lightning" }, level = SuggestedLevel.ADMIN)
    public static final String BASE_LIGHTNING = "nucleus.lightning.base";

    @PermissionMetadata(descriptionKey = "permission.exempt.cooldown", replacements = { "lightning" }, level = SuggestedLevel.ADMIN)
    public static final String EXEMPT_COOLDOWN_LIGHTNING = "nucleus.lightning.exempt.cooldown";

    @PermissionMetadata(descriptionKey = "permission.exempt.cost", replacements = { "lightning" }, level = SuggestedLevel.ADMIN)
    public static final String EXEMPT_COST_LIGHTNING = "nucleus.lightning.exempt.cost";

    @PermissionMetadata(descriptionKey = "permission.exempt.warmup", replacements = { "lightning" }, level = SuggestedLevel.ADMIN)
    public static final String EXEMPT_WARMUP_LIGHTNING = "nucleus.lightning.exempt.warmup";

    @PermissionMetadata(descriptionKey = "permission.others", replacements = { "lightning" }, level = SuggestedLevel.ADMIN)
    public static final String OTHERS_LIGHTNING = "nucleus.lightning.others";

    @PermissionMetadata(descriptionKey = "permission.base", replacements = { "rocket" }, level = SuggestedLevel.ADMIN)
    public static final String BASE_ROCKET = "nucleus.rocket.base";

    @PermissionMetadata(descriptionKey = "permission.exempt.cooldown", replacements = { "rocket" }, level = SuggestedLevel.ADMIN)
    public static final String EXEMPT_COOLDOWN_ROCKET = "nucleus.rocket.exempt.cooldown";

    @PermissionMetadata(descriptionKey = "permission.exempt.cost", replacements = { "rocket" }, level = SuggestedLevel.ADMIN)
    public static final String EXEMPT_COST_ROCKET = "nucleus.rocket.exempt.cost";

    @PermissionMetadata(descriptionKey = "permission.exempt.warmup", replacements = { "rocket" }, level = SuggestedLevel.ADMIN)
    public static final String EXEMPT_WARMUP_ROCKET = "nucleus.rocket.exempt.warmup";

    @PermissionMetadata(descriptionKey = "permission.others", replacements = { "rocket" }, level = SuggestedLevel.ADMIN)
    public static final String OTHERS_ROCKET = "nucleus.rocket.others";

}

