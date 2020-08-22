/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.vanish;

import io.github.nucleuspowered.nucleus.annotationprocessor.RegisterPermissions;
import io.github.nucleuspowered.nucleus.services.interfaces.annotation.PermissionMetadata;
import io.github.nucleuspowered.nucleus.services.interfaces.data.SuggestedLevel;

@RegisterPermissions
public class VanishPermissions {
    private VanishPermissions() {
        throw new AssertionError("Nope");
    }

    @PermissionMetadata(descriptionKey = "permission.base", replacements = { "vanish" }, level = SuggestedLevel.ADMIN)
    public static final String BASE_VANISH = "nucleus.vanish.base";

    @PermissionMetadata(descriptionKey = "permission.vanish.onlogin", level = SuggestedLevel.NONE)
    public static final String VANISH_ONLOGIN = "nucleus.vanish.onlogin";

    @PermissionMetadata(descriptionKey = "permission.others", replacements = { "vanish" }, level = SuggestedLevel.ADMIN)
    public static final String OTHERS_VANISH = "nucleus.vanish.others";

    @PermissionMetadata(descriptionKey = "permission.vanish.persist", level = SuggestedLevel.ADMIN)
    public static final String VANISH_PERSIST = "nucleus.vanish.persist";

    @PermissionMetadata(descriptionKey = "permission.vanish.see", level = SuggestedLevel.ADMIN)
    public static final String VANISH_SEE = "nucleus.vanish.see";

}