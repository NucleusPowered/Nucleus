/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.admin;

import io.github.nucleuspowered.nucleus.annotationprocessor.RegisterPermissions;
import io.github.nucleuspowered.nucleus.services.interfaces.annotation.PermissionMetadata;
import io.github.nucleuspowered.nucleus.services.interfaces.data.SuggestedLevel;

@RegisterPermissions
public class AdminPermissions {

    private AdminPermissions() {
        throw new AssertionError("Nope");
    }

    @PermissionMetadata(descriptionKey = "permission.base", replacements = { "blockzap" }, level = SuggestedLevel.ADMIN)
    public static final String BASE_BLOCKZAP = "nucleus.blockzap.base";

    @PermissionMetadata(descriptionKey = "permission.exempt.cooldown", replacements = { "blockzap" }, level = SuggestedLevel.ADMIN)
    public static final String EXEMPT_COOLDOWN_BLOCKZAP = "nucleus.blockzap.exempt.cooldown";

    @PermissionMetadata(descriptionKey = "permission.exempt.cost", replacements = { "blockzap" }, level = SuggestedLevel.ADMIN)
    public static final String EXEMPT_COST_BLOCKZAP = "nucleus.blockzap.exempt.cost";

    @PermissionMetadata(descriptionKey = "permission.exempt.warmup", replacements = { "blockzap" }, level = SuggestedLevel.ADMIN)
    public static final String EXEMPT_WARMUP_BLOCKZAP = "nucleus.blockzap.exempt.warmup";

    @PermissionMetadata(descriptionKey = "permission.base", replacements = { "gamemode" }, level = SuggestedLevel.ADMIN)
    public static final String BASE_GAMEMODE = "nucleus.gamemode.base";

    @PermissionMetadata(descriptionKey = "permission.exempt.cooldown", replacements = { "gamemode" }, level = SuggestedLevel.ADMIN)
    public static final String EXEMPT_COOLDOWN_GAMEMODE = "nucleus.gamemode.exempt.cooldown";

    @PermissionMetadata(descriptionKey = "permission.exempt.cost", replacements = { "gamemode" }, level = SuggestedLevel.ADMIN)
    public static final String EXEMPT_COST_GAMEMODE = "nucleus.gamemode.exempt.cost";

    @PermissionMetadata(descriptionKey = "permission.exempt.warmup", replacements = { "gamemode" }, level = SuggestedLevel.ADMIN)
    public static final String EXEMPT_WARMUP_GAMEMODE = "nucleus.gamemode.exempt.warmup";

    @PermissionMetadata(descriptionKey = "permission.gamemode.modes.root", level = SuggestedLevel.ADMIN, isPrefix = true)
    public static final String GAMEMODE_MODES_ROOT = "nucleus.gamemode.modes";

    @PermissionMetadata(descriptionKey = "permission.gamemode.modes.adventure", level = SuggestedLevel.ADMIN)
    public static final String GAMEMODE_MODES_ADVENTURE = GAMEMODE_MODES_ROOT + ".adventure";

    @PermissionMetadata(descriptionKey = "permission.gamemode.modes.creative", level = SuggestedLevel.ADMIN)
    public static final String GAMEMODE_MODES_CREATIVE = GAMEMODE_MODES_ROOT +  ".creative";

    @PermissionMetadata(descriptionKey = "permission.gamemode.modes.spectator", level = SuggestedLevel.ADMIN)
    public static final String GAMEMODE_MODES_SPECTATOR = GAMEMODE_MODES_ROOT + ".spectator";

    @PermissionMetadata(descriptionKey = "permission.gamemode.modes.survival", level = SuggestedLevel.ADMIN)
    public static final String GAMEMODE_MODES_SURVIVAL = GAMEMODE_MODES_ROOT + ".survival";

    @PermissionMetadata(descriptionKey = "permission.gamemode.other", level = SuggestedLevel.ADMIN)
    public static final String GAMEMODE_OTHER = "nucleus.gamemode.others";

    @PermissionMetadata(descriptionKey = "permission.base", replacements = { "kill" }, level = SuggestedLevel.ADMIN)
    public static final String BASE_KILL = "nucleus.kill.base";

    @PermissionMetadata(descriptionKey = "permission.exempt.cooldown", replacements = { "kill" }, level = SuggestedLevel.ADMIN)
    public static final String EXEMPT_COOLDOWN_KILL = "nucleus.kill.exempt.cooldown";

    @PermissionMetadata(descriptionKey = "permission.exempt.cost", replacements = { "kill" }, level = SuggestedLevel.ADMIN)
    public static final String EXEMPT_COST_KILL = "nucleus.kill.exempt.cost";

    @PermissionMetadata(descriptionKey = "permission.exempt.warmup", replacements = { "kill" }, level = SuggestedLevel.ADMIN)
    public static final String EXEMPT_WARMUP_KILL = "nucleus.kill.exempt.warmup";

    @PermissionMetadata(descriptionKey = "permission.base", replacements = { "killentity" }, level = SuggestedLevel.ADMIN)
    public static final String BASE_KILLENTITY = "nucleus.killentity.base";

    @PermissionMetadata(descriptionKey = "permission.exempt.cooldown", replacements = { "killentity" }, level = SuggestedLevel.ADMIN)
    public static final String EXEMPT_COOLDOWN_KILLENTITY = "nucleus.killentity.exempt.cooldown";

    @PermissionMetadata(descriptionKey = "permission.exempt.cost", replacements = { "killentity" }, level = SuggestedLevel.ADMIN)
    public static final String EXEMPT_COST_KILLENTITY = "nucleus.killentity.exempt.cost";

    @PermissionMetadata(descriptionKey = "permission.exempt.warmup", replacements = { "killentity" }, level = SuggestedLevel.ADMIN)
    public static final String EXEMPT_WARMUP_KILLENTITY = "nucleus.killentity.exempt.warmup";

    @PermissionMetadata(descriptionKey = "permission.base", replacements = { "stop" }, level = SuggestedLevel.ADMIN)
    public static final String BASE_STOP = "nucleus.stop.base";

    @PermissionMetadata(descriptionKey = "permission.base", replacements = { "sudo" }, level = SuggestedLevel.ADMIN)
    public static final String BASE_SUDO = "nucleus.sudo.base";

    @PermissionMetadata(descriptionKey = "permission.sudo.exempt", level = SuggestedLevel.ADMIN)
    public static final String SUDO_EXEMPT = "nucleus.sudo.exempt.target";

    @PermissionMetadata(descriptionKey = "permission.base", replacements = { "tellplain" }, level = SuggestedLevel.ADMIN)
    public static final String BASE_TELLPLAIN = "nucleus.tellplain.base";

}
