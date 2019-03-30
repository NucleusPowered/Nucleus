/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.vanish.listener;

import io.github.nucleuspowered.nucleus.Nucleus;
import io.github.nucleuspowered.nucleus.internal.interfaces.ListenerBase;
import io.github.nucleuspowered.nucleus.internal.interfaces.Reloadable;
import io.github.nucleuspowered.nucleus.internal.userprefs.UserPreferenceService;
import io.github.nucleuspowered.nucleus.modules.vanish.VanishKeys;
import io.github.nucleuspowered.nucleus.modules.vanish.VanishUserPrefKeys;
import io.github.nucleuspowered.nucleus.modules.vanish.commands.VanishCommand;
import io.github.nucleuspowered.nucleus.modules.vanish.config.VanishConfig;
import io.github.nucleuspowered.nucleus.modules.vanish.config.VanishConfigAdapter;
import io.github.nucleuspowered.nucleus.modules.vanish.services.VanishService;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.filter.Getter;
import org.spongepowered.api.event.network.ClientConnectionEvent;
import org.spongepowered.api.text.action.TextActions;

public class VanishListener implements Reloadable, ListenerBase {

    private VanishConfig vanishConfig = new VanishConfig();
    private VanishService service = getServiceUnchecked(VanishService.class);

    private final String permission = getPermissionHandlerFor(VanishCommand.class).getPermissionWithSuffix("persist");
    public static final String LOGIN_VANISH_PERMISSION = "nucleus.vanish.onlogin";

    @Listener
    public void onLogin(ClientConnectionEvent.Join event, @Getter("getTargetEntity") Player player) {
        boolean persist = this.service.isVanished(player);

        boolean shouldVanish = (hasPermission(player, LOGIN_VANISH_PERMISSION)
                && getServiceUnchecked(UserPreferenceService.class).get(player.getUniqueId(), VanishUserPrefKeys.VANISH_ON_LOGIN).orElse(false))
                || persist;

        if (shouldVanish) {
            if (!hasPermission(player, this.permission)) {
                // No permission, no vanish.
                this.service.unvanishPlayer(player);
                return;
            } else if (this.vanishConfig.isSuppressMessagesOnVanish()) {
                event.setMessageCancelled(true);
            }

            this.service.vanishPlayer(player, true);
            sendMessageTo(player, "vanish.login");

            if (!persist) {
                // on login
                player.sendMessage(getMessageFor(player, "vanish.onlogin.prefs").toBuilder()
                        .onClick(TextActions.runCommand("/nuserprefs vanish-on-login false")).build());
            }
        }
    }

    @Listener
    public void onQuit(ClientConnectionEvent.Disconnect event, @Getter("getTargetEntity") Player player) {
        if (player.get(Keys.VANISH).orElse(false)) {
            Nucleus.getNucleus().getStorageManager().getUserService().get(player.getUniqueId())
                    .thenAccept(x -> x.ifPresent(t -> t.set(VanishKeys.VANISH_STATUS, false)));
            if (this.vanishConfig.isSuppressMessagesOnVanish()) {
                event.setMessageCancelled(true);
            }
        }
    }

    @Override
    public void onReload() {
        this.vanishConfig = getServiceUnchecked(VanishConfigAdapter.class).getNodeOrDefault();
    }
}
