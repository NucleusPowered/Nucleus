/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.jail.infoprovider;

import io.github.nucleuspowered.nucleus.modules.jail.JailPermissions;
import io.github.nucleuspowered.nucleus.modules.jail.data.JailData;
import io.github.nucleuspowered.nucleus.modules.jail.services.JailHandler;
import io.github.nucleuspowered.nucleus.services.INucleusServiceCollection;
import io.github.nucleuspowered.nucleus.services.impl.playerinformation.NucleusProvider;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.action.TextActions;

import java.util.Optional;

public class JailInfoProvider implements NucleusProvider {

    @Override public String getCategory() {
        return NucleusProvider.PUNISHMENT;
    }

    @Override public Optional<Text> get(User user, CommandSource source,
            INucleusServiceCollection serviceCollection) {
        if (serviceCollection.permissionService().hasPermission(source, JailPermissions.BASE_CHECKJAIL)) {
            // If we have a ban service, then check for a ban.
            JailHandler jh = serviceCollection.getServiceUnchecked(JailHandler.class);
            if (jh.isPlayerJailed(user)) {
                JailData jd = jh.getPlayerJailDataInternal(user).get();
                Text.Builder m;
                if (jd.getRemainingTime().isPresent()) {
                    m = serviceCollection.messageProvider().getMessageFor(source, "seen.isjailed.temp",
                            serviceCollection.messageProvider().getTimeString(source.getLocale(),
                                    jd.getRemainingTime().get().getSeconds())).toBuilder();
                } else {
                    m = serviceCollection.messageProvider().getMessageFor(source.getLocale(), "seen.isjailed.perm").toBuilder();
                }

                return Optional.of(
                        Text.of(
                            m.onClick(TextActions.runCommand("/nucleus:checkjail " + user.getName()))
                                .onHover(TextActions.showText(serviceCollection.messageProvider().getMessageFor(source.getLocale(),
                                        "standard.clicktoseemore"))).build(),
                        Text.NEW_LINE,
                        serviceCollection.messageProvider().getMessageFor(source.getLocale(), "standard.reason", jd.getReason())));
            }

            return Optional.of(serviceCollection.messageProvider().getMessageFor(source.getLocale(), "seen.notjailed"));
        }
        return Optional.empty();
    }
}
