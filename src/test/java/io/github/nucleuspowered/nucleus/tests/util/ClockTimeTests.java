/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.tests.util;

import com.cronutils.model.Cron;
import io.github.nucleuspowered.nucleus.util.ClockTime;

import org.junit.Assert;
import org.junit.Test;

public class ClockTimeTests {

    @Test
    public void testParsingHHMM() throws NullPointerException, IllegalArgumentException {
        ClockTime clockTime = ClockTime.fromString("10:30");

        Assert.assertNotNull(clockTime);
        Assert.assertEquals("10:30", clockTime.asString());
    }

    @Test
    public void testParsingHHMMSS() throws NullPointerException, IllegalArgumentException {
        ClockTime clockTime = ClockTime.fromString("10:30:25");

        Assert.assertNotNull(clockTime);
        Assert.assertEquals("10:30:25", clockTime.asString());
    }

    @Test
    public void testFormattingWithTwoChars() throws NullPointerException, IllegalArgumentException {
        ClockTime first = ClockTime.fromString("09:25");
        ClockTime second = ClockTime.fromString("10:09:09");

        Assert.assertNotNull(first);
        Assert.assertNotNull(second);
        Assert.assertEquals("09:25", first.asString());
        Assert.assertEquals("10:09:09", second.asString());
    }

    @Test
    public void testBackingCronFormat() throws NullPointerException, IllegalArgumentException {
        ClockTime asClockTime = ClockTime.fromString("09:09:09");
        Assert.assertNotNull(asClockTime);
        Cron cron = asClockTime.getBackingCron();

        Assert.assertNotNull(cron);
        Assert.assertEquals("9 9 9 * * ? *", cron.asString());
    }
}
