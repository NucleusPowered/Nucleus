/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.freezeplayer.infoprovider;

import io.github.nucleuspowered.nucleus.modules.freezeplayer.FreezePlayerPermissions;
import io.github.nucleuspowered.nucleus.modules.freezeplayer.services.FreezePlayerService;
import io.github.nucleuspowered.nucleus.services.INucleusServiceCollection;
import io.github.nucleuspowered.nucleus.services.impl.playerinformation.NucleusProvider;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.text.Text;

import java.util.Optional;

public class FreezeInfoProvider implements NucleusProvider {

    @Override public String getCategory() {
        return NucleusProvider.PUNISHMENT;
    }

    @Override public Optional<Text> get(User user, CommandSource source,
            INucleusServiceCollection serviceCollection) {
        if (serviceCollection.permissionService().hasPermission(source, FreezePlayerPermissions.OTHERS_FREEZEPLAYER)) {
            if (serviceCollection.getServiceUnchecked(FreezePlayerService.class).getFromUUID(user.getUniqueId())) {
                return Optional.of(serviceCollection.messageProvider().getMessageFor(source, "seen.frozen"));
            }

            return Optional.of(serviceCollection.messageProvider().getMessageFor(source, "seen.notfrozen"));
        }

        return Optional.empty();
    }
}
