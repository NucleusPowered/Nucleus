/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.note.listeners;

import io.github.nucleuspowered.nucleus.modules.note.NotePermissions;
import io.github.nucleuspowered.nucleus.modules.note.config.NoteConfig;
import io.github.nucleuspowered.nucleus.modules.note.data.NoteData;
import io.github.nucleuspowered.nucleus.modules.note.services.NoteHandler;
import io.github.nucleuspowered.nucleus.scaffold.listener.ListenerBase;
import io.github.nucleuspowered.nucleus.services.INucleusServiceCollection;
import io.github.nucleuspowered.nucleus.services.interfaces.IMessageProviderService;
import io.github.nucleuspowered.nucleus.services.interfaces.IPermissionService;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.filter.Getter;
import org.spongepowered.api.event.network.ClientConnectionEvent;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.text.action.TextActions;
import org.spongepowered.api.text.channel.MutableMessageChannel;

import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

public class NoteListener implements ListenerBase.Conditional {

    private final NoteHandler noteHandler;
    private final IPermissionService permissionService;
    private final IMessageProviderService messageService;
    private final PluginContainer pluginContainer;

    @Inject
    public NoteListener(INucleusServiceCollection serviceCollection) {
        this.noteHandler = serviceCollection.getServiceUnchecked(NoteHandler.class);
        this.permissionService = serviceCollection.permissionService();
        this.messageService = serviceCollection.messageProvider();
        this.pluginContainer = serviceCollection.pluginContainer();
    }

    /**
     * At the time the subject joins, check to see if the subject has any notes,
     * if he does send them to users with the permission plugin.note.showonlogin
     *
     * @param event The event.
     * @param player The {@link Player} that has just logged in.
     */
    @Listener
    public void onPlayerLogin(final ClientConnectionEvent.Join event, @Getter("getTargetEntity") final Player player) {
        Sponge.getScheduler().createTaskBuilder().async().delay(500, TimeUnit.MILLISECONDS).execute(() -> {
            List<NoteData> notes = this.noteHandler.getNotesInternal(player);
            if (notes != null && !notes.isEmpty()) {
                MutableMessageChannel messageChannel =
                        this.permissionService.permissionMessageChannel(NotePermissions.NOTE_SHOWONLOGIN).asMutable();
                messageChannel.send(
                        this.messageService.getMessage("note.login.notify", player.getName(), String.valueOf(notes.size())).toBuilder()
                        .onHover(TextActions.showText(this.messageService.getMessage("note.login.view", player.getName())))
                        .onClick(TextActions.runCommand("/checknotes " + player.getName()))
                        .build());

            }
        }).submit(this.pluginContainer);
    }

    @Override public boolean shouldEnable(INucleusServiceCollection serviceCollection) {
        return serviceCollection.moduleDataProvider().getModuleConfig(NoteConfig.class).isShowOnLogin();
    }

}
