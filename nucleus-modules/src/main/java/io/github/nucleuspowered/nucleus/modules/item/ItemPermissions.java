/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.item;

import io.github.nucleuspowered.nucleus.annotationprocessor.RegisterPermissions;
import io.github.nucleuspowered.nucleus.services.interfaces.annotation.PermissionMetadata;
import io.github.nucleuspowered.nucleus.services.interfaces.data.SuggestedLevel;

@RegisterPermissions
public class ItemPermissions {
    private ItemPermissions() {
        throw new AssertionError("Nope");
    }

    @PermissionMetadata(descriptionKey = "permission.base", replacements = { "enchant" }, level = SuggestedLevel.ADMIN)
    public static final String BASE_ENCHANT = "nucleus.enchant.base";

    @PermissionMetadata(descriptionKey = "permission.exempt.cooldown", replacements = { "enchant" }, level = SuggestedLevel.ADMIN)
    public static final String EXEMPT_COOLDOWN_ENCHANT = "nucleus.enchant.exempt.cooldown";

    @PermissionMetadata(descriptionKey = "permission.exempt.cost", replacements = { "enchant" }, level = SuggestedLevel.ADMIN)
    public static final String EXEMPT_COST_ENCHANT = "nucleus.enchant.exempt.cost";

    @PermissionMetadata(descriptionKey = "permission.exempt.warmup", replacements = { "enchant" }, level = SuggestedLevel.ADMIN)
    public static final String EXEMPT_WARMUP_ENCHANT = "nucleus.enchant.exempt.warmup";

    @PermissionMetadata(descriptionKey = "permission.enchant.unsafe", level = SuggestedLevel.ADMIN)
    public static final String ENCHANT_UNSAFE = "nucleus.enchant.unsafe";

    @PermissionMetadata(descriptionKey = "permission.base", replacements = { "itemname" }, level = SuggestedLevel.ADMIN)
    public static final String BASE_ITEMNAME = "nucleus.itemname.base";

    @PermissionMetadata(descriptionKey = "permission.base", replacements = { "itemname clear" }, level = SuggestedLevel.ADMIN)
    public static final String BASE_ITEMNAME_CLEAR = "nucleus.itemname.clear.base";

    @PermissionMetadata(descriptionKey = "permission.exempt.cooldown", replacements = { "itemname clear" }, level = SuggestedLevel.ADMIN)
    public static final String EXEMPT_COOLDOWN_ITEMNAME_CLEAR = "nucleus.itemname.clear.exempt.cooldown";

    @PermissionMetadata(descriptionKey = "permission.exempt.cost", replacements = { "itemname clear" }, level = SuggestedLevel.ADMIN)
    public static final String EXEMPT_COST_ITEMNAME_CLEAR = "nucleus.itemname.clear.exempt.cost";

    @PermissionMetadata(descriptionKey = "permission.exempt.warmup", replacements = { "itemname clear" }, level = SuggestedLevel.ADMIN)
    public static final String EXEMPT_WARMUP_ITEMNAME_CLEAR = "nucleus.itemname.clear.exempt.warmup";

    @PermissionMetadata(descriptionKey = "permission.base", replacements = { "itemname set" }, level = SuggestedLevel.ADMIN)
    public static final String BASE_ITEMNAME_SET = "nucleus.itemname.set.base";

    @PermissionMetadata(descriptionKey = "permission.exempt.cooldown", replacements = { "itemname set" }, level = SuggestedLevel.ADMIN)
    public static final String EXEMPT_COOLDOWN_ITEMNAME_SET = "nucleus.itemname.set.exempt.cooldown";

    @PermissionMetadata(descriptionKey = "permission.exempt.cost", replacements = { "itemname set" }, level = SuggestedLevel.ADMIN)
    public static final String EXEMPT_COST_ITEMNAME_SET = "nucleus.itemname.set.exempt.cost";

    @PermissionMetadata(descriptionKey = "permission.exempt.warmup", replacements = { "itemname set" }, level = SuggestedLevel.ADMIN)
    public static final String EXEMPT_WARMUP_ITEMNAME_SET = "nucleus.itemname.set.exempt.warmup";

    @PermissionMetadata(descriptionKey = "permission.base", replacements = { "lore" }, level = SuggestedLevel.ADMIN)
    public static final String BASE_LORE = "nucleus.lore.base";

    @PermissionMetadata(descriptionKey = "permission.base", replacements = { "lore insert" }, level = SuggestedLevel.ADMIN)
    public static final String BASE_LORE_SET = "nucleus.lore.set.base";

    @PermissionMetadata(descriptionKey = "permission.exempt.cooldown", replacements = { "lore insert" }, level = SuggestedLevel.ADMIN)
    public static final String EXEMPT_COOLDOWN_LORE_SET = "nucleus.lore.set.exempt.cooldown";

    @PermissionMetadata(descriptionKey = "permission.exempt.cost", replacements = { "lore insert" }, level = SuggestedLevel.ADMIN)
    public static final String EXEMPT_COST_LORE_SET = "nucleus.lore.set.exempt.cost";

    @PermissionMetadata(descriptionKey = "permission.exempt.warmup", replacements = { "lore insert" }, level = SuggestedLevel.ADMIN)
    public static final String EXEMPT_WARMUP_LORE_SET = "nucleus.lore.set.exempt.warmup";

    @PermissionMetadata(descriptionKey = "permission.base", replacements = { "more" }, level = SuggestedLevel.ADMIN)
    public static final String BASE_MORE = "nucleus.more.base";

    @PermissionMetadata(descriptionKey = "permission.exempt.cooldown", replacements = { "more" }, level = SuggestedLevel.ADMIN)
    public static final String EXEMPT_COOLDOWN_MORE = "nucleus.more.exempt.cooldown";

    @PermissionMetadata(descriptionKey = "permission.exempt.cost", replacements = { "more" }, level = SuggestedLevel.ADMIN)
    public static final String EXEMPT_COST_MORE = "nucleus.more.exempt.cost";

    @PermissionMetadata(descriptionKey = "permission.exempt.warmup", replacements = { "more" }, level = SuggestedLevel.ADMIN)
    public static final String EXEMPT_WARMUP_MORE = "nucleus.more.exempt.warmup";

    @PermissionMetadata(descriptionKey = "permission.base", replacements = { "repair" }, level = SuggestedLevel.ADMIN)
    public static final String BASE_REPAIR = "nucleus.repair.base";

    @PermissionMetadata(descriptionKey = "permission.exempt.cooldown", replacements = { "repair" }, level = SuggestedLevel.ADMIN)
    public static final String EXEMPT_COOLDOWN_REPAIR = "nucleus.repair.exempt.cooldown";

    @PermissionMetadata(descriptionKey = "permission.exempt.cost", replacements = { "repair" }, level = SuggestedLevel.ADMIN)
    public static final String EXEMPT_COST_REPAIR = "nucleus.repair.exempt.cost";

    @PermissionMetadata(descriptionKey = "permission.exempt.warmup", replacements = { "repair" }, level = SuggestedLevel.ADMIN)
    public static final String EXEMPT_WARMUP_REPAIR = "nucleus.repair.exempt.warmup";

    @PermissionMetadata(descriptionKey = "permission.repair.exempt.restriction", level = SuggestedLevel.ADMIN)
    public static final String EXEMPT_REPAIR_RESTRICTION_CHECK = "nucleus.repair.exempt.restriction";

    @PermissionMetadata(descriptionKey = "permission.repair.flag.all", level = SuggestedLevel.ADMIN)
    public static final String REPAIR_FLAG_ALL = "nucleus.repair.flag.all";

    @PermissionMetadata(descriptionKey = "permission.repair.flag.equip", level = SuggestedLevel.ADMIN)
    public static final String REPAIR_FLAG_EQUIP = "nucleus.repair.flag.equip";

    @PermissionMetadata(descriptionKey = "permission.repair.flag.hotbar", level = SuggestedLevel.ADMIN)
    public static final String REPAIR_FLAG_HOTBAR = "nucleus.repair.flag.hotbar";

    @PermissionMetadata(descriptionKey = "permission.repair.flag.offhand", level = SuggestedLevel.ADMIN)
    public static final String REPAIR_FLAG_OFFHAND = "nucleus.repair.flag.offhand";

    @PermissionMetadata(descriptionKey = "permission.others", replacements = { "repair" }, level = SuggestedLevel.ADMIN)
    public static final String OTHERS_REPAIR = "nucleus.repair.others";

    @PermissionMetadata(descriptionKey = "permission.base", replacements = { "showitemattributes" }, level = SuggestedLevel.ADMIN)
    public static final String BASE_SHOWITEMATTRIBUTES = "nucleus.showitemattributes.base";

    @PermissionMetadata(descriptionKey = "permission.exempt.cooldown", replacements = { "showitemattributes" }, level = SuggestedLevel.ADMIN)
    public static final String EXEMPT_COOLDOWN_SHOWITEMATTRIBUTES = "nucleus.showitemattributes.exempt.cooldown";

    @PermissionMetadata(descriptionKey = "permission.exempt.cost", replacements = { "showitemattributes" }, level = SuggestedLevel.ADMIN)
    public static final String EXEMPT_COST_SHOWITEMATTRIBUTES = "nucleus.showitemattributes.exempt.cost";

    @PermissionMetadata(descriptionKey = "permission.exempt.warmup", replacements = { "showitemattributes" }, level = SuggestedLevel.ADMIN)
    public static final String EXEMPT_WARMUP_SHOWITEMATTRIBUTES = "nucleus.showitemattributes.exempt.warmup";

    @PermissionMetadata(descriptionKey = "permission.base", replacements = { "skull" }, level = SuggestedLevel.ADMIN)
    public static final String BASE_SKULL = "nucleus.skull.base";

    @PermissionMetadata(descriptionKey = "permission.exempt.cooldown", replacements = { "skull" }, level = SuggestedLevel.ADMIN)
    public static final String EXEMPT_COOLDOWN_SKULL = "nucleus.skull.exempt.cooldown";

    @PermissionMetadata(descriptionKey = "permission.exempt.cost", replacements = { "skull" }, level = SuggestedLevel.ADMIN)
    public static final String EXEMPT_COST_SKULL = "nucleus.skull.exempt.cost";

    @PermissionMetadata(descriptionKey = "permission.skull.exempt.limit", level = SuggestedLevel.ADMIN)
    public static final String SKULL_EXEMPT_LIMIT = "nucleus.skull.exempt.limit";

    @PermissionMetadata(descriptionKey = "permission.exempt.warmup", replacements = { "skull" }, level = SuggestedLevel.ADMIN)
    public static final String EXEMPT_WARMUP_SKULL = "nucleus.skull.exempt.warmup";

    @PermissionMetadata(descriptionKey = "permission.others", replacements = { "skull" }, level = SuggestedLevel.ADMIN)
    public static final String OTHERS_SKULL = "nucleus.skull.others";

    @PermissionMetadata(descriptionKey = "permission.base", replacements = { "trash" }, level = SuggestedLevel.USER)
    public static final String BASE_TRASH = "nucleus.trash.base";

    @PermissionMetadata(descriptionKey = "permission.exempt.cooldown", replacements = { "trash" }, level = SuggestedLevel.ADMIN)
    public static final String EXEMPT_COOLDOWN_TRASH = "nucleus.trash.exempt.cooldown";

    @PermissionMetadata(descriptionKey = "permission.exempt.cost", replacements = { "trash" }, level = SuggestedLevel.ADMIN)
    public static final String EXEMPT_COST_TRASH = "nucleus.trash.exempt.cost";

    @PermissionMetadata(descriptionKey = "permission.exempt.warmup", replacements = { "trash" }, level = SuggestedLevel.ADMIN)
    public static final String EXEMPT_WARMUP_TRASH = "nucleus.trash.exempt.warmup";

    @PermissionMetadata(descriptionKey = "permission.base", replacements = { "unsignbook" }, level = SuggestedLevel.ADMIN)
    public static final String BASE_UNSIGNBOOK = "nucleus.unsignbook.base";

    @PermissionMetadata(descriptionKey = "permission.exempt.cooldown", replacements = { "unsignbook" }, level = SuggestedLevel.ADMIN)
    public static final String EXEMPT_COOLDOWN_UNSIGNBOOK = "nucleus.unsignbook.exempt.cooldown";

    @PermissionMetadata(descriptionKey = "permission.exempt.cost", replacements = { "unsignbook" }, level = SuggestedLevel.ADMIN)
    public static final String EXEMPT_COST_UNSIGNBOOK = "nucleus.unsignbook.exempt.cost";

    @PermissionMetadata(descriptionKey = "permission.exempt.warmup", replacements = { "unsignbook" }, level = SuggestedLevel.ADMIN)
    public static final String EXEMPT_WARMUP_UNSIGNBOOK = "nucleus.unsignbook.exempt.warmup";

    @PermissionMetadata(descriptionKey = "permission.others", replacements = { "unsignbook" }, level = SuggestedLevel.ADMIN)
    public static final String OTHERS_UNSIGNBOOK = "nucleus.unsignbook.others";

}
