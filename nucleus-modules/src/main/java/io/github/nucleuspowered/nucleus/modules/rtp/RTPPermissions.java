/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.rtp;

import io.github.nucleuspowered.nucleus.annotationprocessor.RegisterPermissions;
import io.github.nucleuspowered.nucleus.services.interfaces.annotation.PermissionMetadata;
import io.github.nucleuspowered.nucleus.services.interfaces.data.SuggestedLevel;

@RegisterPermissions
public class RTPPermissions {
    private RTPPermissions() {
        throw new AssertionError("Nope");
    }

    @PermissionMetadata(descriptionKey = "permission.base", replacements = { "rtp" }, level = SuggestedLevel.ADMIN)
    public static final String BASE_RTP = "nucleus.rtp.base";

    @PermissionMetadata(descriptionKey = "permission.exempt.cooldown", replacements = { "rtp" }, level = SuggestedLevel.ADMIN)
    public static final String EXEMPT_COOLDOWN_RTP = "nucleus.rtp.exempt.cooldown";

    @PermissionMetadata(descriptionKey = "permission.exempt.cost", replacements = { "rtp" }, level = SuggestedLevel.ADMIN)
    public static final String EXEMPT_COST_RTP = "nucleus.rtp.exempt.cost";

    @PermissionMetadata(descriptionKey = "permission.exempt.warmup", replacements = { "rtp" }, level = SuggestedLevel.ADMIN)
    public static final String EXEMPT_WARMUP_RTP = "nucleus.rtp.exempt.warmup";

    @PermissionMetadata(descriptionKey = "permission.others", replacements = { "rtp" }, level = SuggestedLevel.ADMIN)
    public static final String OTHERS_RTP = "nucleus.rtp.others";

    @PermissionMetadata(descriptionKey = "permission.rtp.worlds", level = SuggestedLevel.ADMIN)
    public static final String RTP_WORLDS = "nucleus.rtp.worlds";

}
