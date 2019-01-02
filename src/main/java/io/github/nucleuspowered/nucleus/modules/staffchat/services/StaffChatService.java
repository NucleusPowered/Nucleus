/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.staffchat.services;

import io.github.nucleuspowered.nucleus.Nucleus;
import io.github.nucleuspowered.nucleus.api.chat.NucleusChatChannel;
import io.github.nucleuspowered.nucleus.api.service.NucleusStaffChatService;
import io.github.nucleuspowered.nucleus.internal.annotations.APIService;
import io.github.nucleuspowered.nucleus.internal.interfaces.ServiceBase;
import io.github.nucleuspowered.nucleus.internal.userprefs.UserPreferenceService;
import io.github.nucleuspowered.nucleus.modules.staffchat.StaffChatMessageChannel;
import io.github.nucleuspowered.nucleus.modules.staffchat.StaffChatUserPrefKeys;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.channel.MessageChannel;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@APIService(NucleusStaffChatService.class)
public class StaffChatService implements NucleusStaffChatService, ServiceBase {

    private final Map<UUID, MessageChannel> previousChannels = new HashMap<>();

    @Override
    public NucleusChatChannel.StaffChat getStaffChat() {
        return StaffChatMessageChannel.getInstance();
    }

    public void reset(UUID uuid) {
        this.previousChannels.remove(uuid);
    }

    public boolean isToggledChat(Player player) {
        return player.getMessageChannel() == StaffChatMessageChannel.getInstance();
    }

    public void toggle(Player player, boolean toggle) {
        if (toggle) {
            MessageChannel current = player.getMessageChannel();
            if (current != StaffChatMessageChannel.getInstance()) {
                this.previousChannels.put(player.getUniqueId(), player.getMessageChannel());
            }
            player.setMessageChannel(StaffChatMessageChannel.getInstance());

            // If you switch, you're switching to the staff chat channel so you should want to listen to it.
            Nucleus.getNucleus().getInternalServiceManager().getServiceUnchecked(UserPreferenceService.class)
                    .setPreferenceFor(player, StaffChatUserPrefKeys.VIEW_STAFF_CHAT, true);
        } else {
            @Nullable MessageChannel mc = this.previousChannels.get(player.getUniqueId());
            if (mc == null) {
                player.setMessageChannel(MessageChannel.TO_ALL);
            } else {
                player.setMessageChannel(mc);
                this.previousChannels.remove(player.getUniqueId());
            }
        }
    }
}
