/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.core.services;

import io.github.nucleuspowered.nucleus.modules.core.config.CoreConfig;
import io.github.nucleuspowered.nucleus.scaffold.service.ServiceBase;
import io.github.nucleuspowered.nucleus.services.INucleusServiceCollection;
import io.github.nucleuspowered.nucleus.services.impl.storage.dataobjects.modular.IUserDataObject;
import io.github.nucleuspowered.nucleus.services.impl.storage.queryobjects.IUserQueryObject;
import io.github.nucleuspowered.nucleus.services.interfaces.IReloadableService;
import io.github.nucleuspowered.storage.services.IStorageService;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.manipulator.mutable.entity.JoinData;
import org.spongepowered.api.profile.GameProfile;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.service.user.UserStorageService;

import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class UniqueUserService implements ServiceBase, IReloadableService.Reloadable {

    public static UniqueUserService INSTANCE;
    private final INucleusServiceCollection serviceCollection;
    private boolean isMoreAccurate = false;

    @Inject
    public UniqueUserService(INucleusServiceCollection serviceCollection) {
        if (INSTANCE == null) {
            INSTANCE = this;
        }
        this.serviceCollection = serviceCollection;
    }

    private static boolean ERROR_REPORTED = false;

    // This is a session variable - does not get saved on restart.
    private long userCount = 0;
    private boolean userCountIsDirty = false;

    public long getUniqueUserCount() {
        if (this.userCountIsDirty) {
            return this.userCount + 1;
        }

        return this.userCount;
    }

    public void resetUniqueUserCount() {
        resetUniqueUserCount(null);
    }

    public void resetUniqueUserCount(@Nullable final Consumer<Long> resultConsumer) {
        if (!this.userCountIsDirty) {
            this.userCountIsDirty = true;
            ERROR_REPORTED = false;

            if (Sponge.getServer().isMainThread()) {
                Task.builder().async().execute(t -> this.doTask(resultConsumer)).submit(this.serviceCollection.pluginContainer());
            } else {
                this.doTask(resultConsumer);
            }
        }
    }

    private void doTask(@Nullable final Consumer<Long> resultConsumer) {
        UserStorageService uss = Sponge.getServiceManager().provideUnchecked(UserStorageService.class);
        IStorageService.Keyed<UUID, IUserQueryObject, IUserDataObject> service =
                this.serviceCollection.storageManager().getUserService();

        // This could be slow...
        if (this.isMoreAccurate) {
            this.userCount = uss.getAll().stream().filter(GameProfile::isFilled)
                    .map(uss::get).filter(Optional::isPresent)
                    .filter(x -> {
                        boolean ret = x.get().getPlayer().isPresent() || service.exists(x.get().getUniqueId()).join(); // already async
                        if (!ret) {
                            try {
                                // Temporary until Data is hooked up properly, I hope.
                                return x.get().get(JoinData.class).map(y -> y.firstPlayed().getDirect().isPresent()).orElse(false);
                            } catch (IllegalStateException e) {
                                if (!ERROR_REPORTED) {
                                    ERROR_REPORTED = true;
                                    this.serviceCollection.logger().warn("The Sponge player data provider has not yet been initialised, not "
                                            + "using join data in this count.");
                                }
                            } catch (NoSuchElementException e) {
                                if (!ERROR_REPORTED) {
                                    ERROR_REPORTED = true;
                                    this.serviceCollection.logger().warn("The join data can not be constructed on some users.");
                                }
                            }
                        }

                        return ret;
                    }).count();
        } else {
            this.userCount = uss.getAll().stream().filter(GameProfile::isFilled).filter(x -> service.exists(x.getUniqueId()).join()).count();
        }

        this.userCountIsDirty = false;
        if (resultConsumer != null) {
            resultConsumer.accept(this.userCount);
        }
    }

    @Override public void onReload(INucleusServiceCollection serviceCollection) {
        this.isMoreAccurate = serviceCollection.moduleDataProvider().getModuleConfig(CoreConfig.class)
                .isMoreAccurate();
    }
}
