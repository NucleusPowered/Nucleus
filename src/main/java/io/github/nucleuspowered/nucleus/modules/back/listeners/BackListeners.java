/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.back.listeners;

import com.google.common.collect.Maps;
import com.google.inject.Inject;
import io.github.nucleuspowered.nucleus.api.service.NucleusJailService;
import io.github.nucleuspowered.nucleus.internal.CommandPermissionHandler;
import io.github.nucleuspowered.nucleus.internal.listeners.ListenerBase;
import io.github.nucleuspowered.nucleus.internal.permissions.PermissionInformation;
import io.github.nucleuspowered.nucleus.internal.permissions.SuggestedLevel;
import io.github.nucleuspowered.nucleus.modules.back.commands.BackCommand;
import io.github.nucleuspowered.nucleus.modules.back.config.BackConfigAdapter;
import io.github.nucleuspowered.nucleus.modules.back.handlers.BackHandler;
import org.spongepowered.api.entity.living.Living;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.entity.DestructEntityEvent;
import org.spongepowered.api.event.entity.DisplaceEntityEvent;
import org.spongepowered.api.event.filter.Getter;
import org.spongepowered.api.event.filter.type.Exclude;

import java.util.Map;

public class BackListeners extends ListenerBase {

    static final String onTeleport = "targets.teleport";
    static final String onDeath = "targets.death";
    static final String onPortal = "targets.portal";

    @Inject private BackHandler handler;
    @Inject private BackConfigAdapter bca;
    @Inject(optional = true) private NucleusJailService njs;

    private CommandPermissionHandler s = null;

    private CommandPermissionHandler getPermissionUtil() {
        if (s == null) {
            s = plugin.getPermissionRegistry().getService(BackCommand.class).orElseGet(() -> new CommandPermissionHandler(new BackCommand(), plugin));
        }

        return s;
    }

    @Override
    public Map<String, PermissionInformation> getPermissions() {
        Map<String, PermissionInformation> m = Maps.newHashMap();
        m.put(getPermissionUtil().getPermissionWithSuffix(onDeath), new PermissionInformation(plugin.getMessageProvider().getMessageWithFormat("permission.back.ondeath"), SuggestedLevel.USER));
        m.put(getPermissionUtil().getPermissionWithSuffix(onTeleport), new PermissionInformation(plugin.getMessageProvider().getMessageWithFormat("permission.back.onteleport"), SuggestedLevel.USER));
        m.put(getPermissionUtil().getPermissionWithSuffix(onPortal), new PermissionInformation(plugin.getMessageProvider().getMessageWithFormat("permission.back.onportal"), SuggestedLevel.USER));
        return m;
    }

    @Listener
    @Exclude(DisplaceEntityEvent.Teleport.Portal.class) // Don't set /back on a portal.
    public void onTeleportPlayer(DisplaceEntityEvent.Teleport event, @Getter("getTargetEntity") Player pl) {
        if (bca.getNodeOrDefault().isOnTeleport() && getLogBack(pl) && getPermissionUtil().testSuffix(pl, onTeleport)) {
            handler.setLastLocation(pl, event.getFromTransform());
        }
    }

    @Listener
    public void onPortalPlayer(DisplaceEntityEvent.Teleport.Portal event, @Getter("getTargetEntity") Player pl) {
        if (bca.getNodeOrDefault().isOnPortal() && getLogBack(pl) && getPermissionUtil().testSuffix(pl, onPortal)) {
            handler.setLastLocation(pl, event.getFromTransform());
        }
    }

    @Listener
    public void onDeathEvent(DestructEntityEvent.Death event) {
        Living e = event.getTargetEntity();
        if (!(e instanceof Player)) {
            return;
        }

        Player pl = (Player)e;
        if (bca.getNodeOrDefault().isOnDeath() && getLogBack(pl) && getPermissionUtil().testSuffix(pl, onDeath)) {
            handler.setLastLocation(pl, event.getTargetEntity().getTransform());
        }
    }

    private boolean getLogBack(Player player) {
        return !(njs != null && njs.isPlayerJailed(player)) && handler.getLogBack(player);
    }
}
