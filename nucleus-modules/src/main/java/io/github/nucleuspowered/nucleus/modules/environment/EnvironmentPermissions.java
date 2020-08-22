/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.environment;

import io.github.nucleuspowered.nucleus.annotationprocessor.RegisterPermissions;
import io.github.nucleuspowered.nucleus.services.interfaces.annotation.PermissionMetadata;
import io.github.nucleuspowered.nucleus.services.interfaces.data.SuggestedLevel;

@RegisterPermissions
public class EnvironmentPermissions {
    private EnvironmentPermissions() {
        throw new AssertionError("Nope");
    }

    @PermissionMetadata(descriptionKey = "permission.base", replacements = { "lockweather" }, level = SuggestedLevel.ADMIN)
    public static final String BASE_LOCKWEATHER = "nucleus.lockweather.base";

    @PermissionMetadata(descriptionKey = "permission.base", replacements = { "time" }, level = SuggestedLevel.USER)
    public static final String BASE_TIME = "nucleus.time.base";

    @PermissionMetadata(descriptionKey = "permission.exempt.cooldown", replacements = { "time" }, level = SuggestedLevel.ADMIN)
    public static final String EXEMPT_COOLDOWN_TIME = "nucleus.time.exempt.cooldown";

    @PermissionMetadata(descriptionKey = "permission.exempt.cost", replacements = { "time" }, level = SuggestedLevel.ADMIN)
    public static final String EXEMPT_COST_TIME = "nucleus.time.exempt.cost";

    @PermissionMetadata(descriptionKey = "permission.exempt.warmup", replacements = { "time" }, level = SuggestedLevel.ADMIN)
    public static final String EXEMPT_WARMUP_TIME = "nucleus.time.exempt.warmup";

    @PermissionMetadata(descriptionKey = "permission.base", replacements = { "time set" }, level = SuggestedLevel.ADMIN)
    public static final String BASE_TIME_SET = "nucleus.time.set.base";

    @PermissionMetadata(descriptionKey = "permission.exempt.cooldown", replacements = { "time set" }, level = SuggestedLevel.ADMIN)
    public static final String EXEMPT_COOLDOWN_TIME_SET = "nucleus.time.set.exempt.cooldown";

    @PermissionMetadata(descriptionKey = "permission.exempt.cost", replacements = { "time set" }, level = SuggestedLevel.ADMIN)
    public static final String EXEMPT_COST_TIME_SET = "nucleus.time.set.exempt.cost";

    @PermissionMetadata(descriptionKey = "permission.exempt.warmup", replacements = { "time set" }, level = SuggestedLevel.ADMIN)
    public static final String EXEMPT_WARMUP_TIME_SET = "nucleus.time.set.exempt.warmup";

    @PermissionMetadata(descriptionKey = "permission.base", replacements = { "weather" }, level = SuggestedLevel.ADMIN)
    public static final String BASE_WEATHER = "nucleus.weather.base";

    @PermissionMetadata(descriptionKey = "permission.exempt.cooldown", replacements = { "weather" }, level = SuggestedLevel.ADMIN)
    public static final String EXEMPT_COOLDOWN_WEATHER = "nucleus.weather.exempt.cooldown";

    @PermissionMetadata(descriptionKey = "permission.exempt.cost", replacements = { "weather" }, level = SuggestedLevel.ADMIN)
    public static final String EXEMPT_COST_WEATHER = "nucleus.weather.exempt.cost";

    @PermissionMetadata(descriptionKey = "permission.weather.exempt.length", level = SuggestedLevel.ADMIN)
    public static final String WEATHER_EXEMPT_LENGTH = "nucleus.weather.exempt.length";

    @PermissionMetadata(descriptionKey = "permission.exempt.warmup", replacements = { "weather" }, level = SuggestedLevel.ADMIN)
    public static final String EXEMPT_WARMUP_WEATHER = "nucleus.weather.exempt.warmup";

}
