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
        aliases = "basicactionbar",
        basePermission = NotificationPermissions.BASE_BASICACTIONBAR,
        commandDescriptionKey = "basicactionbar"
)
public class BasicActionbarCommand extends TitleBase {

    @Inject
    public BasicActionbarCommand() {
        super(NotificationPermissions.BASICACTIONBAR_MULTI, "Action Bar");
    }

    @Override
    protected Title.Builder applyToBuilder(Title.Builder builder, Text text) {
        return builder.actionBar(text);
    }

}
