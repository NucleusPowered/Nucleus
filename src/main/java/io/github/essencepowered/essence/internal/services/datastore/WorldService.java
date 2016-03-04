/*
 * This file is part of Essence, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.essencepowered.essence.internal.services.datastore;

import com.google.common.base.Preconditions;
import com.google.common.reflect.TypeToken;
import io.github.essencepowered.essence.Essence;
import io.github.essencepowered.essence.api.data.EssenceWorld;
import io.github.essencepowered.essence.config.serialisers.WorldConfig;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.SimpleConfigurationNode;
import ninja.leaping.configurate.gson.GsonConfigurationLoader;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import org.spongepowered.api.world.World;

import java.io.IOException;
import java.nio.file.Path;
import java.util.UUID;

public class WorldService implements EssenceWorld {

    private final Essence plugin;

    // TODO: Think about whether we need the world object, or just the UUID.
    private final World world;
    private final GsonConfigurationLoader loader;

    private WorldConfig config = null;

    public WorldService(Essence plugin, Path worldPath, World world) throws IOException, ObjectMappingException {
        Preconditions.checkNotNull(world);
        Preconditions.checkNotNull(worldPath);
        Preconditions.checkNotNull(plugin);
        this.plugin = plugin;
        this.world = world;
        this.loader = GsonConfigurationLoader.builder().setPath(worldPath).build();
        load();
    }

    private void load() throws IOException, ObjectMappingException {
        ConfigurationNode cn = loader.load();
        config = cn.getValue(TypeToken.of(WorldConfig.class), new WorldConfig());
    }

    public void save() throws IOException, ObjectMappingException {
        ConfigurationNode cn = SimpleConfigurationNode.root();
        cn.setValue(TypeToken.of(WorldConfig.class), config);
        loader.save(cn);
    }

    public World getWorld() {
        return world;
    }

    public UUID getUniqueID() {
        return world.getUniqueId();
    }

    @Override
    public boolean isLockWeather() {
        return config.isLockWeather();
    }

    @Override
    public void setLockWeather(boolean lockWeather) {
        config.setLockWeather(lockWeather);
    }
}
