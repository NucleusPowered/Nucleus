/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.kit;

import com.google.common.collect.ImmutableMap;
import io.github.nucleuspowered.nucleus.modules.kit.config.KitConfig;
import io.github.nucleuspowered.nucleus.modules.kit.config.KitConfigAdapter;
import io.github.nucleuspowered.nucleus.modules.kit.placeholder.KitCooldownPlaceholder;
import io.github.nucleuspowered.nucleus.modules.kit.services.KitService;
import io.github.nucleuspowered.nucleus.quickstart.module.ConfigurableModule;
import io.github.nucleuspowered.nucleus.services.INucleusServiceCollection;
import org.spongepowered.api.text.placeholder.PlaceholderParser;
import uk.co.drnaylor.quickstart.annotations.ModuleData;
import uk.co.drnaylor.quickstart.holders.DiscoveryModuleHolder;

import java.util.Map;
import java.util.function.Supplier;

import javax.inject.Inject;

@ModuleData(id = KitModule.ID, name = "Kit")
public class KitModule extends ConfigurableModule<KitConfig, KitConfigAdapter> {

    public static final String ID = "kit";

    @Inject
    public KitModule(Supplier<DiscoveryModuleHolder<?, ?>> moduleHolder, INucleusServiceCollection collection) {
        super(moduleHolder, collection);
    }

    @Override
    public KitConfigAdapter createAdapter() {
        return new KitConfigAdapter();
    }

    protected Map<String, PlaceholderParser> tokensToRegister(final INucleusServiceCollection serviceCollection) {
        return ImmutableMap.of("kitcooldown", new KitCooldownPlaceholder(
                serviceCollection.storageManager(),
                serviceCollection.getServiceUnchecked(KitService.class),
                serviceCollection.messageProvider()));
    }


}
