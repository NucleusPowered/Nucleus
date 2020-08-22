/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.rtp.registry;

import io.github.nucleuspowered.nucleus.api.module.rtp.kernel.RTPKernel;
import io.github.nucleuspowered.nucleus.api.module.rtp.kernel.RTPKernels;
import io.github.nucleuspowered.nucleus.modules.rtp.kernels.AroundPlayerAndSurfaceKernel;
import io.github.nucleuspowered.nucleus.modules.rtp.kernels.AroundPlayerKernel;
import io.github.nucleuspowered.nucleus.modules.rtp.kernels.DefaultKernel;
import io.github.nucleuspowered.nucleus.modules.rtp.kernels.SurfaceKernel;
import io.github.nucleuspowered.nucleus.scaffold.registry.NucleusRegistryModule;
import io.github.nucleuspowered.nucleus.scaffold.registry.Registry;

import javax.inject.Singleton;

@Singleton
@Registry(RTPKernels.class)
public class RTPRegistryModule extends NucleusRegistryModule<RTPKernel> {

    private static RTPRegistryModule INSTANCE;

    public static RTPRegistryModule getInstance() {
        if (INSTANCE == null) {
            throw new IllegalStateException("Instance is not yet initialised");
        }
        return INSTANCE;
    }

    public RTPRegistryModule() {
        if (INSTANCE != null) {
            throw new IllegalStateException("Singleton already exists");
        }
        INSTANCE = this;
    }

    @Override
    public Class<RTPKernel> catalogClass() {
        return RTPKernel.class;
    }

    @Override
    public void registerModuleDefaults() {
        this.registerAdditionalCatalog(DefaultKernel.INSTANCE);
        this.registerAdditionalCatalog(new AroundPlayerAndSurfaceKernel());
        this.registerAdditionalCatalog(new AroundPlayerKernel());
        this.registerAdditionalCatalog(new SurfaceKernel());
    }

}
