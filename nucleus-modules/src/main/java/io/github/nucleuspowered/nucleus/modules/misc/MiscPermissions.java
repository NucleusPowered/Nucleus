/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.misc;

import io.github.nucleuspowered.nucleus.annotationprocessor.RegisterPermissions;
import io.github.nucleuspowered.nucleus.services.interfaces.annotation.PermissionMetadata;
import io.github.nucleuspowered.nucleus.services.interfaces.data.SuggestedLevel;

@RegisterPermissions
public class MiscPermissions {
    private MiscPermissions() {
        throw new AssertionError("Nope");
    }

    @PermissionMetadata(descriptionKey = "permission.base", replacements = { "blockinfo" }, level = SuggestedLevel.ADMIN)
    public static final String BASE_BLOCKINFO = "nucleus.blockinfo.base";

    @PermissionMetadata(descriptionKey = "permission.blockinfo.extended", level = SuggestedLevel.ADMIN)
    public static final String BLOCKINFO_EXTENDED = "nucleus.blockinfo.extended";

    @PermissionMetadata(descriptionKey = "permission.base", replacements = { "entityinfo" }, level = SuggestedLevel.ADMIN)
    public static final String BASE_ENTITYINFO = "nucleus.entityinfo.base";

    @PermissionMetadata(descriptionKey = "permission.entityinfo.extended", level = SuggestedLevel.ADMIN)
    public static final String ENTITYINFO_EXTENDED = "nucleus.entityinfo.extended";

    @PermissionMetadata(descriptionKey = "permission.base", replacements = { "extinguish" }, level = SuggestedLevel.MOD)
    public static final String BASE_EXTINGUISH = "nucleus.extinguish.base";

    @PermissionMetadata(descriptionKey = "permission.exempt.cooldown", replacements = { "extinguish" }, level = SuggestedLevel.ADMIN)
    public static final String EXEMPT_COOLDOWN_EXTINGUISH = "nucleus.extinguish.exempt.cooldown";

    @PermissionMetadata(descriptionKey = "permission.exempt.cost", replacements = { "extinguish" }, level = SuggestedLevel.ADMIN)
    public static final String EXEMPT_COST_EXTINGUISH = "nucleus.extinguish.exempt.cost";

    @PermissionMetadata(descriptionKey = "permission.exempt.warmup", replacements = { "extinguish" }, level = SuggestedLevel.ADMIN)
    public static final String EXEMPT_WARMUP_EXTINGUISH = "nucleus.extinguish.exempt.warmup";

    @PermissionMetadata(descriptionKey = "permission.others", replacements = { "extinguish" }, level = SuggestedLevel.ADMIN)
    public static final String OTHERS_EXTINGUISH = "nucleus.extinguish.others";

    @PermissionMetadata(descriptionKey = "permission.base", replacements = { "feed" }, level = SuggestedLevel.ADMIN)
    public static final String BASE_FEED = "nucleus.feed.base";

    @PermissionMetadata(descriptionKey = "permission.exempt.cooldown", replacements = { "feed" }, level = SuggestedLevel.ADMIN)
    public static final String EXEMPT_COOLDOWN_FEED = "nucleus.feed.exempt.cooldown";

    @PermissionMetadata(descriptionKey = "permission.exempt.cost", replacements = { "feed" }, level = SuggestedLevel.ADMIN)
    public static final String EXEMPT_COST_FEED = "nucleus.feed.exempt.cost";

    @PermissionMetadata(descriptionKey = "permission.exempt.warmup", replacements = { "feed" }, level = SuggestedLevel.ADMIN)
    public static final String EXEMPT_WARMUP_FEED = "nucleus.feed.exempt.warmup";

    @PermissionMetadata(descriptionKey = "permission.others", replacements = { "feed" }, level = SuggestedLevel.ADMIN)
    public static final String OTHERS_FEED = "nucleus.feed.others";

    @PermissionMetadata(descriptionKey = "permission.base", replacements = { "heal" }, level = SuggestedLevel.ADMIN)
    public static final String BASE_HEAL = "nucleus.heal.base";

    @PermissionMetadata(descriptionKey = "permission.exempt.cooldown", replacements = { "heal" }, level = SuggestedLevel.ADMIN)
    public static final String EXEMPT_COOLDOWN_HEAL = "nucleus.heal.exempt.cooldown";

    @PermissionMetadata(descriptionKey = "permission.exempt.cost", replacements = { "heal" }, level = SuggestedLevel.ADMIN)
    public static final String EXEMPT_COST_HEAL = "nucleus.heal.exempt.cost";

    @PermissionMetadata(descriptionKey = "permission.exempt.warmup", replacements = { "heal" }, level = SuggestedLevel.ADMIN)
    public static final String EXEMPT_WARMUP_HEAL = "nucleus.heal.exempt.warmup";

    @PermissionMetadata(descriptionKey = "permission.others", replacements = { "heal" }, level = SuggestedLevel.ADMIN)
    public static final String OTHERS_HEAL = "nucleus.heal.others";

    @PermissionMetadata(descriptionKey = "permission.base", replacements = { "iteminfo" }, level = SuggestedLevel.ADMIN)
    public static final String BASE_ITEMINFO = "nucleus.iteminfo.base";

    @PermissionMetadata(descriptionKey = "permission.iteminfo.extended", level = SuggestedLevel.ADMIN)
    public static final String ITEMINFO_EXTENDED = "nucleus.iteminfo.extended";

    @PermissionMetadata(descriptionKey = "permission.base", replacements = { "ping" }, level = SuggestedLevel.ADMIN)
    public static final String BASE_PING = "nucleus.ping.base";

    @PermissionMetadata(descriptionKey = "permission.others", replacements = { "ping" }, level = SuggestedLevel.ADMIN)
    public static final String OTHERS_PING = "nucleus.ping.others";

    @PermissionMetadata(descriptionKey = "permission.base", replacements = { "serverstat" }, level = SuggestedLevel.ADMIN)
    public static final String BASE_SERVERSTAT = "nucleus.serverstat.base";

    @PermissionMetadata(descriptionKey = "permission.base", replacements = { "servertime" }, level = SuggestedLevel.ADMIN)
    public static final String BASE_SERVERTIME = "nucleus.servertime.base";

    @PermissionMetadata(descriptionKey = "permission.base", replacements = { "speed" }, level = SuggestedLevel.ADMIN)
    public static final String BASE_SPEED = "nucleus.speed.base";

    @PermissionMetadata(descriptionKey = "permission.exempt.cooldown", replacements = { "speed" }, level = SuggestedLevel.ADMIN)
    public static final String EXEMPT_COOLDOWN_SPEED = "nucleus.speed.exempt.cooldown";

    @PermissionMetadata(descriptionKey = "permission.exempt.cost", replacements = { "speed" }, level = SuggestedLevel.ADMIN)
    public static final String EXEMPT_COST_SPEED = "nucleus.speed.exempt.cost";

    @PermissionMetadata(descriptionKey = "permission.speed.exempt.max", level = SuggestedLevel.OWNER)
    public static final String SPEED_EXEMPT_MAX = "nucleus.speed.exempt.max";

    @PermissionMetadata(descriptionKey = "permission.exempt.warmup", replacements = { "speed" }, level = SuggestedLevel.ADMIN)
    public static final String EXEMPT_WARMUP_SPEED = "nucleus.speed.exempt.warmup";

    @PermissionMetadata(descriptionKey = "permission.others", replacements = { "speed" }, level = SuggestedLevel.ADMIN)
    public static final String OTHERS_SPEED = "nucleus.speed.others";

    @PermissionMetadata(descriptionKey = "permission.base", replacements = { "suicide" }, level = SuggestedLevel.USER)
    public static final String BASE_SUICIDE = "nucleus.suicide.base";

}
