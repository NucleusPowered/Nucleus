/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.warn.handlers;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import io.github.nucleuspowered.nucleus.Nucleus;
import io.github.nucleuspowered.nucleus.api.data.WarnData;
import io.github.nucleuspowered.nucleus.api.service.NucleusWarnService;
import io.github.nucleuspowered.nucleus.dataservices.UserService;
import io.github.nucleuspowered.nucleus.dataservices.loaders.UserDataManager;
import org.spongepowered.api.entity.living.player.User;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public class WarnHandler implements NucleusWarnService {

    private final Nucleus nucleus;
    @Inject private UserDataManager userDataManager;

    public WarnHandler(Nucleus nucleus) {
        this.nucleus = nucleus;
    }

    @Override
    public List<WarnData> getWarnings(User user) {
        Optional<UserService> userService = userDataManager.get(user);
        if (userService.isPresent()) {
            return userService.get().getWarnings();
        }
        return null;
    }

    @Override
    public boolean addWarning(User user, WarnData warning) {
        Preconditions.checkNotNull(user);
        Preconditions.checkNotNull(warning);

        Optional<UserService> optUserService = userDataManager.get(user);
        if (!optUserService.isPresent()) {
            return false;
        }

        UserService userService = optUserService.get();
        if (user.isOnline() && warning.getTimeFromNextLogin().isPresent() && !warning.getEndTimestamp().isPresent()) {
            warning = new WarnData(warning.getWarner(), warning.getReason(), Instant.now().plus(warning.getTimeFromNextLogin().get()));
        }

        userService.addWarning(warning);
        return true;
    }

    @Override
    public boolean removeWarning(User user, WarnData warning) {
        Optional<UserService> userService = userDataManager.get(user);
        if (userService.isPresent()) {
            userService.get().removeWarning(warning);
            return true;
        }

        return false;
    }

    @Override
    public boolean clearWarnings(User user) {
        Optional<UserService> userService = userDataManager.get(user);
        if (userService.isPresent()) {
            userService.get().clearWarnings();
            return true;
        }

        return false;
    }
}
