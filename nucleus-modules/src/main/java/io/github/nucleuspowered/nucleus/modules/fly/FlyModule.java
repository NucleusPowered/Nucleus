/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.fly;

import com.google.common.collect.ImmutableMap;
import io.github.nucleuspowered.nucleus.modules.fly.config.FlyConfig;
import io.github.nucleuspowered.nucleus.modules.fly.config.FlyConfigAdapter;
import io.github.nucleuspowered.nucleus.quickstart.module.ConfigurableModule;
import io.github.nucleuspowered.nucleus.services.INucleusServiceCollection;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.text.placeholder.PlaceholderParser;
import uk.co.drnaylor.quickstart.annotations.ModuleData;
import uk.co.drnaylor.quickstart.holders.DiscoveryModuleHolder;

import java.util.Map;
import java.util.function.Supplier;

import javax.inject.Inject;

@ModuleData(id = "fly", name = "Fly")
public class FlyModule extends ConfigurableModule<FlyConfig, FlyConfigAdapter> {

    @Inject
    public FlyModule(Supplier<DiscoveryModuleHolder<?, ?>> moduleHolder, INucleusServiceCollection collection) {
        super(moduleHolder, collection);
    }

    @Override
    public FlyConfigAdapter createAdapter() {
        return new FlyConfigAdapter();
    }

    @Override
    protected Map<String, PlaceholderParser> tokensToRegister() {
        return ImmutableMap.<String, PlaceholderParser>builder()
                .put("flying",
                        PlaceholderParser.builder()
                            .plugin(this.serviceCollection.pluginContainer())
                            .id("flying")
                            .name("Nucleus Flying Indicator Token")
                            .parser(p -> {
                                if (p.getAssociatedObject().filter(x -> x instanceof Player).flatMap(x -> ((Player) x).get(Keys.IS_FLYING))
                                        .orElse(false)) {
                                    return Text.of(TextColors.GRAY, "[Flying]");
                                }
                                return Text.EMPTY;
                            })
                            .build())
                .build();
    }
}
