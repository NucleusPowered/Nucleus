/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.warn.services;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import io.github.nucleuspowered.nucleus.Nucleus;
import io.github.nucleuspowered.nucleus.NucleusPlugin;
import io.github.nucleuspowered.nucleus.Util;
import io.github.nucleuspowered.nucleus.api.nucleusdata.Warning;
import io.github.nucleuspowered.nucleus.api.service.NucleusWarningService;
import io.github.nucleuspowered.nucleus.internal.annotations.APIService;
import io.github.nucleuspowered.nucleus.internal.interfaces.Reloadable;
import io.github.nucleuspowered.nucleus.internal.interfaces.ServiceBase;
import io.github.nucleuspowered.nucleus.modules.warn.WarnKeys;
import io.github.nucleuspowered.nucleus.modules.warn.WarnModule;
import io.github.nucleuspowered.nucleus.modules.warn.config.WarnConfig;
import io.github.nucleuspowered.nucleus.modules.warn.config.WarnConfigAdapter;
import io.github.nucleuspowered.nucleus.modules.warn.data.WarnData;
import io.github.nucleuspowered.nucleus.modules.warn.events.WarnEvent;
import io.github.nucleuspowered.nucleus.storage.dataobjects.modular.IUserDataObject;
import io.github.nucleuspowered.nucleus.util.CauseStackHelper;
import io.github.nucleuspowered.storage.dataobjects.keyed.IKeyedDataObject;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.event.cause.Cause;

import javax.annotation.Nullable;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@APIService(NucleusWarningService.class)
public class WarnHandler implements NucleusWarningService, Reloadable, ServiceBase {

    private boolean expireWarnings = false;

    public List<WarnData> getWarningsInternal(User user) {
        return getWarningsInternal(user, true, true);
    }

    public List<WarnData> getWarningsInternal(User user, boolean includeActive, boolean includeExpired) {
        List<WarnData> warnings = Nucleus.getNucleus().getStorageManager()
                .getUserService()
                .getOnThread(user.getUniqueId())
                .flatMap(x -> x.get(WarnKeys.WARNINGS))
                .<List<WarnData>>map(Lists::newArrayList)
                .orElseGet(ImmutableList::of);

        if (!warnings.isEmpty()) {
            if (!includeActive) {
                warnings.removeIf(x -> !x.isExpired());
            }
            if (!includeExpired) {
                warnings.removeIf(x -> !x.isExpired());
            }
            return warnings;
        }
        return Lists.newArrayList();
    }

    public boolean addWarning(User user, WarnData warning) {
        Preconditions.checkNotNull(user);
        Preconditions.checkNotNull(warning);

        boolean changed = false;
        IUserDataObject dataObject = Nucleus.getNucleus()
                .getStorageManager()
                .getUserService()
                .getOrNewOnThread(user.getUniqueId());
        try (IKeyedDataObject.Value<List<WarnData>> data = dataObject.getAndSet(WarnKeys.WARNINGS)) {
            List<WarnData> value = data.getValue()
                    .<List<WarnData>>map(ArrayList::new)
                    .orElseGet(ImmutableList::of);
            if (value.isEmpty()) {
                return false;
            }

            Optional<Duration> duration = warning.getTimeFromNextLogin();
            warning.nextLoginToTimestamp();
            value.add(warning);
            changed = true;

            if (!warning.isExpired()) {
                Sponge.getEventManager().post(new WarnEvent.Warned(
                        CauseStackHelper.createCause(warning.getWarner().orElse(Util.consoleFakeUUID)),
                        user,
                        warning.getReason(),
                        duration.orElseGet(() -> warning.getRemainingTime().orElse(null))
                ));
            }

            return true;
        } finally {
            if (changed) {
                Nucleus.getNucleus().getStorageManager().getUserService().save(user.getUniqueId(), dataObject);
            }
        }
    }

    @Override
    public void onReload() {
        this.expireWarnings = Nucleus.getNucleus().getConfigValue(WarnModule.ID, WarnConfigAdapter.class, WarnConfig::isExpireWarnings).orElse(false);
    }

    public boolean removeWarning(User user, WarnData warning) {
        return removeWarning(user, warning, false, CauseStackHelper.createCause(NucleusPlugin.getNucleus()));
    }

    public boolean removeWarning(User user, Warning warning, boolean permanent, Cause of) {

        boolean changed = false;
        IUserDataObject dataObject = Nucleus.getNucleus()
                .getStorageManager()
                .getUserService()
                .getOrNewOnThread(user.getUniqueId());
        try (IKeyedDataObject.Value<List<WarnData>> data = dataObject.getAndSet(WarnKeys.WARNINGS)) {
            List<WarnData> value = data.getValue()
                    .<List<WarnData>>map(ArrayList::new)
                    .orElseGet(ImmutableList::of);
            if (value.isEmpty()) {
                return false;
            }

            changed = value.removeIf(x -> x.equals(warning));
            if (this.expireWarnings && !warning.isExpired() && !permanent) {
                value.add(new WarnData(
                        warning.getDate(),
                        warning.getWarner().orElse(Util.consoleFakeUUID),
                        warning.getReason(),
                        true
                ));

                changed = true;
            }

            if (!warning.isExpired()) {
                Sponge.getEventManager().post(new WarnEvent.Expire(
                        of,
                        user,
                        warning.getReason(),
                        warning.getWarner().orElse(null)
                ));
            }

            return true;
        } finally {
            if (changed) {
                Nucleus.getNucleus().getStorageManager().getUserService().save(user.getUniqueId(), dataObject);
            }
        }
    }

    public boolean clearWarnings(User user, boolean clearActive, boolean clearExpired, Cause of) {
        boolean changed = false;
        IUserDataObject dataObject = Nucleus.getNucleus()
                .getStorageManager()
                .getUserService()
                .getOrNewOnThread(user.getUniqueId());
        try (IKeyedDataObject.Value<List<WarnData>> data = dataObject.getAndSet(WarnKeys.WARNINGS)) {
            List<WarnData> value = data.getValue()
                    .<List<WarnData>>map(ArrayList::new)
                    .orElseGet(ImmutableList::of);
            if (value.isEmpty()) {
                return false;
            }

            if (clearActive) {
                changed = value.removeIf(x -> !x.isExpired());
            }

            if (clearExpired) {
                // boolean OR because we want to execute this anyway.
                changed = changed | value.removeIf(WarnData::isExpired);
            }

            return changed;
        } finally {
            if (changed) {
                Nucleus.getNucleus().getStorageManager().getUserService().save(user.getUniqueId(), dataObject);
            }
        }

    }

    public boolean updateWarnings(User user) {
        boolean changed = false;
        IUserDataObject dataObject = Nucleus.getNucleus()
                .getStorageManager()
                .getUserService()
                .getOrNewOnThread(user.getUniqueId());
        try (IKeyedDataObject.Value<List<WarnData>> data = dataObject.getAndSet(WarnKeys.WARNINGS)) {
            List<WarnData> value = data.getValue()
                    .<List<WarnData>>map(ArrayList::new)
                    .orElseGet(ImmutableList::of);
            if (value.isEmpty()) {
                return false;
            }

            for (WarnData warning : getWarningsInternal(user)) {
                warning.nextLoginToTimestamp();

                if (warning.getEndTimestamp().isPresent() && warning.getEndTimestamp().get().isBefore(Instant.now())) {
                    changed = changed | removeWarning(user, warning);
                }
            }

            return changed;
        }  finally {
            if (changed) {
                Nucleus.getNucleus().getStorageManager().getUserService().save(user.getUniqueId(), dataObject);
            }
        }

    }

    @Override public boolean addWarning(User toWarn, CommandSource warner, String reason, @Nullable Duration duration) {
        return addWarning(toWarn, new WarnData(Instant.now(), Util.getUUID(warner), reason, duration));
    }

    @Override public List<Warning> getWarnings(User user) {
        return ImmutableList.copyOf(getWarningsInternal(user));
    }

    @Override public boolean expireWarning(User user, Warning warning, Cause cause) {
        return removeWarning(user, warning, false, cause);
    }

}
