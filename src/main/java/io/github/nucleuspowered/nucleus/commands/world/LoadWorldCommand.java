/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.commands.world;

import io.github.nucleuspowered.nucleus.Util;
import io.github.nucleuspowered.nucleus.internal.CommandBase;
import io.github.nucleuspowered.nucleus.internal.annotations.Permissions;
import io.github.nucleuspowered.nucleus.internal.annotations.RegisterCommand;
import io.github.nucleuspowered.nucleus.internal.permissions.SuggestedLevel;
import org.spongepowered.api.CatalogTypes;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.entity.living.player.gamemode.GameMode;
import org.spongepowered.api.entity.living.player.gamemode.GameModes;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.world.*;
import org.spongepowered.api.world.difficulty.Difficulties;
import org.spongepowered.api.world.difficulty.Difficulty;
import org.spongepowered.api.world.storage.WorldProperties;

import java.util.Optional;

/**
 * Loads worlds.
 *
 * Command Usage: /world load [name] [dimension] [generator] [gamemode]
 * [difficulty] Permission: nucleus.world.load.base
 */
@Permissions(root = "world", suggestedLevel = SuggestedLevel.ADMIN)
@RegisterCommand(value = {"load"}, subcommandOf = WorldCommand.class)
public class LoadWorldCommand extends CommandBase<CommandSource> {

    private final String name = "name";
    private final String dimension = "dimension";
    private final String generator = "generator";
    private final String gamemode = "gamemode";
    private final String difficulty = "difficulty";

    @Override
    public CommandSpec createSpec() {
        return CommandSpec.builder().description(Text.of("Load World Command"))
                .arguments(GenericArguments.seq(GenericArguments.onlyOne(GenericArguments.string(Text.of(name))),
                        GenericArguments.optional(
                                GenericArguments.onlyOne(GenericArguments.catalogedElement(Text.of(dimension), CatalogTypes.DIMENSION_TYPE))),
                GenericArguments
                        .optional(GenericArguments.onlyOne(GenericArguments.catalogedElement(Text.of(generator), CatalogTypes.GENERATOR_TYPE))),
                GenericArguments.optional(GenericArguments.onlyOne(GenericArguments.catalogedElement(Text.of(gamemode), CatalogTypes.GAME_MODE))),
                GenericArguments.optional(GenericArguments.onlyOne(GenericArguments.catalogedElement(Text.of(difficulty), CatalogTypes.DIFFICULTY)))))
                .executor(this).build();
    }

    @Override
    public CommandResult executeCommand(final CommandSource src, CommandContext args) throws Exception {
        String nameInput = args.<String>getOne(name).get();
        Optional<DimensionType> dimensionInput = args.<DimensionType>getOne(dimension);
        Optional<GeneratorType> generatorInput = args.<GeneratorType>getOne(generator);
        Optional<Difficulty> difficultyInput = args.<Difficulty>getOne(difficulty);
        Optional<GameMode> gamemodeInput = args.<GameMode>getOne(gamemode);
        Difficulty difficulty = difficultyInput.orElse(Difficulties.NORMAL);
        DimensionType dimension = dimensionInput.orElse(DimensionTypes.OVERWORLD);
        GeneratorType generator = generatorInput.orElse(GeneratorTypes.OVERWORLD);
        GameMode gamemode = gamemodeInput.orElse(GameModes.SURVIVAL);

        src.sendMessage(Util.getTextMessageWithFormat("command.world.load.begin", nameInput));

        WorldCreationSettings worldSettings = Sponge.getRegistry().createBuilder(WorldCreationSettings.Builder.class).name(nameInput).enabled(true)
                .loadsOnStartup(true).keepsSpawnLoaded(true).dimension(dimension).generator(generator).gameMode(gamemode).build();

        Optional<WorldProperties> worldProperties = Sponge.getGame().getServer().createWorldProperties(worldSettings);

        if (worldProperties.isPresent()) {
            Optional<World> world = Sponge.getGame().getServer().loadWorld(worldProperties.get());

            if (world.isPresent()) {
                world.get().getProperties().setDifficulty(difficulty);
                src.sendMessage(Util.getTextMessageWithFormat("command.world.load.success", nameInput));
            } else {
                src.sendMessage(Util.getTextMessageWithFormat("command.world.load.fail", nameInput));
            }
        } else {
            src.sendMessage(Util.getTextMessageWithFormat("command.world.load.fail", nameInput));
        }

        return CommandResult.success();
    }
}
