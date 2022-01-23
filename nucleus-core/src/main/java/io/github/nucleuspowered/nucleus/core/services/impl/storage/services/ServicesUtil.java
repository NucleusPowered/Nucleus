/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.core.services.impl.storage.services;

import io.vavr.CheckedFunction0;
import org.spongepowered.api.Sponge;
import org.spongepowered.plugin.PluginContainer;

import java.util.concurrent.CompletableFuture;

public final class ServicesUtil {

    public static <R> CompletableFuture<R> run(final CheckedFunction0<R> taskConsumer, final PluginContainer pluginContainer) {
        return ServicesUtil.run(taskConsumer, pluginContainer, true);
    }

    public static <R> CompletableFuture<R> run(final CheckedFunction0<R> taskConsumer, final PluginContainer pluginContainer, final boolean printException) {
        final CompletableFuture<R> future = new CompletableFuture<>();

        if (Sponge.isServerAvailable() && Sponge.server().onMainThread()) {
            Sponge.asyncScheduler().executor(pluginContainer).submit(() -> ServicesUtil.runInternal(future, taskConsumer, printException));
        } else {
            ServicesUtil.runInternal(future, taskConsumer, printException);
        }

        return future;
    }

    private static <R> void runInternal(final CompletableFuture<R> future, final CheckedFunction0<R> taskConsumer, final boolean printException) {
        try {
            future.complete(taskConsumer.apply());
        } catch (final Throwable e) {
            if (printException) {
                e.printStackTrace();
            }
            future.completeExceptionally(e);
        }
    }
}
