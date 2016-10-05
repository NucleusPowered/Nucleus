/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.dataservices.dataproviders;

import com.google.common.reflect.TypeToken;
import io.github.nucleuspowered.nucleus.configurate.ConfigurateHelper;
import ninja.leaping.configurate.commented.SimpleCommentedConfigurationNode;
import ninja.leaping.configurate.loader.ConfigurationLoader;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Supplier;

public class ConfigurateDataProvider<T> implements DataProvider<T> {

    private final TypeToken<T> typeToken;
    private final ConfigurationLoader<?> loader;
    private final Supplier<T> defaultSupplier;
    private final Path file;

    public ConfigurateDataProvider(TypeToken<T> type, ConfigurationLoader<?> loader, Path file) {
        this(type, loader, () -> {
            try {
                return (T)type.getRawType().newInstance();
            } catch (InstantiationException | IllegalAccessException e) {
                e.printStackTrace();
                return null;
            }
        }, file);
    }

    public ConfigurateDataProvider(TypeToken<T> type, ConfigurationLoader<?> loader, Supplier<T> defaultSupplier, Path file) {
        this.typeToken = type;
        this.loader = loader;
        this.defaultSupplier = defaultSupplier;
        this.file = file;
    }

    @Override
    public T load() throws Exception {
        return loader.load(ConfigurateHelper.setOptions(loader.getDefaultOptions())).getValue(typeToken, defaultSupplier);
    }

    @Override
    public void save(T info) throws Exception {
        loader.save(SimpleCommentedConfigurationNode.root(ConfigurateHelper.setOptions(loader.getDefaultOptions())).setValue(typeToken, info));
    }

    @Override
    public void delete() throws Exception {
        Files.delete(file);
    }
}
