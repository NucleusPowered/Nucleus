/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.message.infoprovider;

import com.google.common.collect.Lists;
import io.github.nucleuspowered.nucleus.modules.message.MessagePermissions;
import io.github.nucleuspowered.nucleus.modules.message.services.MessageHandler;
import io.github.nucleuspowered.nucleus.services.INucleusServiceCollection;
import io.github.nucleuspowered.nucleus.services.impl.playerinformation.NucleusProvider;
import io.github.nucleuspowered.nucleus.services.impl.userprefs.NucleusKeysProvider;
import io.github.nucleuspowered.nucleus.services.interfaces.IMessageProviderService;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.text.Text;

import java.util.List;
import java.util.Optional;

public class MessageInfoProvider implements NucleusProvider {

    @Override
    public String getCategory() {
        return "message";
    }

    @Override public Optional<Text> get(User user, CommandSource source,
            INucleusServiceCollection serviceCollection) {
        if (serviceCollection.permissionService().hasPermission(source, MessagePermissions.BASE_SOCIALSPY)) {
            MessageHandler handler = serviceCollection.getServiceUnchecked(MessageHandler.class);
            boolean socialSpy = handler.isSocialSpy(user);
            boolean msgToggle = serviceCollection.userPreferenceService()
                    .getUnwrapped(user.getUniqueId(), NucleusKeysProvider.RECEIVING_MESSAGES);
            IMessageProviderService mp = serviceCollection.messageProvider();
            List<Text> lt = Lists.newArrayList(
                    mp.getMessageFor(source, "seen.socialspy",
                            mp.getMessageFor(source, "standard.yesno." + Boolean.toString(socialSpy).toLowerCase())));

            /*this.serviceCollection.moduleConfigProvider()
                    .getModuleConfig(MessageConfig.class)*/
            lt.add(
                    mp.getMessageFor(source,
                            "seen.socialspylevel",
                            serviceCollection.permissionService()
                                    .getPositiveIntOptionFromSubject(user, MessagePermissions.SOCIALSPY_LEVEL_KEY).orElse(0))
            );

            lt.add(mp.getMessageFor(source, "seen.msgtoggle",
                    mp.getMessageFor(source, "standard.yesno." + Boolean.toString(msgToggle).toLowerCase())));

            return Optional.of(Text.joinWith(Text.NEW_LINE, lt));
        }

        return Optional.empty();
    }
}
