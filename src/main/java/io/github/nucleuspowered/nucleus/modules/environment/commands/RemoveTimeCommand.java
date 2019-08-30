package io.github.nucleuspowered.nucleus.modules.environment.commands;

import io.github.nucleuspowered.nucleus.Nucleus;
import io.github.nucleuspowered.nucleus.Util;
import io.github.nucleuspowered.nucleus.internal.annotations.command.Permissions;
import io.github.nucleuspowered.nucleus.internal.annotations.command.RegisterCommand;
import io.github.nucleuspowered.nucleus.internal.command.AbstractCommand;
import io.github.nucleuspowered.nucleus.internal.docgen.annotations.EssentialsEquivalent;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.util.annotation.NonnullByDefault;
import org.spongepowered.api.world.storage.WorldProperties;

@Permissions(prefix = "time")
@RegisterCommand(value = "remove", subcommandOf = TimeCommand.class, rootAliasRegister = { "removetime", "timeremove"})
@EssentialsEquivalent(value = {"time", "day", "night"}, isExact = false, notes = "A time MUST be specified.")
@NonnullByDefault
public class RemoveTimeCommand extends AbstractCommand<CommandSource> {
    private final String time = "time";
    private final String world = "world";

    @Override
    public CommandElement[] getArguments() {
        return new CommandElement[] {
                GenericArguments.optionalWeak(GenericArguments.onlyOne(GenericArguments.world(Text.of(this.world)))),
                GenericArguments.onlyOne(GenericArguments.longNum(Text.of(this.time)))
        };
    }

    @Override
    public CommandResult executeCommand(CommandSource src, CommandContext args, Cause cause) {
        WorldProperties pr = getWorldPropertiesOrDefault(src, this.world, args);

        long tick = args.<Long>getOne(this.time).get();
        long time = pr.getWorldTime() - tick;
        pr.setWorldTime(time);
        src.sendMessage(Nucleus.getNucleus().getMessageProvider().getTextMessageWithFormat("command.removetime.done2",
                pr.getWorldName(),
                tick,
                Util.getTimeFromTicks(time)));
        return CommandResult.success();
    }
}