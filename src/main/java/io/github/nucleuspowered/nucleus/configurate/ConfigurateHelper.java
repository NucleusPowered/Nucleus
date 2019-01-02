/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.configurate;

import com.flowpowered.math.vector.Vector3d;
import com.google.common.reflect.TypeToken;
import io.github.nucleuspowered.neutrino.objectmapper.NeutrinoObjectMapperFactory;
import io.github.nucleuspowered.neutrino.typeserialisers.ByteArrayTypeSerialiser;
import io.github.nucleuspowered.neutrino.typeserialisers.IntArrayTypeSerialiser;
import io.github.nucleuspowered.neutrino.typeserialisers.PatternTypeSerialiser;
import io.github.nucleuspowered.neutrino.typeserialisers.SetTypeSerialiser;
import io.github.nucleuspowered.neutrino.typeserialisers.ShortArrayTypeSerialiser;
import io.github.nucleuspowered.nucleus.Nucleus;
import io.github.nucleuspowered.nucleus.configurate.typeserialisers.InstantTypeSerialiser;
import io.github.nucleuspowered.nucleus.configurate.typeserialisers.NucleusItemStackSnapshotSerialiser;
import io.github.nucleuspowered.nucleus.configurate.typeserialisers.NucleusTextTemplateTypeSerialiser;
import io.github.nucleuspowered.nucleus.configurate.typeserialisers.Vector3dTypeSerialiser;
import io.github.nucleuspowered.nucleus.configurate.wrappers.NucleusItemStackSnapshot;
import io.github.nucleuspowered.nucleus.internal.text.NucleusTextTemplateImpl;
import io.github.nucleuspowered.nucleus.storage.DataObjectTranslator;
import io.github.nucleuspowered.storage.dataobjects.AbstractConfigurateBackedDataObject;
import ninja.leaping.configurate.ConfigurationOptions;
import ninja.leaping.configurate.objectmapping.serialize.TypeSerializerCollection;

import java.time.Instant;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ConfigurateHelper {

    private ConfigurateHelper() {}

    private static final TypeToken<AbstractConfigurateBackedDataObject> ABSTRACT_DATA_OBJECT_TYPE_TOKEN = TypeToken.of(
            AbstractConfigurateBackedDataObject.class);

    private static TypeSerializerCollection typeSerializerCollection = null;
    private static final NeutrinoObjectMapperFactory objectMapperFactory;
    private static final Pattern commentPattern = Pattern.compile("^(loc:)?(?<key>([a-zA-Z0-9_-]+\\.?)+)$");

    static {
        objectMapperFactory = NeutrinoObjectMapperFactory.builder()
            .setCommentProcessor(setting -> {
                String comment = setting.comment();
                if (comment.contains(".") && !comment.contains(" ")) {
                    Matcher matcher = commentPattern.matcher(comment);

                    if (matcher.matches()) {
                        return Nucleus.getNucleus().getMessageProvider().getMessageWithFormat(matcher.group("key"));
                    }
                }

                return comment;
        }).build(true);
    }

    /**
     * Set NucleusPlugin specific options on the {@link ConfigurationOptions}
     *
     * @param options The {@link ConfigurationOptions} to alter.
     * @return The {@link ConfigurationOptions}, for easier inline use of this function.
     */
    public static ConfigurationOptions setOptions(ConfigurationOptions options) {
        TypeSerializerCollection tsc = getNucleusTypeSerialiserCollection();

        // Allows us to use localised comments and @ProcessSetting annotations
        return options.setSerializers(tsc).setObjectMapperFactory(objectMapperFactory);
    }

    private static TypeSerializerCollection getNucleusTypeSerialiserCollection() {
        if (typeSerializerCollection != null) {
            return typeSerializerCollection;
        }

        typeSerializerCollection = ConfigurationOptions.defaults().getSerializers().newChild();

        // Custom type serialisers for Nucleus
        typeSerializerCollection.registerType(TypeToken.of(Vector3d.class), new Vector3dTypeSerialiser());
        typeSerializerCollection.registerType(TypeToken.of(NucleusItemStackSnapshot.class), new NucleusItemStackSnapshotSerialiser());
        typeSerializerCollection.registerType(TypeToken.of(Pattern.class), new PatternTypeSerialiser());
        typeSerializerCollection.registerType(TypeToken.of(NucleusTextTemplateImpl.class), new NucleusTextTemplateTypeSerialiser());
        typeSerializerCollection.registerPredicate(
                typeToken -> Set.class.isAssignableFrom(typeToken.getRawType()),
                new SetTypeSerialiser()
        );

        typeSerializerCollection.registerType(new TypeToken<byte[]>(){}, new ByteArrayTypeSerialiser());
        typeSerializerCollection.registerType(new TypeToken<short[]>(){}, new ShortArrayTypeSerialiser());
        typeSerializerCollection.registerType(new TypeToken<int[]>(){}, new IntArrayTypeSerialiser());
        typeSerializerCollection.registerType(TypeToken.of(Instant.class), new InstantTypeSerialiser());

        typeSerializerCollection.registerPredicate(x -> x.isSubtypeOf(ABSTRACT_DATA_OBJECT_TYPE_TOKEN), DataObjectTranslator.INSTANCE);

        return typeSerializerCollection;
    }
}
