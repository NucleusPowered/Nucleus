/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.rtp.services;

import io.github.nucleuspowered.nucleus.api.module.rtp.NucleusRTPService;
import io.github.nucleuspowered.nucleus.api.module.rtp.kernel.RTPKernel;
import io.github.nucleuspowered.nucleus.api.module.rtp.kernel.RTPKernels;
import io.github.nucleuspowered.nucleus.modules.rtp.config.RTPConfig;
import io.github.nucleuspowered.nucleus.modules.rtp.options.RTPOptionsBuilder;
import io.github.nucleuspowered.nucleus.modules.rtp.registry.RTPRegistryModule;
import io.github.nucleuspowered.nucleus.scaffold.service.ServiceBase;
import io.github.nucleuspowered.nucleus.scaffold.service.annotations.APIService;
import io.github.nucleuspowered.nucleus.services.INucleusServiceCollection;
import io.github.nucleuspowered.nucleus.services.interfaces.IReloadableService;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.world.storage.WorldProperties;

import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;

import javax.inject.Inject;

@APIService(NucleusRTPService.class)
public class RTPService implements NucleusRTPService, IReloadableService.Reloadable, ServiceBase {

    private final Logger logger;
    private RTPConfig config = new RTPConfig();
    @Nullable private RTPKernel lazyLoadedKernel = null;
    private final Map<RTPConfig.PerWorldRTPConfig, RTPKernel> perWorldLazyLoadedKernel = new WeakHashMap<>();

    @Inject
    public RTPService(INucleusServiceCollection serviceCollection) {
        this.logger = serviceCollection.logger();
    }

    @Override
    public RTPOptions options(@Nullable WorldProperties world) {
        @Nullable String name = world == null ? null : world.getWorldName();
        return new io.github.nucleuspowered.nucleus.modules.rtp.options.RTPOptions(this.config, name);
    }

    @Override
    public RTPOptions.Builder optionsBuilder() {
        return new RTPOptionsBuilder();
    }

    @Override
    public RTPKernel getDefaultKernel() {
        if (this.lazyLoadedKernel == null) {
            // does the kernel exist?
            String kernelId = this.config.getDefaultRTPKernel();
            kernelId = kernelId.contains(":") ? kernelId : "nucleus:" + kernelId;
            Optional<RTPKernel> rtpKernel = Sponge.getRegistry().getType(RTPKernel.class, kernelId);
            if (!rtpKernel.isPresent()) {
                this.logger.warn("Kernel with ID {} could not be found. Falling back to the default.", RTPKernels.DEFAULT.getId());
                this.lazyLoadedKernel = RTPKernels.DEFAULT;
            } else {
                this.lazyLoadedKernel = rtpKernel.get();
            }
        }

        return this.lazyLoadedKernel;
    }

    @Override public RTPKernel getKernel(WorldProperties world) {
        return getKernel(world.getWorldName());
    }

    @Override public RTPKernel getKernel(String world) {
        return this.config.get(world).map(x -> {
            RTPKernel kernel = this.perWorldLazyLoadedKernel.get(x);
            if (kernel == null) {
                // does the kernel exist?
                String kernelId = x.getDefaultRTPKernel();
                kernelId = kernelId.contains(":") ? kernelId : "nucleus:" + kernelId;
                Optional<RTPKernel> rtpKernel = Sponge.getRegistry().getType(RTPKernel.class, kernelId);
                if (!rtpKernel.isPresent()) {
                    this.logger.warn("Kernel with ID {} for world {} could not be found. Falling back to the default.",
                            kernelId, world);
                    this.perWorldLazyLoadedKernel.put(x, RTPKernels.DEFAULT);
                } else {
                    this.perWorldLazyLoadedKernel.put(x, rtpKernel.get());
                }
            }

            return this.perWorldLazyLoadedKernel.get(x);
        }).orElseGet(this::getDefaultKernel);
    }

    @Override
    public void registerKernel(RTPKernel kernel) {
        RTPRegistryModule.getInstance().registerAdditionalCatalog(kernel);
    }

    @Override
    public void onReload(INucleusServiceCollection serviceCollection) {
        // create the new RTPOptions
        this.config = serviceCollection.moduleDataProvider().getModuleConfig(RTPConfig.class);
    }
}
