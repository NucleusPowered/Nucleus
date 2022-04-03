/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.core.services.impl.scheduler;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.github.nucleuspowered.nucleus.core.services.interfaces.ISchedulerService;
import org.spongepowered.api.Sponge;
import org.spongepowered.plugin.PluginContainer;

import java.util.concurrent.CompletableFuture;

@Singleton
public final class SchedulerService implements ISchedulerService {

    private final PluginContainer pluginContainer;

    @Inject
    public SchedulerService(final PluginContainer pluginContainer) {
        this.pluginContainer = pluginContainer;
    }

    @Override
    public CompletableFuture<Void> runOnMainThread(final Runnable runnable) {
        if (Sponge.server().onMainThread()) {
            try {
                runnable.run();
                return CompletableFuture.completedFuture(null);
            } catch (final Throwable t) {
                final CompletableFuture<Void> completableFuture = new CompletableFuture<>();
                completableFuture.completeExceptionally(t);
                return completableFuture;
            }
        } else {
            final CompletableFuture<Void> future = new CompletableFuture<>();
            Sponge.server().scheduler().executor(this.pluginContainer).submit(() -> {
                try {
                    runnable.run();
                    return CompletableFuture.completedFuture(null);
                } catch (final Throwable t) {
                    final CompletableFuture<Void> completableFuture = new CompletableFuture<>();
                    completableFuture.completeExceptionally(t);
                    return completableFuture;
                }
            });
            return future;
        }
    }
}
