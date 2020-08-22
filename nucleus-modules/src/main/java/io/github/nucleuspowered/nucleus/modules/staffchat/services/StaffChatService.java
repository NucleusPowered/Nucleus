/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.staffchat.services;

import io.github.nucleuspowered.nucleus.api.module.staffchat.NucleusStaffChatService;
import io.github.nucleuspowered.nucleus.modules.staffchat.StaffChatMessageChannel;
import io.github.nucleuspowered.nucleus.scaffold.service.ServiceBase;
import io.github.nucleuspowered.nucleus.scaffold.service.annotations.APIService;
import io.github.nucleuspowered.nucleus.services.INucleusServiceCollection;
import io.github.nucleuspowered.nucleus.services.impl.userprefs.NucleusKeysProvider;
import io.github.nucleuspowered.nucleus.services.interfaces.IChatMessageFormatterService;
import io.github.nucleuspowered.nucleus.services.interfaces.IUserPreferenceService;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.Text;

import javax.inject.Inject;

@APIService(NucleusStaffChatService.class)
public class StaffChatService implements NucleusStaffChatService, ServiceBase {

    private final IUserPreferenceService userPreferenceService;
    private final IChatMessageFormatterService chatMessageFormatService;

    @Inject
    public StaffChatService(INucleusServiceCollection serviceCollection) {
        this.userPreferenceService = serviceCollection.userPreferenceService();
        this.chatMessageFormatService = serviceCollection.chatMessageFormatter();
    }

    @Override
    public void sendMessageFrom(CommandSource source, Text message) {
        StaffChatMessageChannel.getInstance().sendMessageFrom(source, message);
    }

    public boolean isToggledChat(Player player) {
        return this.chatMessageFormatService.getNucleusChannel(player.getUniqueId()).filter(x -> x instanceof StaffChatMessageChannel).isPresent();
    }

    public void toggle(Player player, boolean toggle) {
        if (toggle) {
            this.chatMessageFormatService.setPlayerNucleusChannel(player.getUniqueId(), StaffChatMessageChannel.getInstance());

            // If you switch, you're switching to the staff chat channel so you should want to listen to it.
            this.userPreferenceService.setPreferenceFor(player, NucleusKeysProvider.VIEW_STAFF_CHAT, true);
        } else {
            this.chatMessageFormatService.setPlayerNucleusChannel(player.getUniqueId(), null);
        }
    }
}
