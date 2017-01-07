/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.argumentparsers;

import com.google.common.collect.Lists;
import com.cronutils.model.Cron;
import com.cronutils.model.CronType;
import com.cronutils.model.definition.CronDefinitionBuilder;
import com.cronutils.parser.CronParser;
import io.github.nucleuspowered.nucleus.Nucleus;
import io.github.nucleuspowered.nucleus.util.ClockTime;
import io.github.nucleuspowered.nucleus.util.DelayedTimeType;
import io.github.nucleuspowered.nucleus.util.DelayedTimeValue;
import io.github.nucleuspowered.nucleus.util.WhenTime;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.ArgumentParseException;
import org.spongepowered.api.command.args.CommandArgs;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.text.Text;

import java.util.List;

import javax.annotation.Nullable;


/**
 * Allows Parsing of a DelayedTimeValue from a command argument
 */
public class DelayedTimeValueArgument extends CommandElement {

    private CronParser unixCronParser;
    private CronParser quartzCronParser;
    private CronParser cronjCronParser;

    /**
     * Construct a DelayedTimeValueArgument, used to initialize cron parsers
     *
     * @param key
     *  The key of the DelayedTimeValueArgument
     */
    public DelayedTimeValueArgument(@Nullable Text key) {
        super(key);
        this.unixCronParser = new CronParser(CronDefinitionBuilder.instanceDefinitionFor(CronType.UNIX));
        this.quartzCronParser = new CronParser(CronDefinitionBuilder.instanceDefinitionFor(CronType.QUARTZ));
        this.cronjCronParser = new CronParser(CronDefinitionBuilder.instanceDefinitionFor(CronType.CRON4J));
    }

    /**
     * Parses a DelayedTimeValue from a CommandSource/args
     *
     * @param source
     *  The CommandSource to parse from
     * @param args
     *  The arguments of the command
     * @return
     *  A delayed time value from the next arg
     * @throws ArgumentParseException
     *  If we could not parse the argument as a delayedtimevalue
     */
    @Override
    protected Object parseValue(CommandSource source, CommandArgs args) throws ArgumentParseException {
        String value = args.next();
        try {
            WhenTime asWhenTime = WhenTime.fromString(value);
            return new DelayedTimeValue(asWhenTime);
        } catch (Exception e1) { }
        try {
            ClockTime asClockTime = ClockTime.fromString(value);
            return new DelayedTimeValue(asClockTime);
        } catch (Exception e1) { }
        try {
            Cron asUnixCron = this.unixCronParser.parse(value);
            return new DelayedTimeValue(DelayedTimeType.UNIX_CRON, asUnixCron);
        } catch (Exception e1) { }
        try {
            Cron asQuartzCron = this.quartzCronParser.parse(value);
            return new DelayedTimeValue(DelayedTimeType.QUARTZ_CRON, asQuartzCron);
        } catch (Exception e1) { }
        try {
            Cron asJavaCron = this.cronjCronParser.parse(value);
            return new DelayedTimeValue(DelayedTimeType.CRON4J_CRON, asJavaCron);
        } catch (Exception e1) { }

        throw args.createError(Nucleus.getNucleus().getMessageProvider().getTextMessageWithFormat("args.delayedtimevalue.nomatch"));
    }

    @Override
    public List<String> complete(CommandSource src, CommandArgs args, CommandContext context) {
        return Lists.newArrayList();
    }

    @Override
    public Text getUsage(CommandSource src) {
        return Text.of(this.getKey(), "(every N minutes || HH:MM || cron)");
    }
}
