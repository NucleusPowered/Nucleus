/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.experience;

import io.github.nucleuspowered.nucleus.annotationprocessor.RegisterPermissions;
import io.github.nucleuspowered.nucleus.services.interfaces.annotation.PermissionMetadata;
import io.github.nucleuspowered.nucleus.services.interfaces.data.SuggestedLevel;

@RegisterPermissions
public class ExperiencePermissions {

    @PermissionMetadata(descriptionKey = "permission.base", replacements = { "exp" }, level = SuggestedLevel.ADMIN)
    public static final String BASE_EXP = "nucleus.exp.base";

    @PermissionMetadata(descriptionKey = "permission.base", replacements = { "exp give" }, level = SuggestedLevel.ADMIN)
    public static final String BASE_EXP_GIVE = "nucleus.exp.give.base";

    @PermissionMetadata(descriptionKey = "permission.base", replacements = { "exp set" }, level = SuggestedLevel.ADMIN)
    public static final String BASE_EXP_SET = "nucleus.exp.set.base";

    @PermissionMetadata(descriptionKey = "permission.base", replacements = { "exp take" }, level = SuggestedLevel.ADMIN)
    public static final String BASE_EXP_TAKE = "nucleus.exp.take.base";

    @PermissionMetadata(descriptionKey = "permission.enchantment.keepxp", level = SuggestedLevel.NONE)
    public static final String KEEP_EXP_PERMISSION = "nucleus.exp.keepondeath";

    private ExperiencePermissions() {}

}
