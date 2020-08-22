/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.freezeplayer;

import com.google.common.collect.ImmutableMap;
import io.github.nucleuspowered.nucleus.modules.freezeplayer.services.FreezePlayerService;
import io.github.nucleuspowered.nucleus.quickstart.module.StandardModule;
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

@ModuleData(id = "freeze-subject", name = "Freeze Player")
public class FreezePlayerModule extends StandardModule {

    @Inject
    public FreezePlayerModule(Supplier<DiscoveryModuleHolder<?, ?>> moduleHolder,
            INucleusServiceCollection collection) {
        super(moduleHolder, collection);
    }

    @Override
    protected Map<String, PlaceholderParser> tokensToRegister() {
        return ImmutableMap.<String, PlaceholderParser>builder()
                .put("frozen",
                        PlaceholderParser.builder()
                                .plugin(this.serviceCollection.pluginContainer())
                                .id("frozen")
                                .name("Nucleus Frozen Indicator Token")
                                .parser(p -> {
                                    if (p.getAssociatedObject().filter(x -> x instanceof User)
                                            .map(x -> this.serviceCollection.getServiceUnchecked(FreezePlayerService.class).isFrozen((User) x))
                                            .orElse(false)) {
                                        return Text.of(TextColors.GRAY, "[Frozen]");
                                    }
                                    return Text.EMPTY;
                                }).build()).build();
    }
}
