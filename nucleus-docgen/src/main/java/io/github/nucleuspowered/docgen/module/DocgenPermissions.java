/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.docgen.module;

import io.github.nucleuspowered.nucleus.core.services.interfaces.annotation.PermissionMetadata;
import io.github.nucleuspowered.nucleus.core.services.interfaces.data.SuggestedLevel;

public final class DocgenPermissions {

    @PermissionMetadata(descriptionKey = "permission.base", replacements = { "docgen" }, level = SuggestedLevel.NONE)
    public static final String BASE_DOCGEN = "nucleus.nucleus.docgen.base";

}
