/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.ban;

import io.github.nucleuspowered.nucleus.annotationprocessor.RegisterPermissions;
import io.github.nucleuspowered.nucleus.services.interfaces.annotation.LevelOptionMetadata;
import io.github.nucleuspowered.nucleus.services.interfaces.annotation.PermissionMetadata;
import io.github.nucleuspowered.nucleus.services.interfaces.data.SuggestedLevel;

@RegisterPermissions
public class BanPermissions {

    private BanPermissions() {
        throw new AssertionError("Nope");
    }

    @PermissionMetadata(descriptionKey = "permission.base", replacements = { "ban" }, level = SuggestedLevel.MOD)
    public static final String BASE_BAN = "nucleus.ban.base";

    @PermissionMetadata(descriptionKey = "permission.ban.exempt.target", level = SuggestedLevel.MOD)
    public static final String BAN_EXEMPT_TARGET = "nucleus.ban.exempt.target";

    @PermissionMetadata(descriptionKey = "permission.ban.notify", level = SuggestedLevel.MOD)
    public static final String BAN_NOTIFY = "nucleus.ban.notify";

    @PermissionMetadata(descriptionKey = "permission.ban.offline", level = SuggestedLevel.MOD)
    public static final String BAN_OFFLINE = "nucleus.ban.offline";

    @PermissionMetadata(descriptionKey = "permission.base", replacements = { "checkban" }, level = SuggestedLevel.MOD)
    public static final String BASE_CHECKBAN = "nucleus.checkban.base";

    @PermissionMetadata(descriptionKey = "permission.base", replacements = { "tempban" }, level = SuggestedLevel.MOD)
    public static final String BASE_TEMPBAN = "nucleus.tempban.base";

    @PermissionMetadata(descriptionKey = "permission.tempban.exempt.length", level = SuggestedLevel.MOD)
    public static final String TEMPBAN_EXEMPT_LENGTH = "nucleus.tempban.exempt.length";

    @PermissionMetadata(descriptionKey = "permission.tempban.exempt.target", level = SuggestedLevel.MOD)
    public static final String TEMPBAN_EXEMPT_TARGET = "nucleus.tempban.exempt.target";

    @PermissionMetadata(descriptionKey = "permission.tempban.offline", level = SuggestedLevel.MOD)
    public static final String TEMPBAN_OFFLINE = "nucleus.tempban.offline";

    @PermissionMetadata(descriptionKey = "permission.base", replacements = { "unban" }, level = SuggestedLevel.MOD)
    public static final String BASE_UNBAN = "nucleus.unban.base";

    @LevelOptionMetadata("optionlevel.ban")
    public static final String BAN_LEVEL_KEY = "nucleus.ban.level";

}