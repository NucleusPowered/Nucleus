/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.jail.infoprovider;

import io.github.nucleuspowered.nucleus.api.module.jail.data.Jailing;
import io.github.nucleuspowered.nucleus.api.util.data.TimedEntry;
import io.github.nucleuspowered.nucleus.modules.jail.JailPermissions;
import io.github.nucleuspowered.nucleus.modules.jail.services.JailService;
import io.github.nucleuspowered.nucleus.core.services.INucleusServiceCollection;
import io.github.nucleuspowered.nucleus.core.services.impl.playerinformation.NucleusProvider;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.LinearComponents;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import org.spongepowered.api.command.CommandCause;
import org.spongepowered.api.entity.living.player.User;

import java.time.Duration;
import java.util.Optional;

public final class JailInfoProvider implements NucleusProvider {

    @Override public String getCategory() {
        return NucleusProvider.PUNISHMENT;
    }

    @Override public Optional<Component> get(final User user, final CommandCause source,
            final INucleusServiceCollection serviceCollection) {
        if (serviceCollection.permissionService().hasPermission(source, JailPermissions.BASE_CHECKJAIL)) {
            // If we have a ban service, then check for a ban.
            final JailService jh = serviceCollection.getServiceUnchecked(JailService.class);
            final Optional<Jailing> jailing = jh.getPlayerJailData(user.uniqueId());
            if (jailing.isPresent()) {
                final Jailing jd = jailing.get();
                final Component m;
                final Optional<Duration> duration = jd.getTimedEntry().map(TimedEntry::getRemainingTime);
                if (duration.isPresent()) {
                    m = serviceCollection.messageProvider().getMessageFor(source.audience(), "seen.isjailed.temp",
                            serviceCollection.messageProvider().getTimeString(source.audience(), duration.get()));
                } else {
                    m = serviceCollection.messageProvider().getMessageFor(source.audience(), "seen.isjailed.perm");
                }

                return Optional.of(
                        LinearComponents.linear(
                            m.clickEvent(ClickEvent.runCommand("/nucleus:checkjail " + user.name()))
                                .hoverEvent(HoverEvent.showText(serviceCollection.messageProvider().getMessageFor(source.audience(),
                                        "standard.clicktoseemore"))),
                        Component.newline(),
                        serviceCollection.messageProvider().getMessageFor(source.audience(), "standard.reason", jd.getReason())));
            }

            return Optional.of(serviceCollection.messageProvider().getMessageFor(source.audience(), "seen.notjailed"));
        }
        return Optional.empty();
    }
}
