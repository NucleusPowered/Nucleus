/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.environment.commands;

import com.google.common.reflect.TypeToken;
import io.github.nucleuspowered.nucleus.Util;
import io.github.nucleuspowered.nucleus.modules.environment.EnvironmentPermissions;
import io.github.nucleuspowered.nucleus.modules.environment.parameter.WorldTimeArgument;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandContext;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandExecutor;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandResult;
import io.github.nucleuspowered.nucleus.scaffold.command.annotation.Command;
import io.github.nucleuspowered.nucleus.scaffold.command.annotation.CommandModifier;
import io.github.nucleuspowered.nucleus.scaffold.command.annotation.EssentialsEquivalent;
import io.github.nucleuspowered.nucleus.scaffold.command.modifier.CommandModifiers;
import io.github.nucleuspowered.nucleus.services.INucleusServiceCollection;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.util.annotation.NonnullByDefault;
import org.spongepowered.api.world.storage.WorldProperties;

import java.util.function.LongFunction;

@SuppressWarnings("UnstableApiUsage")
@EssentialsEquivalent(value = {"time", "day", "night"}, isExact = false, notes = "A time MUST be specified.")
@NonnullByDefault
@Command(
        aliases = {"set", "#settime", "#timeset"},
        basePermission = EnvironmentPermissions.BASE_TIME_SET,
        commandDescriptionKey = "time.set",
        parentCommand = TimeCommand.class,
        modifiers = {
                @CommandModifier(value = CommandModifiers.HAS_COOLDOWN, exemptPermission = EnvironmentPermissions.EXEMPT_COOLDOWN_TIME_SET),
                @CommandModifier(value = CommandModifiers.HAS_WARMUP, exemptPermission =  EnvironmentPermissions.EXEMPT_WARMUP_TIME_SET),
                @CommandModifier(value = CommandModifiers.HAS_COST, exemptPermission = EnvironmentPermissions.EXEMPT_COST_TIME_SET)
        }
)
public class SetTimeCommand implements ICommandExecutor<CommandSource> {
    private final String time = "time";
    private final String world = "world";

    @Override
    public CommandElement[] parameters(INucleusServiceCollection serviceCollection) {
        return new CommandElement[] {
            GenericArguments.optionalWeak(GenericArguments.onlyOne(GenericArguments.world(Text.of(this.world)))),
            GenericArguments.onlyOne(new WorldTimeArgument(Text.of(this.time), serviceCollection))
        };
    }

    @Override
    public ICommandResult execute(ICommandContext<? extends CommandSource> context) {
        WorldProperties pr = context.getWorldPropertiesOrFromSelf(this.world).orElseGet(
                () -> Sponge.getServer().getDefaultWorld().get()
        );

        LongFunction<Long> tick = context.requireOne(this.time, new TypeToken<LongFunction<Long>>() {});
        long time = tick.apply(pr.getWorldTime());
        pr.setWorldTime(time);
        context.sendMessage("command.settime.done2", pr.getWorldName(),
                Util.getTimeFromTicks(context.getServiceCollection().messageProvider(), time));
        return context.successResult();
    }
}
