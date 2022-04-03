/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.core.core.services;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.github.nucleuspowered.nucleus.core.scaffold.service.ServiceBase;
import io.github.nucleuspowered.nucleus.core.services.INucleusServiceCollection;
import io.github.nucleuspowered.nucleus.core.services.impl.storage.dataobjects.modular.IUserDataObject;
import io.github.nucleuspowered.nucleus.core.services.impl.storage.queryobjects.IUserQueryObject;
import io.github.nucleuspowered.nucleus.core.services.interfaces.IReloadableService;
import io.github.nucleuspowered.storage.services.IStorageService;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.profile.GameProfile;
import org.spongepowered.api.user.UserManager;

import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import org.checkerframework.checker.nullness.qual.Nullable;

@Singleton
public class UniqueUserService implements ServiceBase, IReloadableService.Reloadable {

    public static UniqueUserService INSTANCE;
    private final INucleusServiceCollection serviceCollection;
    private boolean isMoreAccurate = false;

    @Inject
    public UniqueUserService(final INucleusServiceCollection serviceCollection) {
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
        this.resetUniqueUserCount(null);
    }

    public void resetUniqueUserCount(@Nullable final Consumer<Long> resultConsumer) {
        if (!this.userCountIsDirty) {
            this.userCountIsDirty = true;
            ERROR_REPORTED = false;

            if (Sponge.server().onMainThread()) {
                Sponge.asyncScheduler().executor(this.serviceCollection.pluginContainer())
                        .submit(() -> this.doTask(resultConsumer));
            } else {
                this.doTask(resultConsumer);
            }
        }
    }

    private void doTask(@Nullable final Consumer<Long> resultConsumer) {
        final UserManager uss = Sponge.server().userManager();
        final IStorageService.Keyed<UUID, IUserQueryObject, IUserDataObject> service =
                this.serviceCollection.storageManager().getUserService();

        // This could be slow...
        if (this.isMoreAccurate) {
            this.userCount = uss.streamAll().filter(GameProfile::hasName)
                    .map(uss::load)
                    .map(CompletableFuture::join)
                    .filter(Optional::isPresent)
                    .filter(x -> {
                        final boolean ret = x.get().player().isPresent() || service.exists(x.get().uniqueId()).join(); // already async
                        if (!ret) {
                            try {
                                return x.get().get(Keys.FIRST_DATE_JOINED).isPresent();
                            } catch (final IllegalStateException e) {
                                if (!ERROR_REPORTED) {
                                    ERROR_REPORTED = true;
                                    this.serviceCollection.logger().warn("The Sponge player data provider has not yet been initialised, not "
                                            + "using join data in this count.");
                                }
                            } catch (final NoSuchElementException e) {
                                if (!ERROR_REPORTED) {
                                    ERROR_REPORTED = true;
                                    this.serviceCollection.logger().warn("The join data can not be constructed on some users.");
                                }
                            }
                        }

                        return ret;
                    }).count();
        } else {
            this.userCount = uss.streamAll().filter(GameProfile::hasName).filter(x -> service.exists(x.uniqueId()).join()).count();
        }

        this.userCountIsDirty = false;
        if (resultConsumer != null) {
            resultConsumer.accept(this.userCount);
        }
    }

    @Override public void onReload(final INucleusServiceCollection serviceCollection) {
        this.isMoreAccurate = serviceCollection.configProvider().getCoreConfig()
                .isMoreAccurate();
    }
}
