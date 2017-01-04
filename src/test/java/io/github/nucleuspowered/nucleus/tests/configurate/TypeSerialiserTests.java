/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.tests.configurate;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.reflect.TypeToken;
import com.cronutils.model.Cron;
import com.cronutils.model.CronType;
import com.cronutils.model.definition.CronDefinitionBuilder;
import com.cronutils.parser.CronParser;
import io.github.nucleuspowered.nucleus.configurate.typeserialisers.DelayedTimeValueSerialiser;
import io.github.nucleuspowered.nucleus.configurate.typeserialisers.SetTypeSerialiser;
import io.github.nucleuspowered.nucleus.configurate.typeserialisers.WarningTimeListSerialiser;
import io.github.nucleuspowered.nucleus.util.ClockTime;
import io.github.nucleuspowered.nucleus.util.DelayedTimeType;
import io.github.nucleuspowered.nucleus.util.DelayedTimeValue;
import io.github.nucleuspowered.nucleus.util.TimeValue;
import io.github.nucleuspowered.nucleus.util.WarningTimeList;
import io.github.nucleuspowered.nucleus.util.WhenTime;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import ninja.leaping.configurate.objectmapping.serialize.TypeSerializerCollection;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class TypeSerialiserTests {

    private TestConfigurationLoader getTestLoader() {
        TestConfigurationLoader.Builder tclb = TestConfigurationLoader.builder();
        TypeSerializerCollection tsc = tclb.getDefaultOptions().getSerializers();
        tsc.registerPredicate(
                typeToken -> Set.class.isAssignableFrom(typeToken.getRawType()),
                new SetTypeSerialiser()
        );
        tsc.registerType(TypeToken.of(DelayedTimeValue.class), new DelayedTimeValueSerialiser());
        tsc.registerType(TypeToken.of(WarningTimeList.class), new WarningTimeListSerialiser());

        tclb.setDefaultOptions(tclb.getDefaultOptions().setSerializers(tsc));
        return tclb.build();
    }

    @Test
    public void testThatSetsCanBeSerialised() throws ObjectMappingException {
        TestConfigurationLoader tcl = getTestLoader();
        ConfigurationNode cn = tcl.createEmptyNode().setValue(new TypeToken<Set<String>>() {}, Sets.newHashSet("test", "test2"));

        List<String> ls = cn.getList(TypeToken.of(String.class));
        Assert.assertTrue(ls.contains("test"));
        Assert.assertTrue(ls.contains("test2"));
    }

    @Test
    public void testThatSetsCanBeDeserialised() throws ObjectMappingException {
        TestConfigurationLoader tcl = getTestLoader();
        ConfigurationNode cn = tcl.createEmptyNode().setValue(new TypeToken<List<String>>() {}, Lists.newArrayList("test", "test", "test2"));

        Set<String> ls = cn.getValue(new TypeToken<Set<String>>() {});
        Assert.assertEquals(2, ls.size());
        Assert.assertTrue(ls.contains("test"));
        Assert.assertTrue(ls.contains("test2"));
    }

    @Test
    public void testDelayedTimesCanBeSerialised() throws ObjectMappingException {
        TestConfigurationLoader tcl = getTestLoader();

        CronParser unixCronParser = new CronParser(CronDefinitionBuilder.instanceDefinitionFor(CronType.UNIX));
        CronParser quartzCronParser = new CronParser(CronDefinitionBuilder.instanceDefinitionFor(CronType.QUARTZ));
        CronParser cron4jCronParser = new CronParser(CronDefinitionBuilder.instanceDefinitionFor(CronType.CRON4J));

        Cron unixCron = unixCronParser.parse("0 0 * * *");
        Cron quartzCron = quartzCronParser.parse("0 0 0,12 * * ?");
        Cron cron4jCron = cron4jCronParser.parse("* 12 * * Mon");
        WhenTime whenTime = WhenTime.fromString("every 12 hours");
        ClockTime clockTime = ClockTime.fromString("12:30");

        ConfigurationNode unixNode = tcl.createEmptyNode().setValue(TypeToken.of(DelayedTimeValue.class), new
                DelayedTimeValue(DelayedTimeType.UNIX_CRON, unixCron));
        ConfigurationNode quartzNode = tcl.createEmptyNode().setValue(TypeToken.of(DelayedTimeValue.class), new
                DelayedTimeValue(DelayedTimeType.QUARTZ_CRON, quartzCron));
        ConfigurationNode cron4JNode = tcl.createEmptyNode().setValue(TypeToken.of(DelayedTimeValue.class), new
                DelayedTimeValue(DelayedTimeType.CRON4J_CRON, cron4jCron));
        ConfigurationNode whenNode = tcl.createEmptyNode().setValue(TypeToken.of(DelayedTimeValue.class), new
                DelayedTimeValue(whenTime));
        ConfigurationNode clockNode = tcl.createEmptyNode().setValue(TypeToken.of(DelayedTimeValue.class), new
                DelayedTimeValue(clockTime));

        Assert.assertNotNull(unixNode.getString());
        Assert.assertNotNull(quartzNode.getString());
        Assert.assertNotNull(cron4JNode.getString());
        Assert.assertNotNull(whenNode.getString());
        Assert.assertNotNull(clockNode.getString());
    }

    @Test
    public void testThatStringDelayedTimesCanBeDeserialised() throws ObjectMappingException {
        TestConfigurationLoader tcl = getTestLoader();
        ConfigurationNode unixNode = tcl.createEmptyNode().setValue(new TypeToken<String> () {}, "0 0 * * *");
        ConfigurationNode quartzNode = tcl.createEmptyNode().setValue(new TypeToken<String> () {}, "0 0 0,12 * *"
                + " ?");
        ConfigurationNode cron4JNode = tcl.createEmptyNode().setValue(new TypeToken<String> () {}, "* 12 * * Mon");
        ConfigurationNode whenNode = tcl.createEmptyNode().setValue(new TypeToken<String> () {}, "every 12 hours");
        ConfigurationNode clockNode = tcl.createEmptyNode().setValue(new TypeToken<String> () {}, "12:30");

        DelayedTimeValue asUnix = unixNode.getValue(TypeToken.of(DelayedTimeValue.class));
        DelayedTimeValue asQuartz = quartzNode.getValue(TypeToken.of(DelayedTimeValue.class));
        DelayedTimeValue asCron4J = cron4JNode.getValue(TypeToken.of(DelayedTimeValue.class));
        DelayedTimeValue asWhen = whenNode.getValue(TypeToken.of(DelayedTimeValue.class));
        DelayedTimeValue asClock = clockNode.getValue(TypeToken.of(DelayedTimeValue.class));

        Assert.assertNotNull(asUnix);
        Assert.assertNotNull(asQuartz);
        Assert.assertNotNull(asCron4J);
        Assert.assertNotNull(asWhen);
        Assert.assertNotNull(asClock);
    }

    @Test
    public void testThatMapDelayedTimesCanBeDeserialised() throws ObjectMappingException {
        TestConfigurationLoader tcl = getTestLoader();

        Map<String, String> unixMap = new HashMap<>();
        unixMap.put("type", "unix");
        unixMap.put("statement", "0 0 * * *");
        Map<String, String> quartzMap = new HashMap<>();
        quartzMap.put("type", "quartz");
        quartzMap.put("statement", "0 0 0,12 * * ?");
        Map<String, String> cron4jMap = new HashMap<>();
        cron4jMap.put("type", "cron4j");
        cron4jMap.put("statement", "* 12 * * Mon");
        Map<String, String> whenMap = new HashMap<>();
        whenMap.put("type", "when");
        whenMap.put("statement", "every 12 hours");
        Map<String, String> clockMap = new HashMap<>();
        clockMap.put("type", "clock");
        clockMap.put("statement", "12:30");

        ConfigurationNode unixNode = tcl.createEmptyNode().setValue(new TypeToken<Map<String, String>> () {},
                unixMap);
        ConfigurationNode quartzNode = tcl.createEmptyNode().setValue(new TypeToken<Map<String, String>> () {},
                quartzMap);
        ConfigurationNode cron4JNode = tcl.createEmptyNode().setValue(new TypeToken<Map<String, String>> () {},
                cron4jMap);
        ConfigurationNode whenNode = tcl.createEmptyNode().setValue(new TypeToken<Map<String, String>> () {},
                whenMap);
        ConfigurationNode clockNode = tcl.createEmptyNode().setValue(new TypeToken<Map<String, String>> () {},
                clockMap);

        DelayedTimeValue asUnix = unixNode.getValue(TypeToken.of(DelayedTimeValue.class));
        DelayedTimeValue asQuartz = quartzNode.getValue(TypeToken.of(DelayedTimeValue.class));
        DelayedTimeValue asCron4J = cron4JNode.getValue(TypeToken.of(DelayedTimeValue.class));
        DelayedTimeValue asWhen = whenNode.getValue(TypeToken.of(DelayedTimeValue.class));
        DelayedTimeValue asClock = clockNode.getValue(TypeToken.of(DelayedTimeValue.class));

        Assert.assertNotNull(asUnix);
        Assert.assertNotNull(asQuartz);
        Assert.assertNotNull(asCron4J);
        Assert.assertNotNull(asWhen);
        Assert.assertNotNull(asClock);
    }

    @Test
    public void testThatWarningTimeListCanBeSerialised() throws ObjectMappingException {
        TestConfigurationLoader tcl = getTestLoader();
        List<TimeValue> timeValues = new ArrayList<>();
        timeValues.add(new TimeValue(1L, TimeUnit.MILLISECONDS));
        timeValues.add(new TimeValue(10L, TimeUnit.HOURS));
        WarningTimeList wtl = new WarningTimeList(timeValues);
        ConfigurationNode cn = tcl.createEmptyNode().setValue(new TypeToken<WarningTimeList> () {}, wtl);

        List<String> ls = cn.getList(TypeToken.of(String.class));
        Assert.assertEquals("10 hours", ls.get(0));
        Assert.assertEquals("1 milliseconds", ls.get(1));
    }

    @Test
    public void testThatWarningTimeListCanBeDeserialisedString() throws ObjectMappingException {
        TestConfigurationLoader tcl = getTestLoader();
        ConfigurationNode cn = tcl.createEmptyNode().setValue(TypeToken.of(String.class),
                "1 milliseconds,10 hours,4 minutes");
        WarningTimeList wtl = cn.getValue(TypeToken.of(WarningTimeList.class));

        Assert.assertNotNull(wtl);
        Assert.assertEquals("10 hours", wtl.getWarningTimeAt(0).asString());
        Assert.assertEquals("4 minutes", wtl.getWarningTimeAt(1).asString());
        Assert.assertEquals("1 milliseconds", wtl.getWarningTimeAt(2).asString());
    }

    @Test
    public void testThatWarningTimeListCanBeDeserialisedList() throws ObjectMappingException {
        TestConfigurationLoader tcl = getTestLoader();
        List<String> asListYo = new ArrayList<>();
        asListYo.add("1 milliseconds");
        asListYo.add("4 minutes");
        asListYo.add("2 minutes");
        ConfigurationNode cn = tcl.createEmptyNode().setValue(asListYo);
        WarningTimeList wtl = cn.getValue(TypeToken.of(WarningTimeList.class));

        Assert.assertNotNull(wtl);
        Assert.assertEquals("4 minutes", wtl.getWarningTimeAt(0).asString());
        Assert.assertEquals("2 minutes", wtl.getWarningTimeAt(1).asString());
        Assert.assertEquals("1 milliseconds", wtl.getWarningTimeAt(2).asString());
    }
}
