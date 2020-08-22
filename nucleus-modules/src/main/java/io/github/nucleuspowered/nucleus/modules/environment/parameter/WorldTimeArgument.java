/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.environment.parameter;

import com.google.common.collect.Maps;
import io.github.nucleuspowered.nucleus.services.INucleusServiceCollection;
import io.github.nucleuspowered.nucleus.services.interfaces.IMessageProviderService;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.ArgumentParseException;
import org.spongepowered.api.command.args.CommandArgs;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.text.Text;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.function.LongFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

/**
 * Parses an argument for world time. Could be either:
 *
 * <ul>
 *     <li>[0-23]h, for 24 hour time.</li>
 *     <li>[1-12][am|pm], for 12 hour time.</li>
 *     <li>[0-23999], for ticks.</li>
 * </ul>
 *
 * It could also be one of the pre-defined keywords:
 *
 * <ul>
 *     <li>dawn, sunrise: 0 ticks (6 am)</li>
 *     <li>day, daytime, morning: 1000 ticks (7 am)</li>
 *     <li>noon, afternoon: 6000 ticks (12 pm)</li>
 *     <li>dusk, evening, sunset: 12000 ticks (6 pm)</li>
 *     <li>night: 14000 ticks (8 pm)</li>
 *     <li>midnight: 18000 ticks (12 am)</li>
 * </ul>
 */
public class WorldTimeArgument extends CommandElement {

    private static final int TICKS_IN_DAY = 24000;
    private static final HashMap<String, LongFunction<Long>> tickAliases = Maps.newHashMap();
    private static final Pattern tfh = Pattern.compile("^(\\d{1,2})[hH]$");
    private static final Pattern ampm = Pattern.compile("^(\\d{1,2})(a[m]?|p[m]?)$", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private static final Pattern ticks = Pattern.compile("^(\\d{1,5})$");

    private static final RoundUp DAWN = new RoundUp(0);
    private static final RoundUp DAY = new RoundUp(1000);
    private static final RoundUp NOON = new RoundUp(6000);
    private static final RoundUp DUSK = new RoundUp(12000);
    private static final RoundUp NIGHT = new RoundUp(14000);
    private static final RoundUp MIDNIGHT = new RoundUp(18000);

    // Thanks to http://minecraft.gamepedia.com/Day-night_cycle
    static {
        tickAliases.put("dawn", DAWN);
        tickAliases.put("sunrise", DAWN);
        tickAliases.put("morning", DAY);
        tickAliases.put("day", DAY);
        tickAliases.put("daytime", DAY);
        tickAliases.put("noon", NOON);
        tickAliases.put("afternoon", NOON);
        tickAliases.put("dusk", DUSK);
        tickAliases.put("sunset", DUSK);
        tickAliases.put("evening", DUSK);
        tickAliases.put("night", NIGHT);
        tickAliases.put("midnight", MIDNIGHT);
    }

    private final IMessageProviderService messageProvider;

    public WorldTimeArgument(@Nullable Text key, INucleusServiceCollection serviceCollection) {
        super(key);
        this.messageProvider = serviceCollection.messageProvider();
    }

    private LongFunction<Long> getValue(CommandSource source, String arg, CommandArgs args) throws ArgumentParseException {
        if (tickAliases.containsKey(arg)) {
            return tickAliases.get(arg);
        }

        // <number>h
        Matcher m1 = tfh.matcher(arg);
        if (m1.matches()) {
            // Get the number, multiply by 1000, return.
            long i = Long.parseLong(m1.group(1));
            if (i > 23 || i < 0) {
                throw args.createError(this.messageProvider.getMessageFor(source, "args.worldtime.24herror"));
            }

            i -= 6;
            if (i < 0) {
                i += 24;
            }

            final long res = i * 1000;
            return x -> res;
        }

        // <number>am,pm
        Matcher m2 = ampm.matcher(arg);
        if (m2.matches()) {
            // Get the number, multiply by 1000, return.
            int i = Integer.parseInt(m2.group(1));
            if (i > 12 || i < 1) {
                throw args.createError(this.messageProvider.getMessageFor(source, "args.worldtime.12herror"));
            }

            // Modify to 24 hour time, based on am/pm
            String id = m2.group(2).toLowerCase();
            if (id.startsWith("p") && i < 12) {
                // 11 pm -> 23, 12 pm -> 12.
                i += 12;
            } else if (id.startsWith("a") && i == 12) {
                // 12 am -> 0
                i = 0;
            }

            // Adjust for Minecraft time.
            i -= 6;
            if (i < 0) {
                i += 24;
            }

            final long res = i * 1000;
            return x -> res;
        }

        // 0 -> 23999
        if (ticks.matcher(arg).matches()) {
            final long i = Long.parseLong(arg);
            if (i >= 0 && i <= 23999) {
                return x -> i;
            }

            throw args.createError(this.messageProvider.getMessageFor(source, "args.worldtime.ticks"));
        }

        throw args.createError(this.messageProvider.getMessageFor(source, "args.worldtime.error", arg));
    }

    @Nullable
    @Override
    protected Object parseValue(CommandSource source, CommandArgs args) throws ArgumentParseException {
        String arg = args.next().toLowerCase();
        return getValue(source, arg, args);
    }

    @Override
    public List<String> complete(CommandSource src, CommandArgs args, CommandContext context) {
        try {
            String a = args.peek().toLowerCase();
            return tickAliases.keySet().stream().filter(x -> x.startsWith(a)).collect(Collectors.toList());
        } catch (ArgumentParseException e) {
            return new ArrayList<>(tickAliases.keySet());
        }
    }

    private static class RoundUp implements LongFunction<Long> {

        private final long target;

        private RoundUp(long target) {
            this.target = target;
        }

        @Override
        public final Long apply(long value) {
            // 23999 is the max tick number
            // Get the time of day
            long remainder = value % TICKS_IN_DAY;

            if (this.target == remainder) {
                // no advancement
                return value;
            }

            if (this.target < remainder) {
                // target is below remainder, we need to get to target on next day
                value += TICKS_IN_DAY;
            }

            // remove remainder, add target
            return value - remainder + this.target;
        }
    }
}
