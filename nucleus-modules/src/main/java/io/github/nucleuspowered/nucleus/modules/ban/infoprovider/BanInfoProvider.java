/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.ban.infoprovider;

import io.github.nucleuspowered.nucleus.modules.ban.BanPermissions;
import io.github.nucleuspowered.nucleus.core.services.INucleusServiceCollection;
import io.github.nucleuspowered.nucleus.core.services.impl.playerinformation.NucleusProvider;
import io.github.nucleuspowered.nucleus.core.services.interfaces.IMessageProviderService;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.LinearComponents;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandCause;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.service.ban.Ban;
import org.spongepowered.api.service.ban.BanService;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

public class BanInfoProvider implements NucleusProvider {

    @Override public String getCategory() {
        return "punishment";
    }

    @Override public Optional<Component> get(final User user, final CommandCause source,
            final INucleusServiceCollection serviceCollection) {
        if (serviceCollection.permissionService().hasPermission(source, BanPermissions.BASE_CHECKBAN)) {
            // If we have a ban service, then check for a ban.
            final BanService obs = Sponge.server().serviceProvider().banService();
            final IMessageProviderService messageProviderService = serviceCollection.messageProvider();

            // TODO: Async
            final Optional<Ban.Profile> bs = obs.find(user.profile()).join();
            final Audience audience = source.audience();
            if (bs.isPresent()) {

                // Lightweight checkban.
                final Component m;
                if (bs.get().expirationDate().isPresent()) {
                    m = messageProviderService.getMessageFor(audience, "seen.isbanned.temp",
                            messageProviderService.getTimeString(audience,
                                    Duration.between(Instant.now(), bs.get().expirationDate().get())));
                } else {
                    m = messageProviderService.getMessageFor(audience, "seen.isbanned.perm");
                }

                return Optional.of(
                        LinearComponents.linear(
                            m.clickEvent(ClickEvent.runCommand("/checkban " + user.name()))
                                .hoverEvent(HoverEvent.showText(
                                        messageProviderService.getMessageFor(audience, "standard.clicktoseemore"))),
                            Component.newline(),
                            messageProviderService.getMessageFor(audience, "standard.reason",
                                LegacyComponentSerializer.legacyAmpersand().serialize(
                                        bs.get().reason().orElseGet(() ->
                                                messageProviderService.getMessageFor(audience,"standard.unknown"))))));
            }

            return Optional.of(messageProviderService.getMessageFor(audience, "seen.notbanned"));
        }

        return Optional.empty();
    }
}
