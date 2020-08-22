/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.world;

import io.github.nucleuspowered.nucleus.annotationprocessor.RegisterPermissions;
import io.github.nucleuspowered.nucleus.services.interfaces.annotation.PermissionMetadata;
import io.github.nucleuspowered.nucleus.services.interfaces.data.SuggestedLevel;

@RegisterPermissions
public class WorldPermissions {
    private WorldPermissions() {
        throw new AssertionError("Nope");
    }

    @PermissionMetadata(descriptionKey = "permission.base", replacements = { "world" }, level = SuggestedLevel.ADMIN)
    public static final String BASE_WORLD = "nucleus.world.base";

    @PermissionMetadata(descriptionKey = "permission.base", replacements = { "world border" }, level = SuggestedLevel.ADMIN)
    public static final String BASE_WORLD_BORDER = "nucleus.world.border.base";

    @PermissionMetadata(descriptionKey = "permission.base", replacements = { "border gen" }, level = SuggestedLevel.ADMIN)
    public static final String BASE_BORDER_GEN = "nucleus.world.border.gen.base";

    @PermissionMetadata(descriptionKey = "permission.world.border.gen.notify", level = SuggestedLevel.ADMIN)
    public static final String WORLD_BORDER_GEN_NOTIFY = "nucleus.world.border.gen.notify";

    @PermissionMetadata(descriptionKey = "permission.base", replacements = { "border set" }, level = SuggestedLevel.ADMIN)
    public static final String BASE_BORDER_SET = "nucleus.world.border.set.base";

    @PermissionMetadata(descriptionKey = "permission.base", replacements = { "world clone" }, level = SuggestedLevel.OWNER)
    public static final String BASE_WORLD_CLONE = "nucleus.world.clone.base";

    @PermissionMetadata(descriptionKey = "permission.base", replacements = { "world create" }, level = SuggestedLevel.ADMIN)
    public static final String BASE_WORLD_CREATE = "nucleus.world.create.base";

    @PermissionMetadata(descriptionKey = "permission.base", replacements = { "world delete" }, level = SuggestedLevel.OWNER)
    public static final String BASE_WORLD_DELETE = "nucleus.world.delete.base";

    @PermissionMetadata(descriptionKey = "permission.base", replacements = { "world disable" }, level = SuggestedLevel.ADMIN)
    public static final String BASE_WORLD_DISABLE = "nucleus.world.disable.base";

    @PermissionMetadata(descriptionKey = "permission.base", replacements = { "world enable" }, level = SuggestedLevel.ADMIN)
    public static final String BASE_WORLD_ENABLE = "nucleus.world.enable.base";

    @PermissionMetadata(descriptionKey = "permission.world.force-gamemode.override", level = SuggestedLevel.ADMIN)
    public static final String WORLD_FORCE_GAMEMODE_OVERRIDE = "nucleus.world.force-gamemode.override";

    @PermissionMetadata(descriptionKey = "permission.base", replacements = { "world gamerule" }, level = SuggestedLevel.ADMIN)
    public static final String BASE_WORLD_GAMERULE = "nucleus.world.gamerule.base";

    @PermissionMetadata(descriptionKey = "permission.base", replacements = { "gamerule set" }, level = SuggestedLevel.ADMIN)
    public static final String BASE_WORLD_GAMERULE_SET = "nucleus.world.gamerule.set.base";

    @PermissionMetadata(descriptionKey = "permission.base", replacements = { "world info" }, level = SuggestedLevel.ADMIN)
    public static final String BASE_WORLD_INFO = "nucleus.world.list.base";

    @PermissionMetadata(descriptionKey = "permission.world.seed", level = SuggestedLevel.ADMIN)
    public static final String WORLD_SEED = "nucleus.world.list.seed";

    @PermissionMetadata(descriptionKey = "permission.base", replacements = { "world load" }, level = SuggestedLevel.ADMIN)
    public static final String BASE_WORLD_LOAD = "nucleus.world.load.base";

    @PermissionMetadata(descriptionKey = "permission.base", replacements = { "world rename" }, level = SuggestedLevel.OWNER)
    public static final String BASE_WORLD_RENAME = "nucleus.world.rename.base";

    @PermissionMetadata(descriptionKey = "permission.base", replacements = { "world setdifficulty" }, level = SuggestedLevel.ADMIN)
    public static final String BASE_WORLD_SETDIFFICULTY = "nucleus.world.setdifficulty.base";

    @PermissionMetadata(descriptionKey = "permission.base", replacements = { "world setgamemode" }, level = SuggestedLevel.ADMIN)
    public static final String BASE_WORLD_SETGAMEMODE = "nucleus.world.setgamemode.base";

    @PermissionMetadata(descriptionKey = "permission.base", replacements = { "world sethardcore" }, level = SuggestedLevel.ADMIN)
    public static final String BASE_WORLD_SETHARDCORE = "nucleus.world.sethardcore.base";

    @PermissionMetadata(descriptionKey = "permission.base", replacements = { "world setkeepspawnloaded" }, level = SuggestedLevel.ADMIN)
    public static final String BASE_WORLD_SETKEEPSPAWNLOADED = "nucleus.world.setkeepspawnloaded.base";

    @PermissionMetadata(descriptionKey = "permission.base", replacements = { "world setloadonstartup" }, level = SuggestedLevel.ADMIN)
    public static final String BASE_WORLD_SETLOADONSTARTUP = "nucleus.world.setloadonstartup.base";

    @PermissionMetadata(descriptionKey = "permission.base", replacements = { "world setpvpenabled" }, level = SuggestedLevel.ADMIN)
    public static final String BASE_WORLD_SETPVPENABLED = "nucleus.world.setpvpenabled.base";

    @PermissionMetadata(descriptionKey = "permission.base", replacements = { "world setspawn" }, level = SuggestedLevel.ADMIN)
    public static final String BASE_WORLD_SETSPAWN = "nucleus.world.setspawn.base";

    @PermissionMetadata(descriptionKey = "permission.base", replacements = { "world spawn" }, level = SuggestedLevel.ADMIN)
    public static final String BASE_WORLD_SPAWN = "nucleus.world.spawn.base";

    @PermissionMetadata(descriptionKey = "permission.exempt.cooldown", replacements = { "world spawn" }, level = SuggestedLevel.ADMIN)
    public static final String EXEMPT_COOLDOWN_WORLD_SPAWN = "nucleus.world.spawn.exempt.cooldown";

    @PermissionMetadata(descriptionKey = "permission.exempt.cost", replacements = { "world spawn" }, level = SuggestedLevel.ADMIN)
    public static final String EXEMPT_COST_WORLD_SPAWN = "nucleus.world.spawn.exempt.cost";

    @PermissionMetadata(descriptionKey = "permission.exempt.warmup", replacements = { "world spawn" }, level = SuggestedLevel.ADMIN)
    public static final String EXEMPT_WARMUP_WORLD_SPAWN = "nucleus.world.spawn.exempt.warmup";

    @PermissionMetadata(descriptionKey = "permission.base", replacements = { "world teleport" }, level = SuggestedLevel.ADMIN)
    public static final String BASE_WORLD_TELEPORT = "nucleus.world.teleport.base";

    @PermissionMetadata(descriptionKey = "permission.exempt.cooldown", replacements = { "world teleport" }, level = SuggestedLevel.ADMIN)
    public static final String EXEMPT_COOLDOWN_WORLD_TELEPORT = "nucleus.world.teleport.exempt.cooldown";

    @PermissionMetadata(descriptionKey = "permission.exempt.cost", replacements = { "world teleport" }, level = SuggestedLevel.ADMIN)
    public static final String EXEMPT_COST_WORLD_TELEPORT = "nucleus.world.teleport.exempt.cost";

    @PermissionMetadata(descriptionKey = "permission.exempt.warmup", replacements = { "world teleport" }, level = SuggestedLevel.ADMIN)
    public static final String EXEMPT_WARMUP_WORLD_TELEPORT = "nucleus.world.teleport.exempt.warmup";

    @PermissionMetadata(descriptionKey = "permission.world.teleport.other", level = SuggestedLevel.ADMIN)
    public static final String WORLD_TELEPORT_OTHER = "nucleus.world.teleport.others";

    @PermissionMetadata(descriptionKey = "permission.base", replacements = { "world unload" }, level = SuggestedLevel.ADMIN)
    public static final String BASE_WORLD_UNLOAD = "nucleus.world.unload.base";

    @PermissionMetadata(descriptionKey = "permission.worlds.access", level = SuggestedLevel.ADMIN, isPrefix = true)
    public static final String WORLDS_ACCESS_PERMISSION_PREFIX = "nucleus.worlds";

    public static String getWorldAccessPermission(String world) {
        return WORLDS_ACCESS_PERMISSION_PREFIX + "." + world.toLowerCase();
    }

}
