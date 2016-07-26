/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.warn.handlers;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import io.github.nucleuspowered.nucleus.Nucleus;
import io.github.nucleuspowered.nucleus.api.data.WarnData;
import io.github.nucleuspowered.nucleus.api.service.NucleusWarnService;
import io.github.nucleuspowered.nucleus.dataservices.UserService;
import io.github.nucleuspowered.nucleus.dataservices.loaders.UserDataManager;
import io.github.nucleuspowered.nucleus.modules.warn.config.WarnConfigAdapter;
import org.spongepowered.api.entity.living.player.User;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class WarnHandler implements NucleusWarnService {

    private final Nucleus nucleus;
    @Inject private UserDataManager userDataManager;
    @Inject private WarnConfigAdapter wca;

    public WarnHandler(Nucleus nucleus) {
        this.nucleus = nucleus;
    }

    @Override
    public List<WarnData> getWarnings(User user) {
        return getWarnings(user, true, true);
    }

    @Override
    public List<WarnData> getWarnings(User user, boolean includeActive, boolean includeExpired) {
        Optional<UserService> userService = userDataManager.get(user);
        if (userService.isPresent()) {
            List<WarnData> warnings = userService.get().getWarnings();
            if (!includeActive) {
                warnings = warnings.stream().filter(warnData -> warnData.isExpired()).collect(Collectors.toList());
            }
            if (!includeExpired) {
                warnings = warnings.stream().filter(warnData -> !warnData.isExpired()).collect(Collectors.toList());
            }
            return warnings;
        }
        return Lists.newArrayList();
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
        return removeWarning(user, warning, false);
    }

    @Override
    public boolean removeWarning(User user, WarnData warning, boolean permanent) {
        Optional<UserService> userService = userDataManager.get(user);
        if (userService.isPresent()) {
            userService.get().removeWarning(warning);
            if (wca.getNodeOrDefault().isExpireWarnings() && !warning.isExpired() && !permanent) {
                userService.get().addWarning(new WarnData(warning.getWarner(), warning.getReason(), true));
            }
            return true;
        }

        return false;
    }

    @Override
    public boolean clearWarnings(User user, boolean clearActive, boolean clearExpired) {
        Optional<UserService> userService = userDataManager.get(user);
        if (userService.isPresent()) {
            List<WarnData> warnings = userService.get().getWarnings();

            if (!warnings.isEmpty()) {
                if (!clearActive && !clearExpired) {
                    warnings.stream().filter(warnData -> !warnData.isExpired()).forEach(warnData -> removeWarning(user, warnData));
                    return true;
                }

                if (clearActive) {
                    warnings.stream().filter(warnData -> !warnData.isExpired()).forEach(warnData -> removeWarning(user, warnData, true));
                }
                if (clearExpired) {
                    warnings.stream().filter(warnData -> warnData.isExpired()).forEach(warnData -> removeWarning(user, warnData, true));
                }
                return true;
            } else {
                return false;
            }
        }

        return false;
    }

    @Override
    public boolean updateWarnings(User user) {
        Optional<UserService> userService = userDataManager.get(user);
        if (!userService.isPresent()) {
            return false;
        }

        for (WarnData warning : getWarnings(user)) {
            warning.nextLoginToTimestamp();

            if (warning.getEndTimestamp().isPresent() && warning.getEndTimestamp().get().isBefore(Instant.now())) {
                removeWarning(user, warning);
            }
        }
        return true;
    }
}
