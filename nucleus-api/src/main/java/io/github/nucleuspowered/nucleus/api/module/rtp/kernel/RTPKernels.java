/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.api.module.rtp.kernel;

import io.github.nucleuspowered.nucleus.api.module.rtp.NucleusRTPService;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.util.generator.dummy.DummyObjectProvider;

/**
 * Nucleus supplied {@link RTPKernel}s.
 *
 * <p><strong>A word of warning</strong>, if the RTP module has not been initialised,
 * this objects will remain as dummy objects and will not function.</p>
 *
 * <p>Check that the {@link NucleusRTPService}
 * exists first before attempting to use these kernels.</p>
 */
public final class RTPKernels {

    private RTPKernels() {} // No instantiation please!

    public static class Identifiers {

        public static final String AROUND_PLAYER = "nucleus:around_player";
        public static final String AROUND_PLAYER_SURFACE = "nucleus:around_player_surface";
        public static final String DEFAULT = "nucleus:default";
        public static final String SURFACE_ONLY = "nucleus:surface_only";

    }

    /**
     * The default Nucleus RTP kernel, adjusted to centre around the player,
     * not the world border centre.
     *
     * <p>This has an ID of {@code nucleus:around_player}</p>
     */
    public final static RTPKernel AROUND_PLAYER = Sponge.getRegistry().getType(RTPKernel.class, Identifiers.AROUND_PLAYER).get();

    /**
     * The default Nucleus RTP kernel, adjusted to centre around the player,
     * not the world border centre, and surface only
     *
     * <p>This has an ID of {@code nucleus:around_player_surface}</p>
     */
    public final static RTPKernel AROUND_PLAYER_SURFACE = Sponge.getRegistry().getType(RTPKernel.class, Identifiers.AROUND_PLAYER_SURFACE).get();

    /**
     * The default Nucleus RTP kernel.
     *
     * <p>This has an ID of {@code nucleus:default}</p>
     */
    public final static RTPKernel DEFAULT = Sponge.getRegistry().getType(RTPKernel.class, Identifiers.DEFAULT).get();

    /**
     * The default Nucleus RTP kernel, adjusted to ensure locations are surface only.
     *
     * <p>This has an ID of {@code nucleus:surface_only}</p>
     */
    public final static RTPKernel SURFACE_ONLY = Sponge.getRegistry().getType(RTPKernel.class, Identifiers.SURFACE_ONLY).get();

}
