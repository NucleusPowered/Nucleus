/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.commandspy.listeners;

import com.google.common.collect.ImmutableSet;
import io.github.nucleuspowered.nucleus.Util;
import io.github.nucleuspowered.nucleus.modules.commandspy.CommandSpyPermissions;
import io.github.nucleuspowered.nucleus.modules.commandspy.config.CommandSpyConfig;
import io.github.nucleuspowered.nucleus.scaffold.listener.ListenerBase;
import io.github.nucleuspowered.nucleus.services.INucleusServiceCollection;
import io.github.nucleuspowered.nucleus.services.impl.userprefs.NucleusKeysProvider;
import io.github.nucleuspowered.nucleus.services.interfaces.IPermissionService;
import io.github.nucleuspowered.nucleus.services.interfaces.IReloadableService;
import io.github.nucleuspowered.nucleus.services.interfaces.ITextStyleService;
import io.github.nucleuspowered.nucleus.services.interfaces.IUserPreferenceService;
import io.github.nucleuspowered.nucleus.util.CommandNameCache;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.command.SendCommandEvent;
import org.spongepowered.api.event.filter.cause.Root;
import org.spongepowered.api.text.Text;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.inject.Inject;

public class CommandSpyListener implements IReloadableService.Reloadable, ListenerBase.Conditional {

    private final IPermissionService permissionService;
    private final IUserPreferenceService userPreferenceService;
    private final ITextStyleService textStyleService;
    private CommandSpyConfig config = new CommandSpyConfig();
    private Set<String> toSpy = ImmutableSet.of();
    private boolean listIsEmpty = true;

    @Inject
    public CommandSpyListener(INucleusServiceCollection serviceCollection) {
        this.permissionService = serviceCollection.permissionService();
        this.userPreferenceService = serviceCollection.userPreferenceService();
        this.textStyleService = serviceCollection.textStyleService();
    }

    @Listener(order = Order.LAST)
    public void onCommand(SendCommandEvent event, @Root Player player) {

        if (!this.permissionService.hasPermission(player, CommandSpyPermissions.COMMANDSPY_EXEMPT_TARGET)) {
            boolean isInList = false;
            if (!this.listIsEmpty) {
                String command = event.getCommand().toLowerCase();
                Set<String> cmd = CommandNameCache.INSTANCE.getFromCommandAndSource(command, player);
                cmd.retainAll(this.toSpy);
                isInList = !cmd.isEmpty();
            }

            // If the command is in the list, report it.
            if (isInList == this.config.isUseWhitelist()) {
                UUID currentUUID = player.getUniqueId();
                List<Player> playerList = Sponge.getServer().getOnlinePlayers()
                    .stream()
                    .filter(x -> !x.getUniqueId().equals(currentUUID))
                    .filter(x -> this.permissionService.hasPermission(x, CommandSpyPermissions.BASE_COMMANDSPY))
                    .filter(x -> this.userPreferenceService.getUnwrapped(x.getUniqueId(), NucleusKeysProvider.COMMAND_SPY))
                    .collect(Collectors.toList());

                if (!playerList.isEmpty()) {
                    Text prefix = this.config.getTemplate().getForCommandSource(player);
                    ITextStyleService.TextFormat st = this.textStyleService.getLastColourAndStyle(prefix, null);
                    Text messageToSend = prefix
                            .toBuilder()
                            .append(Text.of(st.colour(), st.style(), "/", event.getCommand(), Util.SPACE, event.getArguments())).build();
                    playerList.forEach(x -> x.sendMessage(messageToSend));
                }
            }
        }
    }

    @Override
    public void onReload(INucleusServiceCollection serviceCollection) {
        this.config = serviceCollection.moduleDataProvider().getModuleConfig(CommandSpyConfig.class);
        this.listIsEmpty = this.config.getCommands().isEmpty();
        this.toSpy = this.config.getCommands().stream().map(String::toLowerCase).collect(ImmutableSet.toImmutableSet());
    }

    @Override
    public boolean shouldEnable(INucleusServiceCollection serviceCollection) {
        return !this.config.isUseWhitelist() || !this.listIsEmpty;
    }
}
