/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.notification.command;

import com.google.inject.Inject;
import io.github.nucleuspowered.nucleus.modules.notification.NotificationPermissions;
import io.github.nucleuspowered.nucleus.scaffold.command.annotation.Command;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.title.Title;

@Command(
        aliases = "basictitle",
        basePermission = NotificationPermissions.BASE_BASICTITLE,
        commandDescriptionKey = "basictitle",
        associatedPermissions = NotificationPermissions.BASICTITLE_MULTI
)
public class BasicTitleCommand extends TitleBase {

    @Inject
    public BasicTitleCommand() {
        super(NotificationPermissions.BASICTITLE_MULTI, "Title");
    }

    @Override
    protected Title.Builder applyToBuilder(Title.Builder builder, Text text) {
        return builder.title(text);
    }

}
