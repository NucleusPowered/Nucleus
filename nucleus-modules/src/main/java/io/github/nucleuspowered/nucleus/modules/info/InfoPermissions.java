/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.info;

import io.github.nucleuspowered.nucleus.annotationprocessor.RegisterPermissions;
import io.github.nucleuspowered.nucleus.services.interfaces.annotation.PermissionMetadata;
import io.github.nucleuspowered.nucleus.services.interfaces.data.SuggestedLevel;

@RegisterPermissions
public class InfoPermissions {
    private InfoPermissions() {
        throw new AssertionError("Nope");
    }

    @PermissionMetadata(descriptionKey = "permission.base", replacements = { "info" }, level = SuggestedLevel.USER)
    public static final String BASE_INFO = "nucleus.info.base";

    @PermissionMetadata(descriptionKey = "permission.info.list", level = SuggestedLevel.ADMIN)
    public static final String INFO_LIST = "nucleus.info.list";

    @PermissionMetadata(descriptionKey = "permission.base", replacements = { "motd" }, level = SuggestedLevel.USER)
    public static final String BASE_MOTD = "nucleus.motd.base";

    @PermissionMetadata(descriptionKey = "permission.motd.join", level = SuggestedLevel.USER)
    public static final String MOTD_JOIN = "nucleus.motd.login";

}
