/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.rtp.kernels;

import com.flowpowered.math.vector.Vector3d;
import com.flowpowered.math.vector.Vector3i;
import io.github.nucleuspowered.nucleus.api.module.rtp.NucleusRTPService;

import java.util.Random;

public class KernelHelper {

    public static final KernelHelper INSTANCE = new KernelHelper();

    protected KernelHelper() {}

    private final Random random = new Random();

    public int getRandomBetween(int min, int max) {
        return random.nextInt(max - min) + min;
    }

    public int randomSign(int in) {
        return random.nextInt(2) == 0 ? -in : in;
    }

    public Vector3d getLocationWithOffset(Vector3i centre, NucleusRTPService.RTPOptions options) {
        int x = this.randomSign(this.getRandomBetween(options.minRadius(), options.maxRadius())) + centre.getX();
        int y = this.getRandomBetween(options.minHeight(), options.maxHeight());
        int z = this.randomSign(this.getRandomBetween(options.minRadius(), options.maxRadius())) + centre.getZ();
        return new Vector3d(x, y, z);
    }

}
