/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.playerinfo;

import io.github.nucleuspowered.nucleus.annotationprocessor.RegisterPermissions;
import io.github.nucleuspowered.nucleus.services.interfaces.annotation.PermissionMetadata;
import io.github.nucleuspowered.nucleus.services.interfaces.data.SuggestedLevel;

@RegisterPermissions
public class    PlayerInfoPermissions {

    @PermissionMetadata(descriptionKey = "permission.base", replacements = { "getfromip" }, level = SuggestedLevel.ADMIN)
    public static final String BASE_GETFROMIP = "nucleus.getfromip.base";

    @PermissionMetadata(descriptionKey = "permission.base", replacements = { "getpos" }, level = SuggestedLevel.USER)
    public static final String BASE_GETPOS = "nucleus.getpos.base";

    @PermissionMetadata(descriptionKey = "permission.exempt.cooldown", replacements = { "getpos" }, level = SuggestedLevel.ADMIN)
    public static final String EXEMPT_COOLDOWN_GETPOS = "nucleus.getpos.exempt.cooldown";

    @PermissionMetadata(descriptionKey = "permission.exempt.cost", replacements = { "getpos" }, level = SuggestedLevel.ADMIN)
    public static final String EXEMPT_COST_GETPOS = "nucleus.getpos.exempt.cost";

    @PermissionMetadata(descriptionKey = "permission.exempt.warmup", replacements = { "getpos" }, level = SuggestedLevel.ADMIN)
    public static final String EXEMPT_WARMUP_GETPOS = "nucleus.getpos.exempt.warmup";

    @PermissionMetadata(descriptionKey = "permission.getpos.others", level = SuggestedLevel.MOD)
    public static final String GETPOS_OTHERS = "nucleus.getpos.others";

    @PermissionMetadata(descriptionKey = "permission.base", replacements = { "list" }, level = SuggestedLevel.USER)
    public static final String BASE_LIST = "nucleus.list.base";

    @PermissionMetadata(descriptionKey = "permission.exempt.cooldown", replacements = { "list" }, level = SuggestedLevel.ADMIN)
    public static final String EXEMPT_COOLDOWN_LIST = "nucleus.list.exempt.cooldown";

    @PermissionMetadata(descriptionKey = "permission.exempt.cost", replacements = { "list" }, level = SuggestedLevel.ADMIN)
    public static final String EXEMPT_COST_LIST = "nucleus.list.exempt.cost";

    @PermissionMetadata(descriptionKey = "permission.exempt.warmup", replacements = { "list" }, level = SuggestedLevel.ADMIN)
    public static final String EXEMPT_WARMUP_LIST = "nucleus.list.exempt.warmup";

    @PermissionMetadata(descriptionKey = "permission.list.seevanished", level = SuggestedLevel.ADMIN)
    public static final String LIST_SEEVANISHED = "nucleus.list.seevanished";

    @PermissionMetadata(descriptionKey = "permission.base", replacements = { "seen" }, level = SuggestedLevel.ADMIN)
    public static final String BASE_SEEN = "nucleus.seen.base";

    @PermissionMetadata(descriptionKey = "permission.seen.extendedperms.firstplayed", level = SuggestedLevel.ADMIN)
    public static final String SEEN_EXTENDEDPERMS_FIRSTPLAYED = "nucleus.seen.extended.firstplayed";

    @PermissionMetadata(descriptionKey = "permission.seen.extendedperms.canfly", level = SuggestedLevel.ADMIN)
    public static final String SEEN_EXTENDEDPERMS_CAN_FLY = "nucleus.seen.extended.flying.canfly";

    @PermissionMetadata(descriptionKey = "permission.seen.extendedperms.isflying", level = SuggestedLevel.ADMIN)
    public static final String SEEN_EXTENDEDPERMS_IS_FLYING = "nucleus.seen.extended.flying.isflying";

    @PermissionMetadata(descriptionKey = "permission.seen.extendedperms.gamemode", level = SuggestedLevel.ADMIN)
    public static final String SEEN_EXTENDEDPERMS_GAMEMODE = "nucleus.seen.extended.gamemode";

    @PermissionMetadata(descriptionKey = "permission.seen.extendedperms.ip", level = SuggestedLevel.ADMIN)
    public static final String SEEN_EXTENDEDPERMS_IP = "nucleus.seen.extended.ip";

    @PermissionMetadata(descriptionKey = "permission.seen.extendedperms.lastplayed", level = SuggestedLevel.ADMIN)
    public static final String SEEN_EXTENDEDPERMS_LASTPLAYED = "nucleus.seen.extended.lastplayed";

    @PermissionMetadata(descriptionKey = "permission.seen.extendedperms.location", level = SuggestedLevel.ADMIN)
    public static final String SEEN_EXTENDEDPERMS_LOCATION = "nucleus.seen.extended.location";

    @PermissionMetadata(descriptionKey = "permission.seen.extendedperms.speedflying", level = SuggestedLevel.ADMIN)
    public static final String SEEN_EXTENDEDPERMS_SPEED_FLYING = "nucleus.seen.extended.speed.flying";

    @PermissionMetadata(descriptionKey = "permission.seen.extendedperms.speedwalking", level = SuggestedLevel.ADMIN)
    public static final String SEEN_EXTENDEDPERMS_SPEED_WALKING = "nucleus.seen.extended.speed.walking";

    @PermissionMetadata(descriptionKey = "permission.seen.extendedperms.uuid", level = SuggestedLevel.ADMIN)
    public static final String SEEN_EXTENDEDPERMS_UUID = "nucleus.seen.extended.uuid";

    @PermissionMetadata(descriptionKey = "permission.base", replacements = { "near" }, level = SuggestedLevel.USER)
    public static final String BASE_NEAR = "nucleus.near.base";

    @PermissionMetadata(descriptionKey = "permission.others", replacements = { "near" }, level = SuggestedLevel.USER)
    public static final String OTHERS_NEAR = "nucleus.near.others";

    @PermissionMetadata(descriptionKey = "permission.exempt.warmup", replacements = { "near" }, level = SuggestedLevel.ADMIN)
    public static final String EXEMPT_WARMUP_NEAR = "nucleus.near.exempt.warmup";

    @PermissionMetadata(descriptionKey = "permission.exempt.cost", replacements = { "near" }, level = SuggestedLevel.ADMIN)
    public static final String EXEMPT_COST_NEAR = "nucleus.near.exempt.cost";

    @PermissionMetadata(descriptionKey = "permission.exempt.cooldown", replacements = { "near" }, level = SuggestedLevel.ADMIN)
    public static final String EXEMPT_COOLDOWN_NEAR = "nucleus.near.exempt.cooldown";

    @PermissionMetadata(descriptionKey = "permission.near.maxexempt", level = SuggestedLevel.ADMIN)
    public static final String EXEMPT_MAXRADIUS_NEAR = "nucleus.near.exempt.maxradius";

    private PlayerInfoPermissions() {
        throw new AssertionError("Nope");
    }

}
