/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.configurate;

import com.flowpowered.math.vector.Vector3d;
import com.google.common.reflect.TypeToken;
import io.github.nucleuspowered.nucleus.configurate.objectmapper.NucleusObjectMapperFactory;
import io.github.nucleuspowered.nucleus.configurate.typeserialisers.ConfigurationNodeTypeSerialiser;
import io.github.nucleuspowered.nucleus.configurate.typeserialisers.ItemStackSnapshotSerialiser;
import io.github.nucleuspowered.nucleus.configurate.typeserialisers.SetTypeSerialiser;
import io.github.nucleuspowered.nucleus.configurate.typeserialisers.Vector3dTypeSerialiser;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.ConfigurationOptions;
import ninja.leaping.configurate.objectmapping.serialize.TypeSerializerCollection;
import org.spongepowered.api.GameState;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.item.inventory.ItemStackSnapshot;

import java.util.Set;

public class ConfigurateHelper {

    private ConfigurateHelper() {}

    private static TypeSerializerCollection typeSerializerCollection = null;

    /**
     * Set NucleusPlugin specific options on the {@link ConfigurationOptions}
     *
     * @param options The {@link ConfigurationOptions} to alter.
     * @return The {@link ConfigurationOptions}, for easier inline use of this function.
     */
    public static ConfigurationOptions setOptions(ConfigurationOptions options) {
        TypeSerializerCollection tsc = getNucleusTypeSerialiserCollection(options);

        // Allows us to use localised comments and @ProcessSetting annotations
        return options.setSerializers(tsc).setObjectMapperFactory(NucleusObjectMapperFactory.getInstance());
    }

    public static TypeSerializerCollection getNucleusTypeSerialiserCollection() {
        return getNucleusTypeSerialiserCollection(ConfigurationOptions.defaults());
    }

    public static TypeSerializerCollection getNucleusTypeSerialiserCollection(ConfigurationOptions options) {
        if (typeSerializerCollection != null) {
            return typeSerializerCollection;
        }

        TypeSerializerCollection tsc = options.getSerializers();

        // Custom type serialisers for NucleusPlugin
        tsc.registerType(TypeToken.of(Vector3d.class), new Vector3dTypeSerialiser());
        tsc.registerType(TypeToken.of(ItemStackSnapshot.class), new ItemStackSnapshotSerialiser());
        tsc.registerType(TypeToken.of(ConfigurationNode.class), new ConfigurationNodeTypeSerialiser());
        tsc.registerPredicate(
                typeToken -> Set.class.isAssignableFrom(typeToken.getRawType()),
                new SetTypeSerialiser()
        );

        if (Sponge.getGame().getState() == GameState.SERVER_STARTED) {
            typeSerializerCollection = tsc;
        }

        return tsc;
    }
}
