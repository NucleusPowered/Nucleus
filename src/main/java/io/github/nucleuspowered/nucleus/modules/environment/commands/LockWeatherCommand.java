/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.environment.commands;

import io.github.nucleuspowered.nucleus.Nucleus;
import io.github.nucleuspowered.nucleus.internal.annotations.RunAsync;
import io.github.nucleuspowered.nucleus.internal.annotations.command.NoModifiers;
import io.github.nucleuspowered.nucleus.internal.annotations.command.Permissions;
import io.github.nucleuspowered.nucleus.internal.annotations.command.RegisterCommand;
import io.github.nucleuspowered.nucleus.internal.command.AbstractCommand;
import io.github.nucleuspowered.nucleus.internal.command.NucleusParameters;
import io.github.nucleuspowered.nucleus.internal.command.ReturnMessageException;
import io.github.nucleuspowered.nucleus.internal.traits.IDataManagerTrait;
import io.github.nucleuspowered.nucleus.modules.environment.EnvironmentKeys;
import io.github.nucleuspowered.storage.dataobjects.keyed.IKeyedDataObject;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.util.annotation.NonnullByDefault;
import org.spongepowered.api.world.storage.WorldProperties;

import java.util.Optional;

@Permissions
@RunAsync
@RegisterCommand({ "lockweather", "killweather" })
@NoModifiers
@NonnullByDefault
public class LockWeatherCommand extends AbstractCommand<CommandSource> implements IDataManagerTrait {

    private final String worldKey = "world";

    @Override
    public CommandElement[] getArguments() {
        return new CommandElement[] {
                GenericArguments.onlyOne(GenericArguments.optionalWeak(GenericArguments.world(Text.of(this.worldKey)))),
                NucleusParameters.OPTIONAL_ONE_TRUE_FALSE
        };
    }

    @Override
    public CommandResult executeCommand(CommandSource src, CommandContext args, Cause cause) throws ReturnMessageException {
        Optional<WorldProperties> world = getWorldProperties(src, this.worldKey, args);
        if (!world.isPresent()) {
            src.sendMessage(Nucleus.getNucleus().getMessageProvider().getTextMessageWithFormat("command.specifyworld"));
            return CommandResult.empty();
        }

        WorldProperties wp = world.get();
        try (IKeyedDataObject.Value<Boolean> vb = getWorldOnThread(wp.getUniqueId())
                .orElseThrow(() -> ReturnMessageException.fromKey("command.noworld", wp.getWorldName()))
                .getAndSet(EnvironmentKeys.LOCKED_WEATHER)) {
            boolean current = vb.getValue().orElse(false);
            boolean toggle = args.<Boolean>getOne(NucleusParameters.Keys.BOOL).orElse(!current);
            vb.setValue(toggle);
            if (toggle) {
                src.sendMessage(Nucleus.getNucleus().getMessageProvider().getTextMessageWithFormat("command.lockweather.locked", wp.getWorldName()));
            } else {
                src.sendMessage(Nucleus.getNucleus().getMessageProvider().getTextMessageWithFormat("command.lockweather.unlocked", wp.getWorldName()));
            }
        }

        return CommandResult.success();
    }
}
