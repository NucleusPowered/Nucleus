/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.nickname;

import io.github.nucleuspowered.nucleus.annotationprocessor.RegisterPermissions;
import io.github.nucleuspowered.nucleus.services.interfaces.annotation.PermissionMetadata;
import io.github.nucleuspowered.nucleus.services.interfaces.data.SuggestedLevel;

@RegisterPermissions
public class NicknamePermissions {
    private NicknamePermissions() {
        throw new AssertionError("Nope");
    }

    @PermissionMetadata(descriptionKey = "permission.base", replacements = { "nick, delnick" }, level = SuggestedLevel.ADMIN)
    public static final String BASE_NICK = "nucleus.nick.base";

    @PermissionMetadata(descriptionKey = "permission.others", replacements = { "nick, delnick" }, level = SuggestedLevel.ADMIN)
    public static final String OTHERS_NICK = "nucleus.nick.others";

    @PermissionMetadata(descriptionKey = "permission.exempt.cooldown", replacements = { "nick" }, level = SuggestedLevel.ADMIN)
    public static final String EXEMPT_COOLDOWN_NICK = "nucleus.nick.exempt.cooldown";

    @PermissionMetadata(descriptionKey = "permission.exempt.cost", replacements = { "nick" }, level = SuggestedLevel.ADMIN)
    public static final String EXEMPT_COST_NICK = "nucleus.nick.exempt.cost";

    @PermissionMetadata(descriptionKey = "permission.exempt.warmup", replacements = { "nick" }, level = SuggestedLevel.ADMIN)
    public static final String EXEMPT_WARMUP_NICK = "nucleus.nick.exempt.warmup";

    @PermissionMetadata(descriptionKey = "permission.base", replacements = { "realname" }, level = SuggestedLevel.USER)
    public static final String BASE_REALNAME = "nucleus.realname.base";

    @PermissionMetadata(descriptionKey = "permission.nick.colour", level = SuggestedLevel.ADMIN)
    public static final String NICKNAME_COLOUR = "nucleus.nick.colour";

    @PermissionMetadata(descriptionKey = "permission.nick.style", level = SuggestedLevel.ADMIN)
    public static final String NICKNAME_STYLE = "nucleus.nick.style";

}
