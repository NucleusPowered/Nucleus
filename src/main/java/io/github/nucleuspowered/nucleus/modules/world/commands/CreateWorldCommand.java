/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.world.commands;

import io.github.nucleuspowered.nucleus.Nucleus;
import io.github.nucleuspowered.nucleus.argumentparsers.DifficultyArgument;
import io.github.nucleuspowered.nucleus.argumentparsers.ImprovedGameModeArgument;
import io.github.nucleuspowered.nucleus.internal.annotations.Permissions;
import io.github.nucleuspowered.nucleus.internal.annotations.RegisterCommand;
import io.github.nucleuspowered.nucleus.internal.command.ReturnMessageException;
import io.github.nucleuspowered.nucleus.internal.permissions.SuggestedLevel;
import org.spongepowered.api.CatalogTypes;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.entity.living.player.gamemode.GameMode;
import org.spongepowered.api.entity.living.player.gamemode.GameModes;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.world.DimensionType;
import org.spongepowered.api.world.DimensionTypes;
import org.spongepowered.api.world.GeneratorType;
import org.spongepowered.api.world.GeneratorTypes;
import org.spongepowered.api.world.World;
import org.spongepowered.api.world.WorldArchetype;
import org.spongepowered.api.world.difficulty.Difficulties;
import org.spongepowered.api.world.difficulty.Difficulty;
import org.spongepowered.api.world.gen.WorldGeneratorModifier;
import org.spongepowered.api.world.storage.WorldProperties;

import java.util.Collection;
import java.util.Optional;

@Permissions(prefix = "world", suggestedLevel = SuggestedLevel.ADMIN)
@RegisterCommand(value = {"create"}, subcommandOf = WorldCommand.class)
public class CreateWorldCommand extends io.github.nucleuspowered.nucleus.internal.command.AbstractCommand<CommandSource> {

    private final String name = "name";
    private final String dimension = "dimension";
    private final String generator = "generator";
    private final String gamemode = "gamemode";
    private final String difficulty = "difficulty";
    private final String modifier = "modifier";
    private final String seed = "seed";

    @Override
    public CommandElement[] getArguments() {
        return new CommandElement[] {
            GenericArguments.flags()
                .valueFlag(GenericArguments.onlyOne(GenericArguments.catalogedElement(Text.of(dimension), CatalogTypes.DIMENSION_TYPE)), "d", "-" + dimension)
                .valueFlag(GenericArguments.onlyOne(GenericArguments.catalogedElement(Text.of(generator), CatalogTypes.GENERATOR_TYPE)), "g", "-" + generator)
                .valueFlag(GenericArguments.catalogedElement(Text.of(modifier), CatalogTypes.WORLD_GENERATOR_MODIFIER), "m", "-" + modifier)
                .valueFlag(GenericArguments.onlyOne(GenericArguments.longNum(Text.of(seed))), "s", "-" + seed)
                .valueFlag(GenericArguments.onlyOne(new ImprovedGameModeArgument(Text.of(gamemode))), "-gm", "-" + gamemode)
                .valueFlag(GenericArguments.onlyOne(new DifficultyArgument(Text.of(difficulty))), "-di", "-" + difficulty)
                .buildWith(GenericArguments.onlyOne(GenericArguments.string(Text.of(name))))
        };
    }

    @Override
    public CommandResult executeCommand(final CommandSource src, CommandContext args) throws Exception {
        String nameInput = args.<String>getOne(name).get();
        DimensionType dimensionInput = args.<DimensionType>getOne(dimension).orElse(DimensionTypes.OVERWORLD);
        GeneratorType generatorInput = args.<GeneratorType>getOne(generator).orElse(GeneratorTypes.DEFAULT);
        GameMode gamemodeInput = args.<GameMode>getOne(gamemode).orElse(GameModes.SURVIVAL);
        Difficulty difficultyInput = args.<Difficulty>getOne(difficulty).orElse(Difficulties.NORMAL);
        Collection<WorldGeneratorModifier> modifiers = args.getAll(modifier);
        Optional<Long> seedInput = args.getOne(seed);

        if (Sponge.getServer().getWorld(nameInput).isPresent()) {
            throw new ReturnMessageException(plugin.getMessageProvider().getTextMessageWithFormat("command.world.create.exists", nameInput));
        }

        src.sendMessage(plugin.getMessageProvider().getTextMessageWithFormat("command.world.create.begin", nameInput));
        src.sendMessage(plugin.getMessageProvider().getTextMessageWithFormat("command.world.create.newparams",
            dimensionInput.getName(),
            generatorInput.getName(),
            modifierString(modifiers),
            gamemodeInput.getName(),
            difficultyInput.getName()));

            WorldArchetype.Builder worldSettingsBuilder = WorldArchetype.builder().enabled(true)
                .loadsOnStartup(true).keepsSpawnLoaded(true).dimension(dimensionInput).generator(generatorInput).gameMode(gamemodeInput);
        if (!modifiers.isEmpty()) {
            worldSettingsBuilder.generatorModifiers(modifiers.toArray(new WorldGeneratorModifier[modifiers.size()]));
        }

        if (seedInput.isPresent()) {
            worldSettingsBuilder.seed(seedInput.get());
        }

        WorldArchetype wa = worldSettingsBuilder.build(nameInput.toLowerCase(), nameInput);
        WorldProperties worldProperties = Sponge.getGame().getServer().createWorldProperties(nameInput, wa);
        Optional<World> world = Sponge.getGame().getServer().loadWorld(worldProperties);

        if (world.isPresent()) {
            world.get().getProperties().setDifficulty(difficultyInput);
            src.sendMessage(plugin.getMessageProvider().getTextMessageWithFormat("command.world.create.success", nameInput));
            return CommandResult.success();
        } else {
            throw new ReturnMessageException(plugin.getMessageProvider().getTextMessageWithFormat("command.world.create.worldfailedtoload", nameInput));
        }
    }

    static String modifierString(Collection<WorldGeneratorModifier> cw) {
        if (cw.isEmpty()) {
            return Nucleus.getNucleus().getMessageProvider().getMessageWithFormat("standard.none");
        }

        StringBuilder sb = new StringBuilder();
        cw.forEach(x -> {
            if (sb.length() > 0) {
                sb.append(", ");
            }

            sb.append(x.getName());
        });

        return sb.toString();
    }
}
