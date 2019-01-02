/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.jail.datamodules;

import io.github.nucleuspowered.nucleus.dataservices.modular.DataKey;
import io.github.nucleuspowered.nucleus.modules.jail.data.JailData;
import io.github.nucleuspowered.nucleus.storage.dataobjects.modules.IUserDataModule;

import java.util.Optional;

import javax.annotation.Nullable;

public class JailUserDataModule implements IUserDataModule {

    @DataKey("jailData")
    @Nullable
    private JailData jailData;

    @DataKey("jailOnNextLogin")
    private boolean jailOnNextLogin = false;

    public Optional<JailData> getJailData() {
        return Optional.ofNullable(this.jailData);
    }

    public void setJailData(@Nullable JailData jailData) {
        this.jailData = jailData;
    }

    public boolean jailOnNextLogin() {
        return this.jailOnNextLogin;
    }

    public void setJailOnNextLogin(boolean set) {
        this.jailOnNextLogin = set && !getService().getPlayer().isPresent();
    }

    public void removeJailData() {
        this.jailOnNextLogin = false;
        setJailData(null);
    }
}
