/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.services.impl.dataversioning;

import com.google.inject.Singleton;
import io.github.nucleuspowered.nucleus.modules.core.CoreKeys;
import io.github.nucleuspowered.nucleus.services.impl.storage.dataobjects.modular.IGeneralDataObject;
import io.github.nucleuspowered.nucleus.services.impl.storage.dataobjects.modular.IUserDataObject;
import io.github.nucleuspowered.nucleus.services.impl.storage.dataobjects.modular.IWorldDataObject;
import io.github.nucleuspowered.nucleus.services.interfaces.IDataVersioning;

@Singleton
public final class DataVersioning implements IDataVersioning {

    private final int userDataVersion = 2;
    private final int worldDataVersion = 1;
    private final int generalDataVersion = 1;

    @Override
    public void setVersion(IUserDataObject userDataObject) {
        userDataObject.set(CoreKeys.USER_VERSION, this.userDataVersion);
    }

    @Override
    public void setVersion(IWorldDataObject worldDataObject) {
        worldDataObject.set(CoreKeys.WORLD_VERSION, this.worldDataVersion);
    }

    @Override
    public void setVersion(IGeneralDataObject generalDataObject) {
        generalDataObject.set(CoreKeys.GENERAL_VERSION, this.generalDataVersion);
    }

    @Override
    public void migrate(IUserDataObject userDataObject) {
        final int currentVersion = userDataObject.get(CoreKeys.USER_VERSION).orElse(1);
        if (currentVersion < this.userDataVersion) {
            if (currentVersion <= 1) {
                userDataObject.set(CoreKeys.FIRST_JOIN_PROCESSED, true);
            }
            userDataObject.set(CoreKeys.USER_VERSION, this.userDataVersion);
        }
    }

    @Override
    public void migrate(IWorldDataObject worldDataObject) {
        final int currentVersion = worldDataObject.get(CoreKeys.WORLD_VERSION).orElse(0);
        if (currentVersion == 0) {
            worldDataObject.set(CoreKeys.WORLD_VERSION, this.worldDataVersion);
        }
    }

    @Override
    public void migrate(IGeneralDataObject generalDataObject) {
        final int currentVersion = generalDataObject.get(CoreKeys.GENERAL_VERSION).orElse(0);
        if (currentVersion == 0) {
            generalDataObject.set(CoreKeys.GENERAL_VERSION, this.generalDataVersion);
        }
    }
}
