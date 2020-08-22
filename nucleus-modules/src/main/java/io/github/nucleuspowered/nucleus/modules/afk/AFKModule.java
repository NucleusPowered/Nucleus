/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.afk;

import com.google.common.collect.ImmutableMap;
import io.github.nucleuspowered.nucleus.modules.afk.config.AFKConfig;
import io.github.nucleuspowered.nucleus.modules.afk.config.AFKConfigAdapter;
import io.github.nucleuspowered.nucleus.modules.afk.services.AFKHandler;
import io.github.nucleuspowered.nucleus.quickstart.module.ConfigurableModule;
import io.github.nucleuspowered.nucleus.services.INucleusServiceCollection;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.text.placeholder.PlaceholderParser;
import uk.co.drnaylor.quickstart.annotations.ModuleData;
import uk.co.drnaylor.quickstart.holders.DiscoveryModuleHolder;

import java.util.Map;
import java.util.function.Supplier;

import javax.inject.Inject;

@ModuleData(id = AFKModule.ID, name = "AFK")
public class AFKModule extends ConfigurableModule<AFKConfig, AFKConfigAdapter> {

    public static final String ID = "afk";

    @Inject
    public AFKModule(Supplier<DiscoveryModuleHolder<?, ?>> moduleHolder, INucleusServiceCollection collection) {
        super(moduleHolder, collection);
    }

    @Override
    public AFKConfigAdapter createAdapter() {
        return new AFKConfigAdapter();
    }

    @Override
    protected Map<String, PlaceholderParser> tokensToRegister() {
        return ImmutableMap.<String, PlaceholderParser>builder()
                .put("afk",
                        PlaceholderParser.builder()
                                .plugin(this.serviceCollection.pluginContainer())
                                .id("afk")
                                .name("Nucleus Muted Indicator Token")
                                .parser(p -> {
                                    if (p.getAssociatedObject().filter(x -> x instanceof Player)
                                            .map(x -> this.serviceCollection.getServiceUnchecked(AFKHandler.class).isAFK((Player) x))
                                            .orElse(false)) {
                                        return Text.of(TextColors.GRAY, "[AFK]");
                                    }
                                    return Text.EMPTY;
                                }).build()).build();
    }
}
