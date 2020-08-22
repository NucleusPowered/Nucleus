/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.playerinfo.services;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import io.github.nucleuspowered.nucleus.api.module.playerinfo.NucleusSeenService;
import io.github.nucleuspowered.nucleus.scaffold.service.ServiceBase;
import io.github.nucleuspowered.nucleus.scaffold.service.annotations.APIService;
import io.github.nucleuspowered.nucleus.services.INucleusServiceCollection;
import io.github.nucleuspowered.nucleus.services.interfaces.IPlayerInformationService;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.util.annotation.NonnullByDefault;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Predicate;

import javax.annotation.Nonnull;
import javax.inject.Inject;

@APIService(NucleusSeenService.class)
@NonnullByDefault
public class SeenHandler implements NucleusSeenService, ServiceBase {

    private final INucleusServiceCollection serviceCollection;
    private final Map<String, List<SeenInformationProvider>> pluginInformationProviders = Maps.newTreeMap();

    @Inject
    public SeenHandler(INucleusServiceCollection serviceCollection) {
        this.serviceCollection = serviceCollection;
    }

    @Override
    public void register(@Nonnull PluginContainer plugin, @Nonnull SeenInformationProvider seenInformationProvider) throws IllegalArgumentException {
        Preconditions.checkNotNull(plugin);
        Preconditions.checkNotNull(seenInformationProvider);

        Plugin pl = plugin.getClass().getAnnotation(Plugin.class);
        Preconditions.checkArgument(pl != null, this.serviceCollection.messageProvider().getMessage("seen.error.requireplugin"));

        String name = pl.name();
        List<SeenInformationProvider> providers;
        if (this.pluginInformationProviders.containsKey(name)) {
            providers = this.pluginInformationProviders.get(name);
        } else {
            providers = Lists.newArrayList();
            this.pluginInformationProviders.put(name, providers);
        }

        providers.add(seenInformationProvider);
    }

    @Override
    public void register(PluginContainer plugin, Predicate<CommandSource> permissionCheck, BiFunction<CommandSource, User, Collection<Text>> informationGetter)
        throws IllegalArgumentException {
        register(plugin, new SeenInformationProvider() {
            @Override public boolean hasPermission(@Nonnull CommandSource source, @Nonnull User user) {
                return permissionCheck.test(source);
            }

            @Override public Collection<Text> getInformation(@Nonnull CommandSource source, @Nonnull User user) {
                return informationGetter.apply(source, user);
            }
        });
    }

    public List<Text> getText(final CommandSource requester, final User user) {
        List<Text> information = Lists.newArrayList();

        Collection<IPlayerInformationService.Provider> providers = this.serviceCollection.playerInformationService().getProviders();
        for (IPlayerInformationService.Provider provider : providers) {
            provider.get(user, requester, this.serviceCollection).ifPresent(information::add);
        }

        for (Map.Entry<String, List<SeenInformationProvider>> entry : this.pluginInformationProviders.entrySet()) {
            entry.getValue().stream().filter(sip -> sip.hasPermission(requester, user)).forEach(sip -> {
                Collection<Text> input = sip.getInformation(requester, user);
                if (input != null && !input.isEmpty()) {
                    if (information.isEmpty()) {
                        information.add(Text.EMPTY);
                        information.add(Text.of("-----"));
                        information.add(this.serviceCollection.messageProvider().getMessageFor(requester, "seen.header.plugins"));
                        information.add(Text.of("-----"));
                    }

                    information.add(Text.EMPTY);
                    information.add(Text.of(TextColors.AQUA, entry.getKey() + ":"));
                    information.addAll(input);
                }
            });
        }

        return information;
    }
}
