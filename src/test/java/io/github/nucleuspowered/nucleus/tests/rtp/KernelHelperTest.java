/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.tests.rtp;

import com.flowpowered.math.vector.Vector3d;
import com.flowpowered.math.vector.Vector3i;
import io.github.nucleuspowered.nucleus.api.service.NucleusRTPService;
import io.github.nucleuspowered.nucleus.modules.rtp.kernels.KernelHelper;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.spongepowered.api.world.biome.BiomeType;

import java.util.Arrays;
import java.util.Set;

@RunWith(Parameterized.class)
public class KernelHelperTest {

    private final static TestOptions DUMMY = new TestOptions();

    @Parameterized.Parameters(name = "{index}: x = {0}, y = {1}, signx = {2}, signz = {3}, xc = {4}, zc = {5}, expected x = {6}, expected z = {7}")
    public static Iterable<Object[]> data() {
        return Arrays.asList(new Object[][] {
                { 0, 0, 1, 1, 0, 0, 0, 0 },
                { 5, 5, 1, 1, 0, 0, 5, 5 },
                { 5, 5, -1, -1, 0, 0, -5, -5 },
                { 5, 5, 1, -1, 0, 0, 5, -5 },
                { 5, 5, -1, 1, 0, 0, -5, 5 },
                { 5, 5, 1, -1, 5, 5, 10, 0 },
                { 5, 5, -1, 1, 5, -5, 0, 0 },
                { 120, 340, 1, -1, 600, -2400, 720, -2740 },
                { 120, 340, -1, 1, 600, -2400, 480, -2060 }
        });
    }

    @Parameterized.Parameter(0)
    public int x;

    @Parameterized.Parameter(1)
    public int z;

    @Parameterized.Parameter(2)
    public int signx;

    @Parameterized.Parameter(3)
    public int signz;

    @Parameterized.Parameter(4)
    public int xc;

    @Parameterized.Parameter(5)
    public int zc;

    @Parameterized.Parameter(6)
    public int expectedx;

    @Parameterized.Parameter(7)
    public int expectedz;

    @Test
    public void TestRTPreturnsRightAnswer() {
        final Vector3i centre = new Vector3i(this.xc, 0, this.zc);
        KernelHelperTestingClass testingClass = new KernelHelperTestingClass(this.x, this.z, this.signx, this.signz);
        Vector3d result = testingClass.getLocationWithOffset(centre, DUMMY);
        Assert.assertEquals("x is incorrect", this.expectedx, result.getFloorX());
        Assert.assertEquals("z is incorrect", this.expectedz, result.getFloorZ());
    }

    static class TestOptions implements NucleusRTPService.RTPOptions {

        @Override
        public int maxRadius() {
            return 0;
        }

        @Override
        public int minRadius() {
            return 0;
        }

        @Override
        public int minHeight() {
            return 0;
        }

        @Override
        public int maxHeight() {
            return 0;
        }

        @Override
        public Set<BiomeType> prohibitedBiomes() {
            return null;
        }
    }

    private static class KernelHelperTestingClass extends KernelHelper {

        private final int x;
        private final int z;
        private final int signx;
        private final int signz;
        private int count = -1;

        private KernelHelperTestingClass(int x, int z, int signx, int signz) {
            this.x = x;
            this.z = z;
            this.signx = signx;
            this.signz = signz;
        }

        @Override
        public int getRandomBetween(int min, int max) {
            ++this.count;
            if (this.count == 0) {
                return this.x;
            } else if (this.count == 1) {
                return 0;
            } else {
                return this.z;
            }

        }

        @Override
        public int randomSign(int in) {
            if (this.count == 0) {
                return this.signx  * in;
            }
            return this.signz * in;
        }

    }
}
