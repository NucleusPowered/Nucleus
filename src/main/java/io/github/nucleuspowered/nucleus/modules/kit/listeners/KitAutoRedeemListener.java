/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.kit.listeners;

import io.github.nucleuspowered.nucleus.Nucleus;
import io.github.nucleuspowered.nucleus.api.exceptions.KitRedeemException;
import io.github.nucleuspowered.nucleus.api.nucleusdata.Kit;
import io.github.nucleuspowered.nucleus.dataservices.KitDataService;
import io.github.nucleuspowered.nucleus.internal.PermissionRegistry;
import io.github.nucleuspowered.nucleus.internal.interfaces.ListenerBase;
import io.github.nucleuspowered.nucleus.internal.interfaces.Reloadable;
import io.github.nucleuspowered.nucleus.internal.traits.InternalServiceManagerTrait;
import io.github.nucleuspowered.nucleus.modules.kit.config.KitConfig;
import io.github.nucleuspowered.nucleus.modules.kit.config.KitConfigAdapter;
import io.github.nucleuspowered.nucleus.modules.kit.services.KitHandler;
import org.slf4j.Logger;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.filter.cause.Root;
import org.spongepowered.api.event.network.ClientConnectionEvent;

import java.util.List;

public class KitAutoRedeemListener implements ListenerBase.Conditional, Reloadable, InternalServiceManagerTrait {

    private final KitHandler handler = getServiceUnchecked(KitHandler.class);
    private final KitDataService gds = Nucleus.getNucleus().getKitDataService();
    private final Logger logger = Nucleus.getNucleus().getLogger();

    private boolean mustGetAll;
    private boolean logAutoRedeem = false;

    // TODO: Replace
    @Listener
    public void onPlayerJoin(ClientConnectionEvent.Join event, @Root Player player) {
        List<Kit> autoRedeemable = this.gds.getAutoRedeemable();
        String name = "[Kit Auto Redeem - " + player.getName() + "]: ";
        for (Kit kit : autoRedeemable) {
            String permission = PermissionRegistry.PERMISSIONS_PREFIX + "kits." + kit.getName().toLowerCase();
            String kitName = kit.getName();
            if (kit.ignoresPermission()) {
                log(name + kitName + " - permission check bypassed.");
            } else if (player.hasPermission(permission)) {
                log(name  + kitName + " - permission check " + permission + " passed.");
            } else {
                continue;
            }

            // Redeem kit in the normal way.
            // TODO: Move this logic into the handler while I'm not on a plane somwhere.
            try {
                this.handler.redeemKit(kit, player, true, true, this.mustGetAll, false);
                log(name  + kitName + " - kit redeemed.");
            } catch (KitRedeemException e) {
                if (this.logAutoRedeem) {
                    Nucleus.getNucleus().getLogger().error(name + kitName + " - kit could not be redeemed.", e);
                }
            }
        }
    }

    @Override public boolean shouldEnable() {
        return getServiceUnchecked(KitConfigAdapter.class).getNodeOrDefault().isEnableAutoredeem();
    }

    private void log(String message) {
        if (this.logAutoRedeem) {
            this.logger.info(message);
        }
    }

    @Override public void onReload() throws Exception {
        KitConfig kca = getServiceUnchecked(KitConfigAdapter.class).getNodeOrDefault();
        this.mustGetAll = kca.isMustGetAll();
        this.logAutoRedeem = kca.isLogAutoredeem();
    }
}
