/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.sign.listeners;

import com.google.common.collect.Maps;
import io.github.nucleuspowered.nucleus.Nucleus;
import io.github.nucleuspowered.nucleus.internal.ListenerBase;
import io.github.nucleuspowered.nucleus.internal.PermissionRegistry;
import io.github.nucleuspowered.nucleus.internal.annotations.ConditionalListener;
import io.github.nucleuspowered.nucleus.internal.permissions.PermissionInformation;
import io.github.nucleuspowered.nucleus.internal.permissions.SuggestedLevel;
import io.github.nucleuspowered.nucleus.modules.sign.SignModule;
import io.github.nucleuspowered.nucleus.modules.sign.config.SignConfig;
import io.github.nucleuspowered.nucleus.modules.sign.config.SignConfigAdapter;
import org.spongepowered.api.data.manipulator.mutable.tileentity.SignData;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.block.tileentity.ChangeSignEvent;
import org.spongepowered.api.event.filter.cause.Root;
import org.spongepowered.api.text.serializer.TextSerializers;

import java.util.Map;
import java.util.function.Predicate;

@ConditionalListener(ColouredSignListener.Condition.class)
public class ColouredSignListener extends ListenerBase {

    private final String permission = PermissionRegistry.PERMISSIONS_PREFIX + "sign.formatting";

    @Listener
    public void onPlayerChangeSign(ChangeSignEvent event, @Root Player player) {
        SignData signData = event.getText();

        if (player.hasPermission(permission)) {
            for (int i = 0; i < signData.lines().size(); i++) {
                signData = signData.set(signData.lines().set(i, TextSerializers.FORMATTING_CODE.deserialize(signData.lines().get(i).toPlain())));
            }
        }
    }

    @Override
    public Map<String, PermissionInformation> getPermissions() {
        Map<String, PermissionInformation> mp = Maps.newHashMap();
        mp.put(permission, new PermissionInformation(plugin.getMessageProvider().getMessageWithFormat("permission.sign.formatting"), SuggestedLevel.ADMIN));
        return mp;
    }

    public static class Condition implements Predicate<Nucleus> {

        @Override public boolean test(Nucleus nucleus) {
            return nucleus.getConfigValue(SignModule.ID, SignConfigAdapter.class, SignConfig::isColouredSigns).orElse(false);
        }
    }
}
