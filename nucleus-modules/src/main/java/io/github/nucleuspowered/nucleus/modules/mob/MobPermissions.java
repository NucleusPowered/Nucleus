/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.mob;

import io.github.nucleuspowered.nucleus.annotationprocessor.RegisterPermissions;
import io.github.nucleuspowered.nucleus.services.interfaces.annotation.PermissionMetadata;
import io.github.nucleuspowered.nucleus.services.interfaces.data.SuggestedLevel;
import org.spongepowered.api.entity.EntityType;

@RegisterPermissions
public class MobPermissions {
    private MobPermissions() {
        throw new AssertionError("Nope");
    }

    @PermissionMetadata(descriptionKey = "permission.base", replacements = { "spawnmob" }, level = SuggestedLevel.ADMIN)
    public static final String BASE_SPAWNMOB = "nucleus.spawnmob.base";

    @PermissionMetadata(descriptionKey = "permission.exempt.cooldown", replacements = { "spawnmob" }, level = SuggestedLevel.ADMIN)
    public static final String EXEMPT_COOLDOWN_SPAWNMOB = "nucleus.spawnmob.exempt.cooldown";

    @PermissionMetadata(descriptionKey = "permission.exempt.cost", replacements = { "spawnmob" }, level = SuggestedLevel.ADMIN)
    public static final String EXEMPT_COST_SPAWNMOB = "nucleus.spawnmob.exempt.cost";

    @PermissionMetadata(descriptionKey = "permission.exempt.warmup", replacements = { "spawnmob" }, level = SuggestedLevel.ADMIN)
    public static final String EXEMPT_WARMUP_SPAWNMOB = "nucleus.spawnmob.exempt.warmup";

    @PermissionMetadata(descriptionKey = "permission.spawnmob.mob", level = SuggestedLevel.ADMIN, isPrefix = true, isConsoleBypassable = true)
    public static final String SPAWNMOB_MOB = "nucleus.spawnmob.mob";

    @PermissionMetadata(descriptionKey = "permission.others", replacements = { "spawnmob" }, level = SuggestedLevel.ADMIN)
    public static final String OTHERS_SPAWNMOB = "nucleus.spawnmob.others";

    public static String getSpawnMobPermissionFor(EntityType entityType) {
        return SPAWNMOB_MOB + "." + entityType.getId().toLowerCase().replace(":", ".");
    }

}
