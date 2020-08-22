/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.notification;

import io.github.nucleuspowered.nucleus.annotationprocessor.RegisterPermissions;
import io.github.nucleuspowered.nucleus.services.interfaces.annotation.PermissionMetadata;
import io.github.nucleuspowered.nucleus.services.interfaces.data.SuggestedLevel;

@RegisterPermissions
public class NotificationPermissions {

    private NotificationPermissions() {}

    @PermissionMetadata(descriptionKey = "permission.base", replacements = { "broadcast" }, level = SuggestedLevel.ADMIN)
    public static final String BASE_BROADCAST = "nucleus.broadcast.base";

    @PermissionMetadata(descriptionKey = "permission.base", replacements = { "basictitle" }, level = SuggestedLevel.ADMIN)
    public static final String BASE_BASICTITLE = "nucleus.basictitle.base";

    @PermissionMetadata(descriptionKey = "permission.multitarget", replacements = { "basictitle" }, level = SuggestedLevel.ADMIN)
    public static final String BASICTITLE_MULTI = "nucleus.basictitle.multiple";

    @PermissionMetadata(descriptionKey = "permission.base", replacements = { "basicsubtitle" }, level = SuggestedLevel.ADMIN)
    public static final String BASE_BASICSUBTITLE = "nucleus.basicsubtitle.base";

    @PermissionMetadata(descriptionKey = "permission.multitarget", replacements = { "basicsubtitle" }, level = SuggestedLevel.ADMIN)
    public static final String BASICSUBTITLE_MULTI = "nucleus.basicsubtitle.multiple";

    @PermissionMetadata(descriptionKey = "permission.base", replacements = { "basicactionbar" }, level = SuggestedLevel.ADMIN)
    public static final String BASE_BASICACTIONBAR = "nucleus.basicactionbar.base";

    @PermissionMetadata(descriptionKey = "permission.multitarget", replacements = { "basicactionbar" }, level = SuggestedLevel.ADMIN)
    public static final String BASICACTIONBAR_MULTI = "nucleus.basicactionbar.multiple";

    @PermissionMetadata(descriptionKey = "permission.base", replacements = { "plainbroadcast" }, level = SuggestedLevel.OWNER)
    public static final String BASE_PLAINBROADCAST = "nucleus.plainbroadcast.base";
}
