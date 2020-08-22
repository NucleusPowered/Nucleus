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
        aliases = "basicsubtitle",
        basePermission = NotificationPermissions.BASE_BASICSUBTITLE,
        commandDescriptionKey = "basicsubtitle",
        associatedPermissions = NotificationPermissions.BASICSUBTITLE_MULTI
)
public class BasicSubtitleCommand extends TitleBase {

    @Inject
    public BasicSubtitleCommand() {
        super(NotificationPermissions.BASICSUBTITLE_MULTI, "Subtitle");
    }

    @Override
    protected Title.Builder applyToBuilder(Title.Builder builder, Text text) {
        return builder.subtitle(text);
    }

}
