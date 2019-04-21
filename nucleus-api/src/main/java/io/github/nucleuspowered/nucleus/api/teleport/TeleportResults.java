/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.api.teleport;

import org.spongepowered.api.util.annotation.CatalogedBy;
import org.spongepowered.api.util.generator.dummy.DummyObjectProvider;

@CatalogedBy(TeleportResult.class)
public final class TeleportResults {

    public static final TeleportResult SUCCESS = DummyObjectProvider.createFor(TeleportResult.class, "SUCCESS");

    public static final TeleportResult FAIL_NO_LOCATION = DummyObjectProvider.createFor(TeleportResult.class, "FAIL_NO_LOCATION");

    public static final TeleportResult FAIL_CANCELLED = DummyObjectProvider.createFor(TeleportResult.class, "FAIL_CANCELLED");

}
