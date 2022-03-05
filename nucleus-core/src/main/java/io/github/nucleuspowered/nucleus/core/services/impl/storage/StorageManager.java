/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.core.services.impl.storage;

import io.github.nucleuspowered.nucleus.core.services.impl.storage.dataaccess.AbstractDataContainerDataTranslator;
import io.github.nucleuspowered.nucleus.core.services.impl.storage.dataaccess.KeyBasedDataTranslator;
import io.github.nucleuspowered.nucleus.core.services.impl.storage.dataobjects.keyed.AbstractKeyBasedDataObject;
import io.github.nucleuspowered.nucleus.core.util.TypeTokens;
import io.leangen.geantyref.TypeToken;
import org.spongepowered.api.data.persistence.DataContainer;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.github.nucleuspowered.nucleus.core.core.config.CoreConfig;
import io.github.nucleuspowered.nucleus.core.guice.DataDirectory;
import io.github.nucleuspowered.nucleus.core.services.impl.storage.dataobjects.GeneralDataObject;
import io.github.nucleuspowered.nucleus.core.services.impl.storage.dataobjects.IGeneralDataObject;
import io.github.nucleuspowered.nucleus.core.services.impl.storage.dataobjects.IUserDataObject;
import io.github.nucleuspowered.nucleus.core.services.impl.storage.dataobjects.IWorldDataObject;
import io.github.nucleuspowered.nucleus.core.services.impl.storage.dataobjects.UserDataObject;
import io.github.nucleuspowered.nucleus.core.services.impl.storage.dataobjects.WorldDataObject;
import io.github.nucleuspowered.nucleus.core.services.impl.storage.persistence.FlatFileStorageRepositoryFactory;
import io.github.nucleuspowered.storage.query.IUserQueryObject;
import io.github.nucleuspowered.storage.query.IWorldQueryObject;
import io.github.nucleuspowered.nucleus.core.services.impl.storage.services.SingleCachedService;
import io.github.nucleuspowered.nucleus.core.services.impl.storage.services.UserService;
import io.github.nucleuspowered.nucleus.core.services.impl.storage.services.WorldService;
import io.github.nucleuspowered.nucleus.core.services.interfaces.IConfigProvider;
import io.github.nucleuspowered.nucleus.core.services.interfaces.IConfigurateHelper;
import io.github.nucleuspowered.nucleus.core.services.interfaces.IDataVersioning;
import io.github.nucleuspowered.nucleus.core.services.interfaces.IStorageManager;
import io.github.nucleuspowered.nucleus.core.services.impl.storage.dataaccess.IDataTranslator;
import io.github.nucleuspowered.nucleus.core.services.impl.storage.dataobjects.IDataObject;
import io.github.nucleuspowered.storage.persistence.IStorageRepository;
import io.github.nucleuspowered.storage.persistence.IStorageRepositoryFactory;
import io.github.nucleuspowered.nucleus.core.services.impl.storage.services.IStorageService;
import org.apache.logging.log4j.Logger;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.data.persistence.DataView;
import org.spongepowered.api.data.persistence.InvalidDataException;
import org.spongepowered.configurate.ConfigurationOptions;
import org.spongepowered.plugin.PluginContainer;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

@Singleton
public final class StorageManager implements IStorageManager {

    private final FlatFileStorageRepositoryFactory flatFileStorageRepositoryFactory;
    private final IConfigurateHelper configurateHelper;
    private final IConfigProvider configProvider;
    private final IStorageService.SingleCached<IGeneralDataObject> generalService;
    private final UserService userService;
    private final WorldService worldService;

    private final Map<Class<? extends IStorageModule<?, ?, ?, ?>>, IStorageModule<?, ?, ?, ?>> additionalStorageServices = new HashMap<>();

    @Inject
    public StorageManager(
            @DataDirectory final Supplier<Path> dataDirectory,
            final Logger logger,
            final IConfigurateHelper configurateHelper,
            final IConfigProvider configProvider,
            final IDataVersioning dataVersioning,
            final PluginContainer pluginContainer) {
        this.flatFileStorageRepositoryFactory = new FlatFileStorageRepositoryFactory(dataDirectory, logger);
        this.configurateHelper = configurateHelper;
        this.configProvider = configProvider;
        this.userService = new UserService(this, pluginContainer, dataVersioning);
        this.worldService = new WorldService(this, pluginContainer, dataVersioning);
        this.generalService = new SingleCachedService<>(
                this::getGeneralRepository,
                this::getGeneralDataAccess,
                pluginContainer,
                dataVersioning::setVersion,
                dataVersioning::migrate);
    }

    @Override
    public final IStorageRepositoryFactory getFlatFileRepositoryFactory() {
        return this.flatFileStorageRepositoryFactory;
    }

    // ugh
    @Override
    @SuppressWarnings("unchecked")
    public <T extends IDataObject, S extends IStorageService<T>> void register(final IStorageModule<T, S,
            ? extends IStorageRepository, ? extends IDataTranslator<T, DataContainer>> module) {
        final Class<? extends IStorageModule<?, ?, ?, ?>> clazz = (Class<? extends IStorageModule<?, ?, ?, ?>>) module.getClass();
        if (this.additionalStorageServices.containsKey(clazz)) {
            throw new IllegalArgumentException("Class is already registered");
        }
        this.additionalStorageServices.put(clazz, module);
    }

    private IStorageRepository.@Nullable Keyed<UUID, IUserQueryObject, DataContainer> userRepository;

    private IStorageRepository.@Nullable Keyed<ResourceKey, IWorldQueryObject, DataContainer> worldRepository;

    private IStorageRepository.@Nullable Single<DataContainer> generalRepository;

    private final AbstractDataContainerDataTranslator<IUserDataObject> userDataAccess =
            new KeyBasedDataTranslator<>(TypeToken.get(IUserDataObject.class), UserDataObject::new);

    private final AbstractDataContainerDataTranslator<IWorldDataObject> worldDataAccess =
            new KeyBasedDataTranslator<>(TypeToken.get(IWorldDataObject.class), WorldDataObject::new);

    private final AbstractDataContainerDataTranslator<IGeneralDataObject> generalDataAccess =
            new KeyBasedDataTranslator<>(TypeToken.get(IGeneralDataObject.class), GeneralDataObject::new);;

    @Override
    public IStorageService.SingleCached<IGeneralDataObject> getGeneralService() {
        return this.generalService;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends IDataObject, S extends IStorageService<T>, M extends IStorageModule<T, S, ?, ?>> Optional<S> getAdditionalStorageServiceForDataObject(final Class<M> clazz) {
        return Optional.ofNullable(this.additionalStorageServices.get(clazz)).map(x -> (S) x.getService());
    }

    @Override
    public UserService getUserService() {
        return this.userService;
    }

    @Override
    public WorldService getWorldService() {
        return this.worldService;
    }

    @Override public IDataTranslator<IUserDataObject, DataContainer> getUserDataAccess() {
        return this.userDataAccess;
    }

    @Override public IDataTranslator<IWorldDataObject, DataContainer> getWorldDataAccess() {
        return this.worldDataAccess;
    }

    @Override public IDataTranslator<IGeneralDataObject, DataContainer> getGeneralDataAccess() {
        return this.generalDataAccess;
    }

    @Override
    public IStorageRepository.Keyed<UUID, IUserQueryObject, DataContainer> getUserRepository() {
        if (this.userRepository == null) {
            // fallback to flat file
            this.userRepository = this.flatFileStorageRepositoryFactory.userRepository();
        }
        return this.userRepository;
    }

    @Override
    public IStorageRepository.Keyed<ResourceKey, IWorldQueryObject, DataContainer> getWorldRepository() {
        if (this.worldRepository== null) {
            // fallback to flat file
            this.worldRepository = this.flatFileStorageRepositoryFactory.worldRepository();
        }
        return this.worldRepository;
    }

    @Override
    public IStorageRepository.Single<DataContainer> getGeneralRepository() {
        if (this.generalRepository == null) {
            // fallback to flat file
            this.generalRepository = this.flatFileStorageRepositoryFactory.generalRepository();
        }
        return this.generalRepository;
    }

    @Override
    public CompletableFuture<Void> saveAndInvalidateAllCaches() {
        final List<CompletableFuture<Void>> futures = new ArrayList<>();
        futures.add(this.generalService.ensureSaved().whenComplete((cv, t) -> this.generalService.clearCache()));
        futures.add(this.userService.ensureSaved().whenComplete((cv, t) -> this.userService.clearCache()));
        futures.add(this.worldService.ensureSaved().whenComplete((cv, t) -> this.worldService.clearCache()));
        this.additionalStorageServices.values().forEach(x -> futures.add(x.getService().ensureSaved().whenComplete((cv, t) -> x.getService().clearCache())));
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
    }

    @Override
    public CompletableFuture<Void> saveAll() {
        final List<CompletableFuture<Void>> futures = new ArrayList<>();
        futures.add(this.generalService.ensureSaved());
        futures.add(this.userService.ensureSaved());
        futures.add(this.worldService.ensureSaved());
        this.additionalStorageServices.values().forEach(x -> futures.add(x.getService().ensureSaved()));
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
    }

    @Override
    public void attachAll() {
        this.detachAll();
        // TODO: attach based on core config
        final CoreConfig config = this.configProvider.getCoreConfig();
    }

    @Override
    public void detachAll() {
        // TODO: Data registry
        if (this.generalRepository != null) {
            this.generalRepository.shutdown();
        }

        this.generalRepository = null;

        if (this.worldRepository != null) {
            this.worldRepository.shutdown();
        }

        this.worldRepository = null;

        if (this.userRepository != null) {
            this.userRepository.shutdown();
        }

        this.userRepository = null;

        this.additionalStorageServices.values().forEach(IStorageModule::detach);
    }



}

