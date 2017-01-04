/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.configurate.typeserialisers;

import com.google.common.reflect.TypeToken;
import com.cronutils.model.Cron;
import com.cronutils.model.CronType;
import com.cronutils.model.definition.CronDefinitionBuilder;
import com.cronutils.parser.CronParser;
import io.github.nucleuspowered.nucleus.util.ClockTime;
import io.github.nucleuspowered.nucleus.util.DelayedTimeType;
import io.github.nucleuspowered.nucleus.util.DelayedTimeValue;
import io.github.nucleuspowered.nucleus.util.WhenTime;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.SimpleConfigurationNode;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import ninja.leaping.configurate.objectmapping.serialize.TypeSerializer;

import java.util.Map;

/**
 * Manages serialization of a DelayedTimeValue
 */
public class DelayedTimeValueSerialiser implements TypeSerializer<DelayedTimeValue> {

    private CronParser unixCronParser;
    private CronParser quartzCronParser;
    private CronParser cronjCronParser;

    /**
     * Constructs a DelayedTimeValueSerialiser, initializes the cron parsers
     */
    public DelayedTimeValueSerialiser() {
        this.unixCronParser = new CronParser(CronDefinitionBuilder.instanceDefinitionFor(CronType.UNIX));
        this.quartzCronParser = new CronParser(CronDefinitionBuilder.instanceDefinitionFor(CronType.QUARTZ));
        this.cronjCronParser = new CronParser(CronDefinitionBuilder.instanceDefinitionFor(CronType.CRON4J));
    }

    @Override
    public DelayedTimeValue deserialize(TypeToken<?> type, ConfigurationNode value) throws ObjectMappingException {
        if (value.getValue() instanceof String) {
            String node = value.getString();
            try {
                Cron asUnixCron = this.unixCronParser.parse(node);
                return new DelayedTimeValue(DelayedTimeType.UNIX_CRON, asUnixCron);
            } catch (Exception e1) {
            }
            try {
                Cron asQuartzCron = this.quartzCronParser.parse(node);
                return new DelayedTimeValue(DelayedTimeType.QUARTZ_CRON, asQuartzCron);
            } catch (Exception e1) {
            }
            try {
                Cron asJavaCron = this.cronjCronParser.parse(node);
                return new DelayedTimeValue(DelayedTimeType.CRON4J_CRON, asJavaCron);
            } catch (Exception e1) {
            }
            try {
                WhenTime asWhenTime = WhenTime.fromString(node);
                return new DelayedTimeValue(asWhenTime);
            } catch (Exception e1) {
            }
            try {
                ClockTime asClockTime = ClockTime.fromString(node);
                return new DelayedTimeValue(asClockTime);
            } catch (Exception e1) {
            }

            throw new ObjectMappingException("Couldn't find a delayedtimevalue amidst the string");
        }

        if (value.getValue() instanceof Map) {
            Map node = value.getChildrenMap();
            if (node.containsKey("type") && node.containsKey("statement")) {
                Object typeObject = node.get("type");
                Object statementObject = node.get("statement");
                if (!(typeObject instanceof SimpleConfigurationNode && statementObject instanceof
                        SimpleConfigurationNode)) {
                    throw new IllegalArgumentException("Failed to parse map type/statement");
                }
                SimpleConfigurationNode asUnparsedType = (SimpleConfigurationNode) typeObject;
                SimpleConfigurationNode asUnparsedStatement = (SimpleConfigurationNode) statementObject;
                try {
                    String asStringType = asUnparsedType.getString();
                    String asStringStatement = asUnparsedStatement.getString();
                    DelayedTimeType dtt = DelayedTimeType.fromString(asStringType);
                    if (dtt == null) {
                        throw new ObjectMappingException("Null DelayedTimeType");
                    }
                    switch (dtt) {
                        case QUARTZ_CRON:
                            Cron quartzCron = this.quartzCronParser.parse(asStringStatement);
                            return new DelayedTimeValue(DelayedTimeType.QUARTZ_CRON, quartzCron);
                        case UNIX_CRON:
                            Cron unixCron = this.unixCronParser.parse(asStringStatement);
                            return new DelayedTimeValue(DelayedTimeType.UNIX_CRON, unixCron);
                        case CRON4J_CRON:
                            Cron javaCron = this.cronjCronParser.parse(asStringStatement);
                            return new DelayedTimeValue(DelayedTimeType.CRON4J_CRON, javaCron);
                        case CLOCK_TIME:
                            ClockTime clockTime = ClockTime.fromString(asStringStatement);
                            return new DelayedTimeValue(clockTime);
                        case WHEN_TIME:
                            WhenTime whenTime = WhenTime.fromString(asStringStatement);
                            return new DelayedTimeValue(whenTime);
                        default:
                            throw new ObjectMappingException("Failed to find DelayedTimeType");
                    }
                } catch (Exception e1) {
                    System.err.println(e1.getMessage());
                    for (StackTraceElement ste : e1.getStackTrace()) {
                        System.err.println(ste);
                    }
                    throw new ObjectMappingException(e1.getCause());
                }
            }
        }

        throw new ObjectMappingException("No Possible Delayed Time type.");
    }

    @Override
    public void serialize(TypeToken<?> type, DelayedTimeValue obj, ConfigurationNode value)
            throws ObjectMappingException {
        value.setValue(obj.asString());
    }
}
