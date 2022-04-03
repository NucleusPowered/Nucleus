/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.note.listeners;

import com.google.inject.Inject;
import io.github.nucleuspowered.nucleus.modules.note.NotePermissions;
import io.github.nucleuspowered.nucleus.modules.note.config.NoteConfig;
import io.github.nucleuspowered.nucleus.modules.note.services.NoteHandler;
import io.github.nucleuspowered.nucleus.core.scaffold.listener.ListenerBase;
import io.github.nucleuspowered.nucleus.core.services.INucleusServiceCollection;
import io.github.nucleuspowered.nucleus.core.services.interfaces.IMessageProviderService;
import io.github.nucleuspowered.nucleus.core.services.interfaces.IPermissionService;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.filter.Getter;
import org.spongepowered.api.event.network.ServerSideConnectionEvent;
import org.spongepowered.plugin.PluginContainer;

public class NoteListener implements ListenerBase.Conditional {

    private final NoteHandler noteHandler;
    private final IPermissionService permissionService;
    private final IMessageProviderService messageService;
    private final PluginContainer pluginContainer;

    @Inject
    public NoteListener(final INucleusServiceCollection serviceCollection) {
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
    public void onPlayerLogin(final ServerSideConnectionEvent.Join event, @Getter("player") final ServerPlayer player) {
        this.noteHandler.getNotes(player.uniqueId()).thenAccept(notes -> {
            if (notes != null && !notes.isEmpty()) {
                final Audience audience = this.permissionService.permissionMessageChannel(NotePermissions.NOTE_SHOWONLOGIN);
                Sponge.server().scheduler().executor(this.pluginContainer).execute(() ->
                        audience.sendMessage(this.messageService.getMessage("note.login.notify", player.name(), String.valueOf(notes.size()))
                            .hoverEvent(HoverEvent.showText(this.messageService.getMessage("note.login.view", player.name())))
                            .clickEvent(ClickEvent.runCommand("/nucleus:checknotes " + player.name()))));
            }
        });
    }

    @Override
    public boolean shouldEnable(final INucleusServiceCollection serviceCollection) {
        return serviceCollection.configProvider().getModuleConfig(NoteConfig.class).isShowOnLogin();
    }

}
