/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.api.module.rtp.kernel;

import io.github.nucleuspowered.nucleus.api.module.rtp.NucleusRTPService;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.registry.DefaultedRegistryReference;
import org.spongepowered.api.registry.DefaultedRegistryType;
import org.spongepowered.api.registry.RegistryRoots;
import org.spongepowered.api.registry.RegistryType;

/**
 * Nucleus supplied {@link RTPKernel}s.
 *
 * <p>Check that the {@link NucleusRTPService}
 * exists first before attempting to use these kernels.</p>
 */
public final class RTPKernels {

    private RTPKernels() {} // No instantiation please!

    public final static class Identifiers {

        public static final ResourceKey AROUND_PLAYER = ResourceKey.of("nucleus", "around_player");
        public static final ResourceKey AROUND_PLAYER_SURFACE = ResourceKey.of("nucleus", "around_player_surface");
        public static final ResourceKey DEFAULT = ResourceKey.of("nucleus", "default");
        public static final ResourceKey SURFACE_ONLY = ResourceKey.of("nucleus", "surface_only");

    }

    public final static DefaultedRegistryType<RTPKernel> REGISTRY_TYPE =
            RegistryType.of(RegistryRoots.SPONGE, ResourceKey.of("nucleus", "rtp")).asDefaultedType(() -> Sponge.game());

    /**
     * The default Nucleus RTP kernel, adjusted to centre around the player,
     * not the world border centre.
     *
     * <p>This has an ID of {@code nucleus:around_player}</p>
     */
    public final static DefaultedRegistryReference<RTPKernel> AROUND_PLAYER = RTPKernels.getReference(RTPKernels.Identifiers.AROUND_PLAYER);

    /**
     * The default Nucleus RTP kernel, adjusted to centre around the player,
     * not the world border centre, and surface only
     *
     * <p>This has an ID of {@code nucleus:around_player_surface}</p>
     */
    public final static DefaultedRegistryReference<RTPKernel> AROUND_PLAYER_SURFACE = RTPKernels.getReference(Identifiers.AROUND_PLAYER_SURFACE);

    /**
     * The default Nucleus RTP kernel.
     *
     * <p>This has an ID of {@code nucleus:default}</p>
     */
    public final static DefaultedRegistryReference<RTPKernel> DEFAULT = RTPKernels.getReference(Identifiers.DEFAULT);

    /**
     * The default Nucleus RTP kernel, adjusted to ensure locations are surface only.
     *
     * <p>This has an ID of {@code nucleus:surface_only}</p>
     */
    public final static DefaultedRegistryReference<RTPKernel> SURFACE_ONLY = RTPKernels.getReference(Identifiers.SURFACE_ONLY);

    private static DefaultedRegistryReference<RTPKernel> getReference(final ResourceKey resourceKey) {
        return RTPKernels.REGISTRY_TYPE.asDefaultedType(Sponge::game).defaultReferenced(resourceKey);
    }

}
