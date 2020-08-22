/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.freezeplayer;

import io.github.nucleuspowered.nucleus.annotationprocessor.RegisterPermissions;
import io.github.nucleuspowered.nucleus.services.interfaces.annotation.PermissionMetadata;
import io.github.nucleuspowered.nucleus.services.interfaces.data.SuggestedLevel;

@RegisterPermissions
public class FreezePlayerPermissions {

    private FreezePlayerPermissions() {
        throw new AssertionError("Nope");
    }

    @PermissionMetadata(descriptionKey = "permission.base", replacements = { "freezeplayer" }, level = SuggestedLevel.ADMIN)
    public static final String BASE_FREEZEPLAYER = "nucleus.freezeplayer.base";

    @PermissionMetadata(descriptionKey = "permission.exempt.cooldown", replacements = { "freezeplayer" }, level = SuggestedLevel.ADMIN)
    public static final String EXEMPT_COOLDOWN_FREEZEPLAYER = "nucleus.freezeplayer.exempt.cooldown";

    @PermissionMetadata(descriptionKey = "permission.exempt.cost", replacements = { "freezeplayer" }, level = SuggestedLevel.ADMIN)
    public static final String EXEMPT_COST_FREEZEPLAYER = "nucleus.freezeplayer.exempt.cost";

    @PermissionMetadata(descriptionKey = "permission.exempt.warmup", replacements = { "freezeplayer" }, level = SuggestedLevel.ADMIN)
    public static final String EXEMPT_WARMUP_FREEZEPLAYER = "nucleus.freezeplayer.exempt.warmup";

    @PermissionMetadata(descriptionKey = "permission.others", replacements = { "freezeplayer" }, level = SuggestedLevel.ADMIN)
    public static final String OTHERS_FREEZEPLAYER = "nucleus.freezeplayer.others";

}
