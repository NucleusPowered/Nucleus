/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.experience.commands;

import io.github.nucleuspowered.nucleus.modules.experience.ExperiencePermissions;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandContext;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandExecutor;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandResult;
import io.github.nucleuspowered.nucleus.scaffold.command.NucleusParameters;
import io.github.nucleuspowered.nucleus.scaffold.command.annotation.Command;
import io.github.nucleuspowered.nucleus.scaffold.command.annotation.EssentialsEquivalent;
import io.github.nucleuspowered.nucleus.services.INucleusServiceCollection;
import io.github.nucleuspowered.nucleus.services.interfaces.IMessageProviderService;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.data.manipulator.mutable.entity.ExperienceHolderData;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.gamemode.GameMode;
import org.spongepowered.api.entity.living.player.gamemode.GameModes;
import org.spongepowered.api.util.annotation.NonnullByDefault;

import java.util.Optional;

@Command(aliases = {"exp", "experience", "xp"},
        basePermission = ExperiencePermissions.BASE_EXP,
        commandDescriptionKey = "exp")
@EssentialsEquivalent({"exp", "xp"})
@NonnullByDefault
public class ExperienceCommand implements ICommandExecutor<CommandSource> {

    static final String experienceKey = "experience";
    static final String levelKey = "level";

    @Override
    public CommandElement[] parameters(INucleusServiceCollection serviceCollection) {
        return new CommandElement[] {
                NucleusParameters.OPTIONAL_ONE_PLAYER.get(serviceCollection)
        };
    }

    @Override public ICommandResult execute(ICommandContext<? extends CommandSource> context) throws CommandException {
        Player pl = context.getPlayerFromArgs();

        ExperienceHolderData ehd = pl.get(ExperienceHolderData.class).get();
        int exp = ehd.totalExperience().get();
        int lv = ehd.level().get();

        context.getServiceCollection().messageProvider()
                .sendMessageTo(context.getCommandSource(), "command.exp.info", pl.getName(), exp, lv);
        return context.successResult();
    }

    static ICommandResult tellUserAboutExperience(ICommandContext<? extends CommandSource> context, Player pl, boolean isSuccess) throws CommandException {
        if (!isSuccess) {
            return context.errorResult("command.exp.set.error");
        }

        int exp = pl.get(Keys.TOTAL_EXPERIENCE).get();
        int newLvl = pl.get(Keys.EXPERIENCE_LEVEL).get();

        CommandSource src = context.getCommandSource();
        IMessageProviderService messageProviderService = context.getServiceCollection().messageProvider();
        if (!src.equals(pl)) {
            messageProviderService.sendMessageTo(
                            src,
                            "command.exp.set.new.other",
                            pl.getName(),
                            exp,
                            newLvl);
        }


        messageProviderService.sendMessageTo(pl, "command.exp.set.new.self", String.valueOf(exp), String.valueOf(newLvl));
        return context.successResult();
    }

    static Optional<ICommandResult> checkGameMode(ICommandContext<? extends CommandSource> source, Player pl) throws CommandException {
        GameMode gm = pl.get(Keys.GAME_MODE).orElse(GameModes.SURVIVAL);
        if (gm == GameModes.CREATIVE || gm == GameModes.SPECTATOR) {
            return Optional.of(source.errorResult("command.exp.gamemode", pl.getName()));
        }

        return Optional.empty();
    }
}
