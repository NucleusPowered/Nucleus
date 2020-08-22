/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.kit;

import io.github.nucleuspowered.nucleus.annotationprocessor.RegisterPermissions;
import io.github.nucleuspowered.nucleus.services.interfaces.annotation.PermissionMetadata;
import io.github.nucleuspowered.nucleus.services.interfaces.data.SuggestedLevel;

@RegisterPermissions
public class KitPermissions {
    private KitPermissions() {
        throw new AssertionError("Nope");
    }

    @PermissionMetadata(descriptionKey = "permission.base", replacements = { "kit autoredeem" }, level = SuggestedLevel.ADMIN)
    public static final String BASE_KIT_AUTOREDEEM = "nucleus.kit.autoredeem.base";

    @PermissionMetadata(descriptionKey = "permission.base", replacements = { "kit" }, level = SuggestedLevel.USER)
    public static final String BASE_KIT = "nucleus.kit.base";

    @PermissionMetadata(descriptionKey = "permission.base", replacements = { "kit reload" }, level = SuggestedLevel.OWNER)
    public static final String BASE_KIT_RELOAD = "nucleus.kit.reload.base";

    @PermissionMetadata(descriptionKey = "permission.base", replacements = { "command add" }, level = SuggestedLevel.OWNER)
    public static final String BASE_KIT_COMMAND_ADD = "nucleus.kit.command.add.base";

    @PermissionMetadata(descriptionKey = "permission.base", replacements = { "kit command" }, level = SuggestedLevel.ADMIN)
    public static final String BASE_KIT_COMMAND = "nucleus.kit.command.base";

    @PermissionMetadata(descriptionKey = "permission.base", replacements = { "command clear" }, level = SuggestedLevel.OWNER)
    public static final String BASE_KIT_COMMAND_REMOVE = "nucleus.kit.command.remove.base";

    @PermissionMetadata(descriptionKey = "permission.base", replacements = { "kit cost" }, level = SuggestedLevel.ADMIN)
    public static final String BASE_KIT_COST = "nucleus.kit.cost.base";

    @PermissionMetadata(descriptionKey = "permission.base", replacements = { "kit create" }, level = SuggestedLevel.ADMIN)
    public static final String BASE_KIT_CREATE = "nucleus.kit.create.base";

    @PermissionMetadata(descriptionKey = "permission.base", replacements = { "kit edit" }, level = SuggestedLevel.ADMIN)
    public static final String BASE_KIT_EDIT = "nucleus.kit.edit.base";

    @PermissionMetadata(descriptionKey = "permission.kit.exempt.cooldown", level = SuggestedLevel.ADMIN)
    public static final String KIT_EXEMPT_COOLDOWN = "nucleus.kit.exempt.cooldown";

    @PermissionMetadata(descriptionKey = "permission.kit.exempt.cost", level = SuggestedLevel.ADMIN)
    public static final String KIT_EXEMPT_COST = "nucleus.kit.exempt.cost";

    @PermissionMetadata(descriptionKey = "permission.kit.exempt.onetime", level = SuggestedLevel.ADMIN)
    public static final String KIT_EXEMPT_ONETIME = "nucleus.kit.exempt.onetime";

    @PermissionMetadata(descriptionKey = "permission.exempt.warmup", replacements = { "kit" }, level = SuggestedLevel.ADMIN)
    public static final String EXEMPT_WARMUP_KIT = "nucleus.kit.exempt.warmup";

    @PermissionMetadata(descriptionKey = "permission.base", replacements = { "kit give" }, level = SuggestedLevel.ADMIN)
    public static final String BASE_KIT_GIVE = "nucleus.kit.give.base";

    @PermissionMetadata(descriptionKey = "permission.kit.give.override", level = SuggestedLevel.ADMIN)
    public static final String KIT_GIVE_OVERRIDE = "nucleus.kit.give.overridecheck";

    @PermissionMetadata(descriptionKey = "permission.base", replacements = { "kit hidden" }, level = SuggestedLevel.ADMIN)
    public static final String BASE_KIT_HIDDEN = "nucleus.kit.hidden.base";

    @PermissionMetadata(descriptionKey = "permission.base", replacements = { "kit info" }, level = SuggestedLevel.ADMIN)
    public static final String BASE_KIT_INFO = "nucleus.kit.info.base";

    @PermissionMetadata(descriptionKey = "permission.base", replacements = { "kit list" }, level = SuggestedLevel.USER)
    public static final String BASE_KIT_LIST = "nucleus.kit.list.base";

    @PermissionMetadata(descriptionKey = "permission.base", replacements = { "kit onetime" }, level = SuggestedLevel.ADMIN)
    public static final String BASE_KIT_ONETIME = "nucleus.kit.onetime.base";

    @PermissionMetadata(descriptionKey = "permission.base", replacements = { "kit permissionbypass" }, level = SuggestedLevel.ADMIN)
    public static final String BASE_KIT_PERMISSIONBYPASS = "nucleus.kit.permissionbypass.base";

    @PermissionMetadata(descriptionKey = "permission.base", replacements = { "kit remove" }, level = SuggestedLevel.ADMIN)
    public static final String BASE_KIT_REMOVE = "nucleus.kit.remove.base";

    @PermissionMetadata(descriptionKey = "permission.base", replacements = { "kit rename" }, level = SuggestedLevel.ADMIN)
    public static final String BASE_KIT_RENAME = "nucleus.kit.rename.base";

    @PermissionMetadata(descriptionKey = "permission.base", replacements = { "kit resetusage" }, level = SuggestedLevel.ADMIN)
    public static final String BASE_KIT_RESETUSAGE = "nucleus.kit.resetusage.base";

    @PermissionMetadata(descriptionKey = "permission.base", replacements = { "kit set" }, level = SuggestedLevel.ADMIN)
    public static final String BASE_KIT_SET = "nucleus.kit.set.base";

    @PermissionMetadata(descriptionKey = "permission.base", replacements = { "kit setcooldown" }, level = SuggestedLevel.ADMIN)
    public static final String BASE_KIT_SETCOOLDOWN = "nucleus.kit.setcooldown.base";

    @PermissionMetadata(descriptionKey = "permission.base", replacements = { "kit setfirstjoin" }, level = SuggestedLevel.ADMIN)
    public static final String BASE_KIT_SETFIRSTJOIN = "nucleus.kit.setfirstjoin.base";

    @PermissionMetadata(descriptionKey = "permission.kit.showhidden", level = SuggestedLevel.ADMIN)
    public static final String KIT_SHOWHIDDEN = "nucleus.kit.showhidden";

    @PermissionMetadata(descriptionKey = "permission.base", replacements = { "kit toggleredeemmessage" }, level = SuggestedLevel.ADMIN)
    public static final String BASE_KIT_TOGGLEREDEEMMESSAGE = "nucleus.kit.toggleredeemmessage.base";

    @PermissionMetadata(descriptionKey = "permission.base", replacements = { "kit view" }, level = SuggestedLevel.ADMIN)
    public static final String BASE_KIT_VIEW = "nucleus.kit.view.base";

    @PermissionMetadata(descriptionKey = "permission.kits", level = SuggestedLevel.ADMIN, isPrefix = true)
    public static final String KITS = "nucleus.kits";

    public static String getKitPermission(String kit) {
        return KITS + "." + kit.toLowerCase();
    }

}
