/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.sign.listeners;

import io.github.nucleuspowered.nucleus.modules.sign.SignPermissions;
import io.github.nucleuspowered.nucleus.scaffold.listener.ListenerBase;
import io.github.nucleuspowered.nucleus.services.INucleusServiceCollection;
import io.github.nucleuspowered.nucleus.services.interfaces.IPermissionService;
import org.spongepowered.api.data.manipulator.mutable.tileentity.SignData;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.block.tileentity.ChangeSignEvent;
import org.spongepowered.api.event.filter.cause.Root;
import org.spongepowered.api.text.serializer.TextSerializers;

import javax.inject.Inject;

public class SignListener implements ListenerBase {

    private final IPermissionService permissionService;

    @Inject
    public SignListener(INucleusServiceCollection serviceCollection) {
        this.permissionService = serviceCollection.permissionService();
    }

    @Listener
    public void onPlayerChangeSign(ChangeSignEvent event, @Root Player player) {
        SignData signData = event.getText();

        if (this.permissionService.hasPermission(player, SignPermissions.SIGN_FORMATTING)) {
            for (int i = 0; i < signData.lines().size(); i++) {
                signData = signData.set(signData.lines().set(i, TextSerializers.FORMATTING_CODE.deserialize(signData.lines().get(i).toPlain())));
            }
        }
    }

}
