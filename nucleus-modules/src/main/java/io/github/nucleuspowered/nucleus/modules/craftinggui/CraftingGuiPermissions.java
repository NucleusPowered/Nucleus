/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.craftinggui;

import io.github.nucleuspowered.nucleus.annotationprocessor.RegisterPermissions;
import io.github.nucleuspowered.nucleus.services.interfaces.annotation.PermissionMetadata;
import io.github.nucleuspowered.nucleus.services.interfaces.data.SuggestedLevel;

@RegisterPermissions
public class CraftingGuiPermissions {

    private CraftingGuiPermissions() {
        throw new AssertionError("Nope");
    }

    @PermissionMetadata(descriptionKey = "permission.base", replacements = {"anvil"}, level = SuggestedLevel.ADMIN)
    public static final String BASE_ANVIL = "nucleus.anvil.base";

    @PermissionMetadata(descriptionKey = "permission.exempt.cooldown", replacements = {"anvil"}, level = SuggestedLevel.ADMIN)
    public static final String EXEMPT_COOLDOWN_ANVIL = "nucleus.anvil.exempt.cooldown";

    @PermissionMetadata(descriptionKey = "permission.exempt.cost", replacements = {"anvil"}, level = SuggestedLevel.ADMIN)
    public static final String EXEMPT_COST_ANVIL = "nucleus.anvil.exempt.cost";

    @PermissionMetadata(descriptionKey = "permission.exempt.warmup", replacements = {"anvil"}, level = SuggestedLevel.ADMIN)
    public static final String EXEMPT_WARMUP_ANVIL = "nucleus.anvil.exempt.warmup";

    @PermissionMetadata(descriptionKey = "permission.base", replacements = {"enchantingtable"}, level = SuggestedLevel.ADMIN)
    public static final String BASE_ENCHANTINGTABLE = "nucleus.enchantingtable.base";

    @PermissionMetadata(descriptionKey = "permission.exempt.cooldown", replacements = {"enchantingtable"}, level = SuggestedLevel.ADMIN)
    public static final String EXEMPT_COOLDOWN_ENCHANTINGTABLE = "nucleus.enchantingtable.exempt.cooldown";

    @PermissionMetadata(descriptionKey = "permission.exempt.cost", replacements = {"enchantingtable"}, level = SuggestedLevel.ADMIN)
    public static final String EXEMPT_COST_ENCHANTINGTABLE = "nucleus.enchantingtable.exempt.cost";

    @PermissionMetadata(descriptionKey = "permission.exempt.warmup", replacements = {"enchantingtable"}, level = SuggestedLevel.ADMIN)
    public static final String EXEMPT_WARMUP_ENCHANTINGTABLE = "nucleus.enchantingtable.exempt.warmup";

    @PermissionMetadata(descriptionKey = "permission.base", replacements = {"workbench"}, level = SuggestedLevel.ADMIN)
    public static final String BASE_WORKBENCH = "nucleus.workbench.base";

    @PermissionMetadata(descriptionKey = "permission.exempt.cooldown", replacements = {"workbench"}, level = SuggestedLevel.ADMIN)
    public static final String EXEMPT_COOLDOWN_WORKBENCH = "nucleus.workbench.exempt.cooldown";

    @PermissionMetadata(descriptionKey = "permission.exempt.cost", replacements = {"workbench"}, level = SuggestedLevel.ADMIN)
    public static final String EXEMPT_COST_WORKBENCH = "nucleus.workbench.exempt.cost";

    @PermissionMetadata(descriptionKey = "permission.exempt.warmup", replacements = {"workbench"}, level = SuggestedLevel.ADMIN)
    public static final String EXEMPT_WARMUP_WORKBENCH = "nucleus.workbench.exempt.warmup";

}
