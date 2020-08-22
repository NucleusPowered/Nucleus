/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.rules;

import io.github.nucleuspowered.nucleus.io.TextFileController;
import io.github.nucleuspowered.nucleus.modules.rules.config.RulesConfig;
import io.github.nucleuspowered.nucleus.modules.rules.config.RulesConfigAdapter;
import io.github.nucleuspowered.nucleus.quickstart.module.ConfigurableModule;
import io.github.nucleuspowered.nucleus.services.INucleusServiceCollection;
import org.spongepowered.api.Sponge;
import uk.co.drnaylor.quickstart.annotations.ModuleData;
import uk.co.drnaylor.quickstart.holders.DiscoveryModuleHolder;

import java.util.function.Supplier;

import javax.inject.Inject;

@ModuleData(id = "rules", name = "Rules")
public class RulesModule extends ConfigurableModule<RulesConfig, RulesConfigAdapter> {

    public static final String RULES_KEY = "rules";

    @Inject
    public RulesModule(Supplier<DiscoveryModuleHolder<?, ?>> moduleHolder, INucleusServiceCollection collection) {
        super(moduleHolder, collection);
    }

    @Override
    public RulesConfigAdapter createAdapter() {
        return new RulesConfigAdapter();
    }

    @Override public void performPreTasks(INucleusServiceCollection serviceCollection) throws Exception {
        super.performPreTasks(serviceCollection);

        serviceCollection.textFileControllerCollection()
                .register(RULES_KEY,
                        new TextFileController(
                                serviceCollection.textTemplateFactory(),
                                Sponge.getAssetManager().getAsset(serviceCollection.pluginContainer(), "rules.txt").get(),
                                serviceCollection.configDir().resolve("rules.txt")
                        ));
    }
}
