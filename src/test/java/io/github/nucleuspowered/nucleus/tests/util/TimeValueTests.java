/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.tests.util;

import io.github.nucleuspowered.nucleus.util.TimeValue;
import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

public class TimeValueTests {

    @Test
    public void testCanParseTimeValue() throws IllegalArgumentException, NullPointerException {
        TimeValue tv = TimeValue.fromString("1 days");

        Assert.assertNotNull(tv);
        Assert.assertEquals(1, tv.getValue());
        Assert.assertEquals(TimeUnit.DAYS, tv.getUnitOfMeasurement());
    }

    @Test
    public void testCanSerializeTimeValue() throws IllegalArgumentException, NullPointerException {
        TimeValue tv = TimeValue.fromString("1 days");
        String asString = tv.asString();

        Assert.assertNotNull(asString);
        Assert.assertEquals("1 days", asString);
    }

    @Test
    public void testCompareTo() throws IllegalArgumentException, NullPointerException {
        TimeValue tv = TimeValue.fromString("1 days");
        TimeValue otherTimeValue = TimeValue.fromString("10 days");
        int comparisonResult = tv.compareTo(otherTimeValue);

        Assert.assertEquals(-1, comparisonResult);
    }
}
