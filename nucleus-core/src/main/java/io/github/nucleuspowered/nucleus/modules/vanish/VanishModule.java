/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.vanish;

import com.google.common.collect.ImmutableMap;
import io.github.nucleuspowered.nucleus.modules.core.CoreModule;
import io.github.nucleuspowered.nucleus.modules.vanish.config.VanishConfig;
import io.github.nucleuspowered.nucleus.modules.vanish.config.VanishConfigAdapter;
import io.github.nucleuspowered.nucleus.modules.vanish.services.VanishService;
import io.github.nucleuspowered.nucleus.quickstart.module.ConfigurableModule;
import io.github.nucleuspowered.nucleus.services.INucleusServiceCollection;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.text.placeholder.PlaceholderParser;
import uk.co.drnaylor.quickstart.annotations.ModuleData;
import uk.co.drnaylor.quickstart.holders.DiscoveryModuleHolder;

import java.util.Map;
import java.util.function.Supplier;

import javax.inject.Inject;

@ModuleData(id = VanishModule.ID, name = "Vanish", dependencies = CoreModule.ID)
public class VanishModule extends ConfigurableModule<VanishConfig, VanishConfigAdapter> {

    public static final String ID = "vanish";

    @Inject
    public VanishModule(Supplier<DiscoveryModuleHolder<?, ?>> moduleHolder, INucleusServiceCollection collection) {
        super(moduleHolder, collection);
    }

    @Override
    public VanishConfigAdapter createAdapter() {
        return new VanishConfigAdapter();
    }

    @Override
    protected Map<String, PlaceholderParser> tokensToRegister() {
        return ImmutableMap.<String, PlaceholderParser>builder()
                .put("vanished",
                        PlaceholderParser.builder()
                                .plugin(this.serviceCollection.pluginContainer())
                                .id("vanished")
                                .name("Nucleus Vanished Indicator Token")
                                .parser(p -> {
                                    if (p.getAssociatedObject().filter(x -> x instanceof User)
                                            .map(x -> this.serviceCollection.getServiceUnchecked(VanishService.class).isVanished((User) x))
                                            .orElse(false)) {
                                        return Text.of(TextColors.GRAY, "[Vanished]");
                                    }
                                    return Text.EMPTY;
                                }).build()).build();
    }

}
