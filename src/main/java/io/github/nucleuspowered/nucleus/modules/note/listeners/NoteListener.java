/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.note.listeners;

import com.google.common.collect.Maps;
import com.google.inject.Inject;
import io.github.nucleuspowered.nucleus.api.data.NoteData;
import io.github.nucleuspowered.nucleus.dataservices.loaders.UserDataManager;
import io.github.nucleuspowered.nucleus.internal.PermissionRegistry;
import io.github.nucleuspowered.nucleus.internal.listeners.ListenerBase;
import io.github.nucleuspowered.nucleus.internal.permissions.PermissionInformation;
import io.github.nucleuspowered.nucleus.internal.permissions.SuggestedLevel;
import io.github.nucleuspowered.nucleus.modules.note.config.NoteConfigAdapter;
import io.github.nucleuspowered.nucleus.modules.note.handlers.NoteHandler;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.network.ClientConnectionEvent;
import org.spongepowered.api.text.action.TextActions;
import org.spongepowered.api.text.channel.MessageChannel;
import org.spongepowered.api.text.channel.MutableMessageChannel;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class NoteListener extends ListenerBase {

    @Inject private NoteHandler handler;
    @Inject private NoteConfigAdapter nca;
    @Inject private UserDataManager userDataManager;
    private final String showOnLogin = PermissionRegistry.PERMISSIONS_PREFIX + "note.showonlogin";


    /**
     * At the time the player joins, check to see if the player has any notes,
     * if he does send them to users with the permission plugin.note.showonlogin
     *
     * @param event The event.
     */
    @Listener
    public void onPlayerLogin(final ClientConnectionEvent.Join event) {
        if (!nca.getNodeOrDefault().isShowOnLogin()) {
            return;
        }

        Sponge.getScheduler().createTaskBuilder().async().delay(500, TimeUnit.MILLISECONDS).execute(() -> {
            Player player = event.getTargetEntity();
            List<NoteData> notes = handler.getNotes(player);
            if (notes != null && !notes.isEmpty()) {
                MutableMessageChannel messageChannel = MessageChannel.permission(showOnLogin).asMutable();
                messageChannel.send(plugin.getMessageProvider().getTextMessageWithFormat("note.login.notify", player.getName(), String.valueOf(notes.size())).toBuilder()
                        .onHover(TextActions.showText(plugin.getMessageProvider().getTextMessageWithFormat("note.login.view", player.getName())))
                        .onClick(TextActions.runCommand("/checknotes " + player.getName()))
                        .build());

            }
        }).submit(plugin);
    }

    @Override
    public Map<String, PermissionInformation> getPermissions() {
        Map<String, PermissionInformation> mp = Maps.newHashMap();
        mp.put(showOnLogin, new PermissionInformation(plugin.getMessageProvider().getMessageWithFormat("permission.note.showonlogin"), SuggestedLevel.MOD));
        return mp;
    }
}
