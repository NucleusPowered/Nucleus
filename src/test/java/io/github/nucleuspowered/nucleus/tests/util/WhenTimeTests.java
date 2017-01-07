/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.tests.util;

import com.cronutils.model.Cron;
import org.junit.Assert;
import org.junit.Test;

import io.github.nucleuspowered.nucleus.util.WhenTime;

public class WhenTimeTests {

    @Test
    public void testThreeArgumentParsing() throws IllegalArgumentException, NullPointerException {
        WhenTime wtDow = WhenTime.fromString("every 4 dow");
        WhenTime wtDom = WhenTime.fromString("every 5 days-of-the-month");
        WhenTime wtMonths = WhenTime.fromString("every 11 months");
        WhenTime wtHours = WhenTime.fromString("every 14 hour");
        WhenTime wtMinutes = WhenTime.fromString("every 5 minute");
        WhenTime wtSeconds = WhenTime.fromString("every 10 seconds");

        Assert.assertNotNull(wtDow);
        Assert.assertNotNull(wtDom);
        Assert.assertNotNull(wtMonths);
        Assert.assertNotNull(wtHours);
        Assert.assertNotNull(wtMinutes);
        Assert.assertNotNull(wtSeconds);
    }

    @Test
    public void testTwoArgumentParsing() throws IllegalArgumentException, NullPointerException {
        WhenTime wtDow = WhenTime.fromString("every dow");
        WhenTime wtDom = WhenTime.fromString("every days-of-the-month");
        WhenTime wtMonths = WhenTime.fromString("every months");
        WhenTime wtHours = WhenTime.fromString("every hour");
        WhenTime wtMinutes = WhenTime.fromString("every minute");
        WhenTime wtSeconds = WhenTime.fromString("every seconds");

        Assert.assertNotNull(wtDow);
        Assert.assertNotNull(wtDom);
        //Assert.assertNotNull(wtYears);
        Assert.assertNotNull(wtMonths);
        Assert.assertNotNull(wtHours);
        Assert.assertNotNull(wtMinutes);
        Assert.assertNotNull(wtSeconds);
    }

    @Test
    public void testAsStringThreeArguments() throws IllegalArgumentException, NullPointerException {
        WhenTime wtDow = WhenTime.fromString("every 4 dow");
        WhenTime wtDom = WhenTime.fromString("every 5 days-of-the-month");
        WhenTime wtMonths = WhenTime.fromString("every 11 months");
        WhenTime wtHours = WhenTime.fromString("every 14 hour");
        WhenTime wtMinutes = WhenTime.fromString("every 5 minute");
        WhenTime wtSeconds = WhenTime.fromString("every 10 seconds");

        Assert.assertEquals("every 4 day_of_week", wtDow.asString());
        Assert.assertEquals("every 5 day_of_month", wtDom.asString());
        Assert.assertEquals("every 11 months", wtMonths.asString());
        Assert.assertEquals("every 14 hours", wtHours.asString());
        Assert.assertEquals("every 5 minutes", wtMinutes.asString());
        Assert.assertEquals("every 10 seconds", wtSeconds.asString());
    }

    @Test
    public void testAsStringTwoArguments() throws IllegalArgumentException, NullPointerException {
        WhenTime wtDow = WhenTime.fromString("every dow");
        WhenTime wtDom = WhenTime.fromString("every days-of-the-month");
        WhenTime wtMonths = WhenTime.fromString("every months");
        WhenTime wtHours = WhenTime.fromString("every hour");
        WhenTime wtMinutes = WhenTime.fromString("every minute");
        WhenTime wtSeconds = WhenTime.fromString("every seconds");

        Assert.assertEquals("every day_of_week", wtDow.asString());
        Assert.assertEquals("every day_of_month", wtDom.asString());
        Assert.assertEquals("every months", wtMonths.asString());
        Assert.assertEquals("every hours", wtHours.asString());
        Assert.assertEquals("every minutes", wtMinutes.asString());
        Assert.assertEquals("every seconds", wtSeconds.asString());
    }

    @Test
    public void underlyingCronInstance() throws IllegalArgumentException, NullPointerException {
        WhenTime wtDow = WhenTime.fromString("every dow");
        WhenTime wtDom = WhenTime.fromString("every days-of-the-month");
        WhenTime wtMonths = WhenTime.fromString("every months");
        WhenTime wtHours = WhenTime.fromString("every hour");
        WhenTime wtMinutes = WhenTime.fromString("every minute");
        WhenTime wtSeconds = WhenTime.fromString("every seconds");

        Cron cronDow = wtDow.getBackingCronInstance();
        Cron cronDom = wtDom.getBackingCronInstance();
        //Cron cronYears = wtYears.getBackingCronInstance();
        Cron cronMonths = wtMonths.getBackingCronInstance();
        Cron cronHours = wtHours.getBackingCronInstance();
        Cron cronMinutes = wtMinutes.getBackingCronInstance();
        Cron cronSeconds = wtSeconds.getBackingCronInstance();

        Assert.assertNotNull(cronDow);
        Assert.assertNotNull(cronDom);
        Assert.assertNotNull(cronMonths);
        Assert.assertNotNull(cronHours);
        Assert.assertNotNull(cronMinutes);
        Assert.assertNotNull(cronSeconds);

        Assert.assertEquals("0 0 0 ? * * *", cronDow.asString());
        Assert.assertEquals("0 0 0 * * ? *", cronDom.asString());
        Assert.assertEquals("0 0 0 1 * ? *", cronMonths.asString());
        Assert.assertEquals("0 0 * * * ? *", cronHours.asString());
        Assert.assertEquals("0 * 0 * * ? *", cronMinutes.asString());
        Assert.assertEquals("* 0 0 * * ? *", cronSeconds.asString());
    }
}
