/*
 * This file is part of Essence, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.essencepowered.essence;

import io.github.essencepowered.essence.internal.interfaces.InternalEssenceUser;
import io.github.essencepowered.essence.internal.services.datastore.UserConfigLoader;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.service.user.UserStorageService;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.action.TextActions;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

public class NameUtil {
    public static Text getNameFromCommandSource(CommandSource src) {
        if (!(src instanceof User)) {
            return Text.of(src.getName());
        }

        return getName((User)src);
    }

    public static Text getName(User player, InternalEssenceUser service) {
        Optional<Text> n = service.getNicknameWithPrefix();
        if (n.isPresent()) {
            return n.get();
        }

        return getName(player);
    }

    public static Text getNameWithHover(User player, UserConfigLoader loader) {
        return getName(player, loader).toBuilder().onHover(TextActions.showText(Text.of(player.getName()))).build();
    }

    public static Text getName(User player, UserConfigLoader loader) {
        try {
            InternalEssenceUser iq = loader.getUser(player);
            return getName(player, iq);
        } catch (IOException | ObjectMappingException e) {
        }

        return getName(player);
    }

    public static Text getName(User player) {
        Optional<Text> vt = player.get(Keys.DISPLAY_NAME);
        boolean b = player.get(Keys.SHOWS_DISPLAY_NAME).orElse(false);
        if (b) {
            return vt.get();
        }

        return Text.of(player.getName());
    }

    public static String getNameFromUUID(UUID uuid) {
        if (Util.consoleFakeUUID.equals(uuid)) {
            return Sponge.getServer().getConsole().getName();
        }

        UserStorageService uss = Sponge.getServiceManager().provideUnchecked(UserStorageService.class);
        Optional<User> user = uss.get(uuid);
        if (user.isPresent()) {
            return user.get().getName();
        }

        return Util.getMessageWithFormat("standard.unknown");
    }
}
