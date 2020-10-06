/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.services.impl.configurate;

import com.flowpowered.math.vector.Vector3d;
import com.google.common.reflect.TypeToken;
import com.google.inject.Injector;
import io.github.nucleuspowered.neutrino.objectmapper.NeutrinoObjectMapperFactory;
import io.github.nucleuspowered.neutrino.settingprocessor.SettingProcessor;
import io.github.nucleuspowered.neutrino.typeserialisers.ByteArrayTypeSerialiser;
import io.github.nucleuspowered.neutrino.typeserialisers.IntArrayTypeSerialiser;
import io.github.nucleuspowered.neutrino.typeserialisers.PatternTypeSerialiser;
import io.github.nucleuspowered.neutrino.typeserialisers.SetTypeSerialiser;
import io.github.nucleuspowered.neutrino.typeserialisers.ShortArrayTypeSerialiser;
import io.github.nucleuspowered.neutrino.util.ClassConstructor;
import io.github.nucleuspowered.nucleus.configurate.typeserialisers.InstantTypeSerialiser;
import io.github.nucleuspowered.nucleus.configurate.typeserialisers.LocaleSerialiser;
import io.github.nucleuspowered.nucleus.configurate.typeserialisers.MailMessageSerialiser;
import io.github.nucleuspowered.nucleus.configurate.typeserialisers.NamedLocationSerialiser;
import io.github.nucleuspowered.nucleus.configurate.typeserialisers.NucleusItemStackSnapshotSerialiser;
import io.github.nucleuspowered.nucleus.configurate.typeserialisers.NucleusTextTemplateTypeSerialiser;
import io.github.nucleuspowered.nucleus.configurate.typeserialisers.Vector3dTypeSerialiser;
import io.github.nucleuspowered.nucleus.configurate.typeserialisers.WarpCategorySerialiser;
import io.github.nucleuspowered.nucleus.configurate.typeserialisers.WarpSerialiser;
import io.github.nucleuspowered.nucleus.configurate.wrappers.NucleusItemStackSnapshot;
import io.github.nucleuspowered.nucleus.services.INucleusServiceCollection;
import io.github.nucleuspowered.nucleus.services.impl.storage.DataObjectTranslator;
import io.github.nucleuspowered.nucleus.services.impl.storage.dataobjects.configurate.AbstractConfigurateBackedDataObject;
import io.github.nucleuspowered.nucleus.services.impl.texttemplatefactory.NucleusTextTemplateImpl;
import io.github.nucleuspowered.nucleus.services.interfaces.IConfigurateHelper;
import io.github.nucleuspowered.nucleus.services.interfaces.IMessageProviderService;
import io.github.nucleuspowered.nucleus.util.PrettyPrinter;
import io.github.nucleuspowered.nucleus.util.TypeTokens;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.ConfigurationOptions;
import ninja.leaping.configurate.objectmapping.ObjectMapper;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;
import ninja.leaping.configurate.objectmapping.serialize.TypeSerializer;
import ninja.leaping.configurate.objectmapping.serialize.TypeSerializerCollection;
import ninja.leaping.configurate.objectmapping.serialize.TypeSerializers;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.event.Level;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.time.Instant;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class ConfigurateHelper implements IConfigurateHelper {

    private final IMessageProviderService messageProvider;

    private static final TypeToken<AbstractConfigurateBackedDataObject> ABSTRACT_DATA_OBJECT_TYPE_TOKEN = TypeToken.of(
            AbstractConfigurateBackedDataObject.class);

    private final TypeSerializerCollection typeSerializerCollection;
    private final NeutrinoObjectMapperFactory objectMapperFactory;
    private final Pattern commentPattern = Pattern.compile("^(loc:)?(?<key>([a-zA-Z0-9_-]+\\.?)+)$");

    @Inject
    public ConfigurateHelper(INucleusServiceCollection serviceCollection) {
        this.messageProvider = serviceCollection.messageProvider();
        this.objectMapperFactory = NeutrinoObjectMapperFactory.builder()
                .setCommentProcessor(setting -> {
                    String comment = setting.comment();
                    if (comment.contains(".") && !comment.contains(" ")) {
                        Matcher matcher = this.commentPattern.matcher(comment);

                        if (matcher.matches()) {
                            return this.messageProvider.getMessageString(matcher.group("key"));
                        }
                    }

                    return comment;
                })
                .setSettingProcessorClassConstructor(new SettingProcessorConstructor(serviceCollection.injector()))
                .build(true);
        this.typeSerializerCollection = setup(serviceCollection);
    }

    /**
     * Set NucleusPlugin specific options on the {@link ConfigurationOptions}
     *
     * @param options The {@link ConfigurationOptions} to alter.
     * @return The {@link ConfigurationOptions}, for easier inline use of this function.
     */
    @Override public ConfigurationOptions setOptions(ConfigurationOptions options) {
        // Allows us to use localised comments and @ProcessSetting annotations
        return options.withSerializers(this.typeSerializerCollection).setObjectMapperFactory(this.objectMapperFactory);
    }

    private static TypeSerializerCollection setup(INucleusServiceCollection serviceCollection) {
        TypeSerializerCollection typeSerializerCollection = ConfigurationOptions.defaults().getSerializers().newChild();

        // Custom type serialisers for Nucleus
        typeSerializerCollection.register(TypeToken.of(Vector3d.class), new Vector3dTypeSerialiser());
        typeSerializerCollection.register(TypeToken.of(NucleusItemStackSnapshot.class), new NucleusItemStackSnapshotSerialiser(serviceCollection));
        typeSerializerCollection.register(TypeToken.of(Pattern.class), new PatternTypeSerialiser());
        typeSerializerCollection.register(TypeToken.of(NucleusTextTemplateImpl.class),
                new NucleusTextTemplateTypeSerialiser(serviceCollection.textTemplateFactory()));
        typeSerializerCollection.register(
                typeToken -> Set.class.isAssignableFrom(typeToken.getRawType()),
                new SetTypeSerialiser()
        );

        typeSerializerCollection.register(new TypeToken<byte[]>(){}, new ByteArrayTypeSerialiser());
        typeSerializerCollection.register(new TypeToken<short[]>(){}, new ShortArrayTypeSerialiser());
        typeSerializerCollection.register(new TypeToken<int[]>(){}, new IntArrayTypeSerialiser());
        typeSerializerCollection.register(TypeToken.of(Instant.class), new InstantTypeSerialiser());

        typeSerializerCollection.register(x -> x.isSubtypeOf(ABSTRACT_DATA_OBJECT_TYPE_TOKEN), DataObjectTranslator.INSTANCE);
        typeSerializerCollection.register(TypeTokens.WARP, new WarpSerialiser(serviceCollection.logger()));
        typeSerializerCollection.register(TypeTokens.WARP_CATEGORY, new WarpCategorySerialiser());
        typeSerializerCollection.register(TypeTokens.NAMEDLOCATION, new NamedLocationSerialiser(serviceCollection.logger()));
        typeSerializerCollection.register(TypeTokens.MAIL_MESSAGE, new MailMessageSerialiser());
        typeSerializerCollection.register(TypeTokens.LOCALE, new LocaleSerialiser());

        fixGrossHacks(serviceCollection.logger(), typeSerializerCollection);

        return typeSerializerCollection;
    }

    private static final String CWBAH_OBJECT_MAPPER = "cz.neumimto.config.blackjack.and.hookers.NotSoStupidObjectMapper";
    private static final String CWBAH_TYPE_SERIALIZER = "cz.neumimto.config.blackjack.and.hookers.ClassTypeNodeSerializer";
    private static final String ANNOTATED_OBJECT_SERIALIZER = "ninja.leaping.configurate.objectmapping.serialize.AnnotatedObjectSerializer";

    /**
     * "Fix" anything that screws around with Configurate
     *
     * @param logger The logger
     * @param collection The TSC to add anything to if needed.
     */
    @SuppressWarnings("unchecked")
    private static void fixGrossHacks(Logger logger, TypeSerializerCollection collection) {
        try {
            // Configurate with Blackjack and Hookers breaks Nucleus by forcing everyone to use his object mapper.
            // We need to undo that for us.
            Class.forName(CWBAH_OBJECT_MAPPER);

            // Is his gross hack in there?
            TypeSerializer<?> serializer = TypeSerializers.getDefaultSerializers().get(TypeToken.of(DummyConfigSerializable.class));
            if (serializer.getClass().getName().equalsIgnoreCase(CWBAH_TYPE_SERIALIZER)) {
                new PrettyPrinter(100).add("ConfigurateButWithBlackjackAndHookers has been detected.")
                        .hr()
                        .add("Some plugins, such as NT-RPG, use a gross hack to inject their object mapper into")
                        .add("Configurate instead of using the supported way. This system is known as")
                        .add("ConfigurateButWithBlackjackAndHookers. Because of the hack, Nucleus cannot use its")
                        .add("own extensions which use standard Configurate API.")
                        .add()
                        .add("We will attempt to work around this by restoring the Configurate code for our type")
                        .add("serialisers only - this will only work for Nucleus. Other plugins may be broken by")
                        .add("ConfigurateButWithBlackjackAndHookers.")
                        .add()
                        .add("We have reported this issue to the author.")
                        .log(logger, Level.WARN);

                // and, just in case...
                // try to get the TS from Configurate
                try {
                    Class<?> clazz = Class.forName(ANNOTATED_OBJECT_SERIALIZER);
                    Constructor<?> ctor = clazz.getDeclaredConstructor();
                    ctor.setAccessible(true);
                    TypeSerializer<Object> ts = (TypeSerializer<Object>) ctor.newInstance();
                    collection.registerPredicate(input -> input.getRawType().isAnnotationPresent(ConfigSerializable.class), ts);
                } catch (Exception e) {
                    logger.error("Could not get standard Configurate serialiser. Using our own.");
                    collection.registerPredicate(input -> input.getRawType().isAnnotationPresent(ConfigSerializable.class),
                            new AnnotatedObjectSerializer());
                }
            }
        } catch (ClassNotFoundException e) {
            // ignored, it's okay!
        }
    }

    /**
     * Mostly taken from
     * https://github.com/SpongePowered/Configurate/blob/7e82385eb8a7f66eeb9cd6957dfe9470f3760198/configurate-core/src/main/java/ninja/leaping/configurate/objectmapping/serialize/AnnotatedObjectSerializer.java#L1
     *
     * Configurate
     * Copyright (C) zml and Configurate contributors
     *
     * Licensed under the Apache License, Version 2.0 (the "License");
     * you may not use this file except in compliance with the License.
     * You may obtain a copy of the License at
     *
     *    http://www.apache.org/licenses/LICENSE-2.0
     *
     * Unless required by applicable law or agreed to in writing, software
     * distributed under the License is distributed on an "AS IS" BASIS,
     * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
     * See the License for the specific language governing permissions and
     * limitations under the License.
     */
    private static class AnnotatedObjectSerializer implements TypeSerializer<Object> {
        public static final String CLASS_KEY = "__class__";

        @Override
        public Object deserialize(@NonNull TypeToken<?> type, @NonNull ConfigurationNode value) throws ObjectMappingException {
            TypeToken<?> clazz = getInstantiableType(type, value.getNode(CLASS_KEY).getString());
            // TODO: Fix for 3.7
            return value.getOptions().getObjectMapperFactory().getMapper(clazz.getRawType()).bindToNew().populate(value);
        }

        private TypeToken<?> getInstantiableType(TypeToken<?> type, String configuredName) throws ObjectMappingException {
            TypeToken<?> retClass;
            Class<?> rawType = type.getRawType();
            if (rawType.isInterface() || Modifier.isAbstract(rawType.getModifiers())) {
                if (configuredName == null) {
                    throw new ObjectMappingException("No available configured type for instances of " + type);
                } else {
                    try {
                        retClass = TypeToken.of(Class.forName(configuredName));
                    } catch (ClassNotFoundException e) {
                        throw new ObjectMappingException("Unknown class of object " + configuredName, e);
                    }
                    if (!retClass.isSubtypeOf(type)) {
                        throw new ObjectMappingException("Configured type " + configuredName + " does not extend "
                                + rawType.getCanonicalName());
                    }
                }
            } else {
                retClass = type;
            }
            return retClass;
        }

        @Override
        @SuppressWarnings("unchecked")
        public void serialize(@NonNull TypeToken<?> type, @Nullable Object obj, @NonNull ConfigurationNode value) throws ObjectMappingException {
            if (obj == null) {
                ConfigurationNode clazz = value.getNode(CLASS_KEY);
                value.setValue(null);
                if (!clazz.isVirtual()) {
                    value.getNode(CLASS_KEY).setValue(clazz);
                }
                return;
            }
            Class<?> rawType = type.getRawType();
            ObjectMapper<?> mapper;
            if (rawType.isInterface() || Modifier.isAbstract(rawType.getModifiers())) {
                // serialize obj's concrete type rather than the interface/abstract class
                value.getNode(CLASS_KEY).setValue(obj.getClass().getName());
                mapper = value.getOptions().getObjectMapperFactory().getMapper((obj.getClass()));
            } else {
                // TODO: Fix for 3.7
                mapper = value.getOptions().getObjectMapperFactory().getMapper(type.getRawType());
            }
            ((ObjectMapper<Object>) mapper).bind(obj).serialize(value);
        }
    }

    @ConfigSerializable
    private static class DummyConfigSerializable { }

    private static class SettingProcessorConstructor implements ClassConstructor<SettingProcessor> {

        private final Injector injector;

        private SettingProcessorConstructor(Injector injector) {
            this.injector = injector;
        }

        @Override public <T extends SettingProcessor> T construct(Class<T> aClass) throws Throwable {
            return this.injector.getInstance(aClass);
        }
    }
}
