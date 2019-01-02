/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.storage.services;

import io.github.nucleuspowered.nucleus.Nucleus;
import io.github.nucleuspowered.nucleus.util.ThrownSupplier;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.scheduler.Task;

import java.util.concurrent.CompletableFuture;

public class ServicesUtil {

    public static <R> CompletableFuture<R> run(ThrownSupplier<R, Exception> taskConsumer) {
        CompletableFuture<R> future = new CompletableFuture<>();

        if (Sponge.getServer().isMainThread()) {
            Task.builder().async().execute(t -> runInternal(future, taskConsumer)).submit(Nucleus.getNucleus());
        } else {
            runInternal(future, taskConsumer);
        }

        return future;
    }

    private static <R> void runInternal(CompletableFuture<R> future, ThrownSupplier<R, Exception> taskConsumer) {
        try {
            future.complete(taskConsumer.get());
        } catch (Exception e) {
            future.completeExceptionally(e);
        }
    }
}
