/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.jail;

import io.github.nucleuspowered.nucleus.annotationprocessor.RegisterPermissions;
import io.github.nucleuspowered.nucleus.services.interfaces.annotation.LevelOptionMetadata;
import io.github.nucleuspowered.nucleus.services.interfaces.annotation.PermissionMetadata;
import io.github.nucleuspowered.nucleus.services.interfaces.data.SuggestedLevel;

@RegisterPermissions
public class JailPermissions {

    private JailPermissions() {
        throw new AssertionError("Nope");
    }

    @PermissionMetadata(descriptionKey = "permission.base", replacements = { "checkjailed" }, level = SuggestedLevel.ADMIN)
    public static final String BASE_CHECKJAILED = "nucleus.checkjailed.base";

    @PermissionMetadata(descriptionKey = "permission.base", replacements = { "jail" }, level = SuggestedLevel.MOD)
    public static final String BASE_JAIL = "nucleus.jail.base";

    @PermissionMetadata(descriptionKey = "permission.base", replacements = { "checkjail" }, level = SuggestedLevel.MOD)
    public static final String BASE_CHECKJAIL = "nucleus.jail.checkjail.base";

    @PermissionMetadata(descriptionKey = "permission.base", replacements = { "jails delete" }, level = SuggestedLevel.ADMIN)
    public static final String BASE_JAILS_DELETE = "nucleus.jail.delete.base";

    @PermissionMetadata(descriptionKey = "permission.jail.exempt.target", level = SuggestedLevel.ADMIN)
    public static final String JAIL_EXEMPT_TARGET = "nucleus.jail.exempt.target";

    @PermissionMetadata(descriptionKey = "permission.base", replacements = { "jails list" }, level = SuggestedLevel.MOD)
    public static final String BASE_JAILS_LIST = "nucleus.jail.list.base";

    @PermissionMetadata(descriptionKey = "permission.base", replacements = { "jails tp" }, level = SuggestedLevel.MOD)
    public static final String BASE_JAILS_TP = "nucleus.jail.tp.base";

    @PermissionMetadata(descriptionKey = "permission.jail.notify", level = SuggestedLevel.MOD)
    public static final String JAIL_NOTIFY = "nucleus.jail.notify";

    @PermissionMetadata(descriptionKey = "permission.jail.offline", level = SuggestedLevel.MOD)
    public static final String JAIL_OFFLINE = "nucleus.jail.offline";

    @PermissionMetadata(descriptionKey = "permission.base", replacements = { "jails set" }, level = SuggestedLevel.ADMIN)
    public static final String BASE_JAILS_SET = "nucleus.jail.set.base";

    @PermissionMetadata(descriptionKey = "permission.jail.teleportjailed", level = SuggestedLevel.ADMIN)
    public static final String JAIL_TELEPORTJAILED = "nucleus.jail.teleportjailed";

    @PermissionMetadata(descriptionKey = "permission.jail.teleporttojailed", level = SuggestedLevel.ADMIN)
    public static final String JAIL_TELEPORTTOJAILED = "nucleus.jail.teleporttojailed";

    @PermissionMetadata(descriptionKey = "permission.jail.unjail", level = SuggestedLevel.MOD)
    public static final String JAIL_UNJAIL = "nucleus.jail.unjail";

    @LevelOptionMetadata("optionlevel.jail")
    public static final String JAIL_LEVEL_KEY = "nucleus.jail.key";

}
