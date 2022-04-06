/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.core.services.impl.configurate;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.github.nucleuspowered.nucleus.api.text.NucleusTextTemplate;
import io.github.nucleuspowered.nucleus.core.configurate.typeserialisers.InstantTypeSerialiser;
import io.github.nucleuspowered.nucleus.core.configurate.typeserialisers.LocaleSerialiser;
import io.github.nucleuspowered.nucleus.core.configurate.typeserialisers.NamedLocationSerialiser;
import io.github.nucleuspowered.nucleus.core.configurate.typeserialisers.NucleusTextTemplateTypeSerialiser;
import io.github.nucleuspowered.nucleus.core.configurate.typeserialisers.PatternTypeSerialiser;
import io.github.nucleuspowered.nucleus.core.configurate.typeserialisers.Vector3dTypeSerialiser;
import io.github.nucleuspowered.nucleus.core.services.INucleusServiceCollection;
import io.github.nucleuspowered.nucleus.core.services.interfaces.IConfigurateHelper;
import io.github.nucleuspowered.nucleus.core.services.interfaces.annotation.configuratehelper.LocalisedComment;
import io.github.nucleuspowered.nucleus.core.util.TypeTokens;
import io.leangen.geantyref.TypeToken;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.api.config.DefaultConfig;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.ConfigurationOptions;
import org.spongepowered.configurate.loader.ConfigurationLoader;
import org.spongepowered.configurate.objectmapping.ObjectMapper;
import org.spongepowered.configurate.serialize.TypeSerializerCollection;
import org.spongepowered.math.vector.Vector3d;

import java.time.Instant;
import java.util.regex.Pattern;

@Singleton
public class ConfigurateHelper implements IConfigurateHelper {

    private final TypeSerializerCollection baseTypeSerializerCollection;
    private final ObjectMapper.Factory objectMapperFactory;
    private final TypeSerializerCollection.Builder moduleBuilderTSC;
    private final ConfigurationOptions options;

    @MonotonicNonNull private ConfigurationOptions defaultConfigOptions;
    @MonotonicNonNull private ConfigurationOptions defaultDataOptions;

    @MonotonicNonNull private TypeSerializerCollection withModules = null;

    @Inject
    public ConfigurateHelper(final INucleusServiceCollection serviceCollection,
            @DefaultConfig(sharedRoot = false) final ConfigurationLoader<CommentedConfigurationNode> configurationLoader) {
        this.options = configurationLoader.defaultOptions();
        this.objectMapperFactory = ObjectMapper
                .factoryBuilder()
                .addProcessor(LocalisedComment.class, ObjectMapperActions.localisedComments(serviceCollection.messageProvider()))
                .build();
        this.baseTypeSerializerCollection = ConfigurateHelper.setupCore(
                serviceCollection,
                this.objectMapperFactory,
                configurationLoader.defaultOptions());
        this.moduleBuilderTSC = this.baseTypeSerializerCollection.childBuilder();
    }

    @Override
    public ConfigurationOptions getDefaultConfigOptions() {
        if (this.withModules != null) {
            if (this.defaultConfigOptions == null) {
                this.defaultConfigOptions = this.options.serializers(this.withModules);
            }
            return this.defaultConfigOptions;
        }

        return this.options.serializers(this.baseTypeSerializerCollection);
    }

    @Override
    public ConfigurationOptions getDefaultDataOptions() {
        if (this.withModules != null) {
            if (this.defaultDataOptions == null) {
                this.defaultDataOptions = this.options.implicitInitialization(false).serializers(this.withModules);
            }
            return this.defaultDataOptions;
        }

        return this.options.implicitInitialization(false).serializers(this.baseTypeSerializerCollection);
    }

    /**
     * Set Nucleus specific options on the {@link ConfigurationOptions}
     *
     * @param options The {@link ConfigurationOptions} to alter.
     * @return The {@link ConfigurationOptions}, for easier inline use of this function.
     */
    @Override
    public ConfigurationOptions setOptions(final ConfigurationOptions options) {
        // Allows us to use localised comments and @ProcessSetting annotations
        if (this.withModules != null) {
            return options.serializers(this.withModules);
        } else {
            return options.serializers(this.baseTypeSerializerCollection);
        }
    }

    @Override
    public void addTypeSerialiser(@Nullable final TypeSerializerCollection collection) {
        if (this.withModules != null) {
            throw new IllegalStateException("Modules have been initialised.");
        }
        if (collection != null) {
            this.moduleBuilderTSC.registerAll(collection);
        }
    }

    @Override
    public TypeSerializerCollection complete() {
        this.withModules = this.moduleBuilderTSC.build();
        return this.withModules;
    }

    @Override
    public CommentedConfigurationNode createConfigNode() {
        return CommentedConfigurationNode.root(this.getDefaultConfigOptions());
    }

    @Override
    public CommentedConfigurationNode createDataNode() {
        return CommentedConfigurationNode.root(this.getDefaultDataOptions());
    }

    private static TypeSerializerCollection setupCore(final INucleusServiceCollection serviceCollection,
            final ObjectMapper.Factory factory,
            final ConfigurationOptions options) {
        final TypeSerializerCollection.Builder typeSerializerCollection = options.serializers().childBuilder();

        // Custom type serialisers for Nucleus
        typeSerializerCollection.registerAnnotatedObjects(factory);
        typeSerializerCollection.register(TypeToken.get(Vector3d.class), new Vector3dTypeSerialiser());
        typeSerializerCollection.register(TypeToken.get(Pattern.class), new PatternTypeSerialiser());
        typeSerializerCollection.register(TypeToken.get(NucleusTextTemplate.class),
                new NucleusTextTemplateTypeSerialiser(serviceCollection.textTemplateFactory()));
        typeSerializerCollection.register(TypeToken.get(Instant.class), new InstantTypeSerialiser());

        typeSerializerCollection.register(TypeTokens.NAMED_LOCATION, new NamedLocationSerialiser());
        typeSerializerCollection.register(TypeTokens.LOCALE, new LocaleSerialiser());

        return typeSerializerCollection.build();
    }

}
