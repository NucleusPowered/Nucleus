/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.services.impl.reloadable;

import io.github.nucleuspowered.nucleus.services.INucleusServiceCollection;
import io.github.nucleuspowered.nucleus.services.interfaces.IReloadableService;

import java.util.HashSet;
import java.util.Set;

import com.google.inject.Singleton;

@Singleton
public class ReloadableService implements IReloadableService {

    private final Set<Reloadable> earlyReloadables = new HashSet<>();
    private final Set<Reloadable> reloadables = new HashSet<>();
    private final Set<DataLocationReloadable> dataLocationReloadables = new HashSet<>();

    @Override public void registerEarlyReloadable(Reloadable reloadable) {
        this.earlyReloadables.add(reloadable);
    }

    @Override public void registerReloadable(Reloadable reloadable) {
        this.reloadables.add(reloadable);
    }

    @Override public void fireReloadables(INucleusServiceCollection serviceCollection) {
        for (Reloadable reloadable : this.earlyReloadables) {
            reloadable.onReload(serviceCollection);
        }

        for (Reloadable reloadable1 : this.reloadables) {
            reloadable1.onReload(serviceCollection);
        }
    }

    @Override public void registerDataFileReloadable(DataLocationReloadable dataLocationReloadable) {
        this.dataLocationReloadables.add(dataLocationReloadable);
    }

    @Override public void fireDataFileReloadables(INucleusServiceCollection serviceCollection) {
        for (DataLocationReloadable dataLocationReloadable : this.dataLocationReloadables) {
            dataLocationReloadable.onDataFileLocationChange(serviceCollection);
        }
    }
}
