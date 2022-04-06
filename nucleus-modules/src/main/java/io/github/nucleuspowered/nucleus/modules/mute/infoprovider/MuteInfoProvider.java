/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.mute.infoprovider;

import io.github.nucleuspowered.nucleus.api.module.mute.data.Mute;
import io.github.nucleuspowered.nucleus.modules.mute.MutePermissions;
import io.github.nucleuspowered.nucleus.modules.mute.services.MuteService;
import io.github.nucleuspowered.nucleus.core.services.INucleusServiceCollection;
import io.github.nucleuspowered.nucleus.core.services.impl.playerinformation.NucleusProvider;
import io.github.nucleuspowered.nucleus.core.services.interfaces.IMessageProviderService;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import org.spongepowered.api.command.CommandCause;
import org.spongepowered.api.entity.living.player.User;

import java.util.Optional;

public class MuteInfoProvider implements NucleusProvider {

    @Override public String getCategory() {
        return NucleusProvider.PUNISHMENT;
    }

    @Override public Optional<Component> get(final User user, final CommandCause source, final INucleusServiceCollection serviceCollection) {
        if (serviceCollection.permissionService().hasPermission(source, MutePermissions.BASE_CHECKMUTE)) {
            // If we have a ban service, then check for a ban.
            final MuteService jh = serviceCollection.getServiceUnchecked(MuteService.class);
            final IMessageProviderService messageProviderService = serviceCollection.messageProvider();
            final Audience audience = source.audience();
            if (jh.isMuted(user.uniqueId())) {
                final Mute jd = jh.getPlayerMuteInfo(user.uniqueId()).get();
                // Lightweight checkban.
                final Component m;
                if (jd.getTimedEntry().isPresent()) {
                    m = messageProviderService.getMessageFor(audience, "seen.ismuted.temp",
                            messageProviderService.getTimeString(audience, jd.getTimedEntry().get().getRemainingTime().getSeconds()));
                } else {
                    m = messageProviderService.getMessageFor(audience, "seen.ismuted.perm");
                }

                return Optional.of(Component.join(Component.newline(),
                        m.clickEvent(ClickEvent.runCommand("/checkmute " + user.name()))
                                .hoverEvent(HoverEvent.showText(
                                        messageProviderService.getMessageFor(audience, "standard.clicktoseemore"))),
                        messageProviderService.getMessageFor(audience, "standard.reason", jd.getReason())));
            }

            return Optional.of(messageProviderService.getMessageFor(audience, "seen.notmuted"));
        }
        return Optional.empty();
    }
}
