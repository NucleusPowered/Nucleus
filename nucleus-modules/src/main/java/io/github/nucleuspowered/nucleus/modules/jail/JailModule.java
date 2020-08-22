/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.jail;

import com.google.common.collect.ImmutableMap;
import io.github.nucleuspowered.nucleus.modules.jail.config.JailConfig;
import io.github.nucleuspowered.nucleus.modules.jail.config.JailConfigAdapter;
import io.github.nucleuspowered.nucleus.modules.jail.services.JailHandler;
import io.github.nucleuspowered.nucleus.quickstart.module.ConfigurableModule;
import io.github.nucleuspowered.nucleus.services.INucleusServiceCollection;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.text.placeholder.PlaceholderParser;
import uk.co.drnaylor.quickstart.annotations.ModuleData;
import uk.co.drnaylor.quickstart.holders.DiscoveryModuleHolder;

import java.util.Map;
import java.util.function.Supplier;

import javax.inject.Inject;

@ModuleData(id = JailModule.ID, name = "Jail")
public class JailModule extends ConfigurableModule<JailConfig, JailConfigAdapter> {

    public static final String ID = "jail";

    @Inject
    public JailModule(Supplier<DiscoveryModuleHolder<?, ?>> moduleHolder, INucleusServiceCollection collection) {
        super(moduleHolder, collection);
    }

    @Override
    public JailConfigAdapter createAdapter() {
        return new JailConfigAdapter();
    }

    @Override
    protected Map<String, PlaceholderParser> tokensToRegister() {
        return ImmutableMap.<String, PlaceholderParser>builder()
                .put("jailed",
                        PlaceholderParser.builder()
                                .plugin(this.serviceCollection.pluginContainer())
                                .id("jailed")
                                .name("Nucleus Jailed Indicator Token")
                                .parser(p -> {
                                    if (p.getAssociatedObject().filter(x -> x instanceof User)
                                            .map(x -> this.serviceCollection.getServiceUnchecked(JailHandler.class).isPlayerJailed((User) x))
                                            .orElse(false)) {
                                        return Text.of(TextColors.GRAY, "[Jailed]");
                                    }
                                    return Text.EMPTY;
                                }).build())
                .put("jail", PlaceholderParser.builder()
                        .plugin(this.serviceCollection.pluginContainer())
                        .id("jail")
                        .name("Nucleus Jail Name Token")
                        .parser(placeholder -> {
                    if (placeholder.getAssociatedObject().filter(x -> x instanceof Player).isPresent()) {
                        return serviceCollection.getServiceUnchecked(JailHandler.class)
                                .getPlayerJailData((Player) placeholder.getAssociatedObject().get())
                                .<Text>map(x -> Text.of(x.getJailName()))
                                .orElse(Text.EMPTY);
                    }

                    return Text.EMPTY;
                }).build()).build();
    }
}
