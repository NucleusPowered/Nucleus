/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.serverlist;

import io.github.nucleuspowered.nucleus.annotationprocessor.RegisterPermissions;
import io.github.nucleuspowered.nucleus.services.interfaces.annotation.PermissionMetadata;
import io.github.nucleuspowered.nucleus.services.interfaces.data.SuggestedLevel;

@RegisterPermissions
public class ServerListPermissions {

    private ServerListPermissions() {
        throw new AssertionError("Nope");
    }

    @PermissionMetadata(descriptionKey = "permission.base", replacements = { "serverlist" }, level = SuggestedLevel.ADMIN)
    public static final String BASE_SERVERLIST = "nucleus.serverlist.base";

    @PermissionMetadata(descriptionKey = "permission.base", replacements = { "serverlist message" }, level = SuggestedLevel.ADMIN)
    public static final String BASE_SERVERLIST_MESSAGE = "nucleus.serverlist.message.base";

}
