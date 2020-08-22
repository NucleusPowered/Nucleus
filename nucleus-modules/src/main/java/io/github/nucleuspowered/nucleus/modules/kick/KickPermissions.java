/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.kick;

import io.github.nucleuspowered.nucleus.annotationprocessor.RegisterPermissions;
import io.github.nucleuspowered.nucleus.services.interfaces.annotation.LevelOptionMetadata;
import io.github.nucleuspowered.nucleus.services.interfaces.annotation.PermissionMetadata;
import io.github.nucleuspowered.nucleus.services.interfaces.data.SuggestedLevel;

@RegisterPermissions
public class KickPermissions {

    private KickPermissions() {
        throw new AssertionError("Nope");
    }

    @PermissionMetadata(descriptionKey = "permission.base", replacements = { "kick" }, level = SuggestedLevel.MOD)
    public static final String BASE_KICK = "nucleus.kick.base";

    @PermissionMetadata(descriptionKey = "permission.kick.exempt.target", level = SuggestedLevel.MOD)
    public static final String KICK_EXEMPT_TARGET = "nucleus.kick.exempt.target";

    @PermissionMetadata(descriptionKey = "permission.kick.notify", level = SuggestedLevel.MOD)
    public static final String KICK_NOTIFY = "nucleus.kick.notify";

    @PermissionMetadata(descriptionKey = "permission.base", replacements = { "kickall" }, level = SuggestedLevel.MOD)
    public static final String BASE_KICKALL = "nucleus.kickall.base";

    @PermissionMetadata(descriptionKey = "permission.kickall.whitelist", level = SuggestedLevel.ADMIN)
    public static final String KICKALL_WHITELIST = "nucleus.kickall.whitelist";

    @LevelOptionMetadata("optionlevel.kick")
    public static final String LEVEL_KEY = "nucleus.kick.level";

}
