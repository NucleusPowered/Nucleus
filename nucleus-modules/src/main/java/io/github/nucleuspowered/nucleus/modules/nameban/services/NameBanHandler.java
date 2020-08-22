/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.nameban.services;

import com.google.common.base.Preconditions;
import com.google.common.reflect.TypeToken;
import com.google.inject.Inject;
import io.github.nucleuspowered.nucleus.Util;
import io.github.nucleuspowered.nucleus.api.module.nameban.NucleusNameBanService;
import io.github.nucleuspowered.nucleus.api.module.nameban.exception.NameBanException;
import io.github.nucleuspowered.nucleus.modules.nameban.events.NameBanEvent;
import io.github.nucleuspowered.nucleus.scaffold.service.ServiceBase;
import io.github.nucleuspowered.nucleus.scaffold.service.annotations.APIService;
import io.github.nucleuspowered.nucleus.services.INucleusServiceCollection;
import io.github.nucleuspowered.nucleus.services.interfaces.IConfigurateHelper;
import io.github.nucleuspowered.nucleus.services.interfaces.IReloadableService;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.ConfigurationOptions;
import ninja.leaping.configurate.SimpleConfigurationNode;
import ninja.leaping.configurate.gson.GsonConfigurationLoader;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.serializer.TextSerializers;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

@APIService(NucleusNameBanService.class)
public class NameBanHandler implements NucleusNameBanService, ServiceBase, IReloadableService.DataLocationReloadable {

    private final Map<String, String> entries = new HashMap<>();
    private final Supplier<Path> dataPath;
    private final IConfigurateHelper configurateOptions;
    private final PluginContainer pluginContainer;
    private Path currentPath;

    @Inject
    public NameBanHandler(INucleusServiceCollection serviceCollection) {
        this.pluginContainer = serviceCollection.pluginContainer();
        this.dataPath = serviceCollection.dataDir();
        this.configurateOptions = serviceCollection.configurateHelper();
    }

    @Override public void addName(String name, String reason, Cause cause) throws NameBanException {
        if (Util.usernameRegex.matcher(name).matches()) {
            this.entries.put(name.toLowerCase(), reason);
            Sponge.getEventManager().post(new NameBanEvent.Banned(name, reason, cause));
            Sponge.getServer().getOnlinePlayers().stream().filter(x -> x.getName().equalsIgnoreCase(name)).findFirst()
                    .ifPresent(x -> x.kick(TextSerializers.FORMATTING_CODE.deserialize(reason)));
            Task.builder().execute(this::save).submit(this.pluginContainer);
        }

        throw new NameBanException(
                Text.of("That is not a valid username."), NameBanException.Reason.DISALLOWED_NAME);
    }

    @Override public Optional<String> getReasonForBan(String name) {
        Preconditions.checkNotNull(name);
        return Optional.ofNullable(this.entries.get(name.toLowerCase()));
    }

    @Override public void removeName(String name, Cause cause) throws NameBanException {
        if (Util.usernameRegex.matcher(name).matches()) {
            Optional<String> reason = getReasonForBan(name);
            if (reason.isPresent() && this.entries.remove(name.toLowerCase()) != null) {
                Sponge.getEventManager().post(new NameBanEvent.Unbanned(name, reason.get(), cause));
            }

            throw new NameBanException(Text.of("Entry does not exist."), NameBanException.Reason.DOES_NOT_EXIST);
        }

        throw new NameBanException(Text.of("That is not a valid username."), NameBanException.Reason.DISALLOWED_NAME);
    }

    public void load() {
        try {
            ConfigurationNode node = createLoader().load();
            // Lowercase the keys.
            this.entries.clear();
            node.getChildrenMap()
                    .forEach((k, v) -> {
                        String lower = k.toString().toLowerCase();
                        if (!k.equals(lower)) {
                            entries.put(lower, v.getString());
                        }
                    });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void save() {
        try {
            ConfigurationNode node = SimpleConfigurationNode.root(this.configurateOptions.setOptions(ConfigurationOptions.defaults()));
            node.setValue(new TypeToken<Map<String, String>>() {}, this.entries);
            createLoader().save(node);
        } catch (IOException | ObjectMappingException e) {
            e.printStackTrace();
        }
    }

    @Override public void onDataFileLocationChange(INucleusServiceCollection serviceCollection) {
        // The path changes
        this.currentPath = this.dataPath.get().resolve("namebans.conf");
        load();
    }

    private GsonConfigurationLoader createLoader() {
        return GsonConfigurationLoader.builder()
                .setPath(this.currentPath)
                .setDefaultOptions(this.configurateOptions.setOptions(ConfigurationOptions.defaults()))
                .build();
    }
}
