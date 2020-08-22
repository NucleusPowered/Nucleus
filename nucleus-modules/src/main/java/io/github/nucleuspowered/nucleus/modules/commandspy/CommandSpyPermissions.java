/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.commandspy;

import io.github.nucleuspowered.nucleus.annotationprocessor.RegisterPermissions;
import io.github.nucleuspowered.nucleus.services.interfaces.annotation.PermissionMetadata;
import io.github.nucleuspowered.nucleus.services.interfaces.data.SuggestedLevel;

@RegisterPermissions
public class CommandSpyPermissions {

    private CommandSpyPermissions() {
        throw new AssertionError("Nope");
    }

    @PermissionMetadata(descriptionKey = "permission.base", replacements = { "commandspy" }, level = SuggestedLevel.ADMIN)
    public static final String BASE_COMMANDSPY = "nucleus.commandspy.base";

    @PermissionMetadata(descriptionKey = "permission.commandspy.exempt.target", level = SuggestedLevel.ADMIN)
    public static final String COMMANDSPY_EXEMPT_TARGET = "nucleus.commandspy.exempt.target";

}
