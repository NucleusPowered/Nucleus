/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.ban.infoprovider;

import io.github.nucleuspowered.nucleus.modules.ban.BanPermissions;
import io.github.nucleuspowered.nucleus.services.INucleusServiceCollection;
import io.github.nucleuspowered.nucleus.services.impl.playerinformation.NucleusProvider;
import io.github.nucleuspowered.nucleus.services.interfaces.IMessageProviderService;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.service.ban.BanService;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.action.TextActions;
import org.spongepowered.api.text.serializer.TextSerializers;
import org.spongepowered.api.util.ban.Ban;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

public class BackInfoProvider implements NucleusProvider {

    @Override public String getCategory() {
        return "punishment";
    }

    @Override public Optional<Text> get(User user, CommandSource source,
            INucleusServiceCollection serviceCollection) {
        if (serviceCollection.permissionService().hasPermission(source, BanPermissions.BASE_CHECKBAN)) {
            // If we have a ban service, then check for a ban.
            Optional<BanService> obs = Sponge.getServiceManager().provide(BanService.class);
            IMessageProviderService messageProviderService = serviceCollection.messageProvider();
            if (obs.isPresent()) {
                Optional<Ban.Profile> bs = obs.get().getBanFor(user.getProfile());
                if (bs.isPresent()) {

                    // Lightweight checkban.
                    Text.Builder m;
                    if (bs.get().getExpirationDate().isPresent()) {
                        m = messageProviderService.getMessageFor(source, "seen.isbanned.temp",
                                messageProviderService.getTimeString(source.getLocale(),
                                        Duration.between(Instant.now(), bs.get().getExpirationDate().get()))).toBuilder();
                    } else {
                        m = messageProviderService.getMessageFor(source, "seen.isbanned.perm").toBuilder();
                    }

                    return Optional.of(
                            Text.of(
                                m.onClick(TextActions.runCommand("/checkban " + user.getName()))
                                    .onHover(TextActions.showText(
                                            messageProviderService.getMessageFor(source, "standard.clicktoseemore")))
                                    .build(),
                                Text.NEW_LINE,
                                messageProviderService.getMessageFor(source, "standard.reason",
                                    TextSerializers.FORMATTING_CODE.serialize(
                                            bs.get().getReason().orElseGet(() ->
                                                    messageProviderService.getMessageFor(source,"standard.unknown"))))));
                }
            }

            return Optional.of(messageProviderService.getMessageFor(source.getLocale(), "seen.notbanned"));
        }

        return Optional.empty();
    }
}
