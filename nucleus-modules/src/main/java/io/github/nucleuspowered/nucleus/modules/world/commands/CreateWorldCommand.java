/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.world.commands;

import io.github.nucleuspowered.nucleus.modules.world.WorldPermissions;
import io.github.nucleuspowered.nucleus.modules.world.config.WorldConfig;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandContext;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandExecutor;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandResult;
import io.github.nucleuspowered.nucleus.scaffold.command.annotation.Command;
import io.github.nucleuspowered.nucleus.scaffold.command.parameter.ImprovedCatalogTypeArgument;
import io.github.nucleuspowered.nucleus.scaffold.command.parameter.ImprovedGameModeArgument;
import io.github.nucleuspowered.nucleus.services.INucleusServiceCollection;
import io.github.nucleuspowered.nucleus.services.interfaces.IMessageProviderService;
import io.github.nucleuspowered.nucleus.services.interfaces.IReloadableService;
import org.spongepowered.api.CatalogType;
import org.spongepowered.api.CatalogTypes;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.ArgumentParseException;
import org.spongepowered.api.command.args.CommandArgs;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.data.DataContainer;
import org.spongepowered.api.data.DataQuery;
import org.spongepowered.api.data.persistence.DataFormats;
import org.spongepowered.api.entity.living.player.gamemode.GameMode;
import org.spongepowered.api.entity.living.player.gamemode.GameModes;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.util.annotation.NonnullByDefault;
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

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import javax.annotation.Nullable;

@NonnullByDefault
@Command(
        aliases = {"create"},
        basePermission = WorldPermissions.BASE_WORLD_CREATE,
        commandDescriptionKey = "world.create",
        parentCommand = WorldCommand.class
)
public class CreateWorldCommand implements ICommandExecutor<CommandSource>, IReloadableService.Reloadable {

    private final DataQuery uuidLeast = DataQuery.of("SpongeData", "UUIDLeast");
    private final DataQuery uuidMost = DataQuery.of("SpongeData", "UUIDMost");
    private final DataQuery levelName = DataQuery.of("Data", "LevelName");
    private final DataQuery toId = DataQuery.of("SpongeData", "dimensionId");

    private final String preset = "preset";
    private final String name = "name";
    private final String dimension = "dimension";
    private final String generator = "generator";
    private final String gamemode = "gamemode";
    private final String difficulty = "difficulty";
    private final String modifier = "modifier";
    private final String seed = "seed";
    @Nullable private Long worldBorderDefault;

    @Override
    public CommandElement[] parameters(INucleusServiceCollection serviceCollection) {
        return new CommandElement[] {
            GenericArguments.flags()
                .valueFlag(GenericArguments.onlyOne(new ImprovedCatalogTypeArgument(Text.of(preset), WorldArchetype.class, serviceCollection)), "p", "-" + preset)
                .valueFlag(GenericArguments.onlyOne(
                        new ExtendedDimensionArgument(Text.of(dimension), serviceCollection.messageProvider())), "d", "-" + dimension)
                .valueFlag(GenericArguments.onlyOne(new ImprovedCatalogTypeArgument(Text.of(generator), CatalogTypes.GENERATOR_TYPE,
                        serviceCollection)), "g", "-" + generator)
                .valueFlag(new ImprovedCatalogTypeArgument(Text.of(modifier), CatalogTypes.WORLD_GENERATOR_MODIFIER, serviceCollection), "m", "-" + modifier)
                .valueFlag(GenericArguments.onlyOne(GenericArguments.longNum(Text.of(seed))), "s", "-" + seed)
                .valueFlag(GenericArguments.onlyOne(new ImprovedGameModeArgument(Text.of(gamemode), serviceCollection)), "-gm", "-" + gamemode)
                .valueFlag(GenericArguments.onlyOne(new ImprovedCatalogTypeArgument(Text.of(difficulty), CatalogTypes.DIFFICULTY, serviceCollection)), "-di", "-" + difficulty)
                .flag("n", "-nostructures")
                .flag("i")
                .valueFlag(GenericArguments.bool(Text.of("l")), "l", "-loadonstartup")
                .valueFlag(GenericArguments.bool(Text.of("k")), "k", "-keepspawnloaded")
                .valueFlag(GenericArguments.bool(Text.of("c")), "c", "-allowcommands")
                .valueFlag(GenericArguments.bool(Text.of("b")), "b", "-bonuschest")
                .buildWith(GenericArguments.onlyOne(GenericArguments.string(Text.of(name))))
        };
    }

    @Override public ICommandResult execute(ICommandContext<? extends CommandSource> context) throws CommandException {
        String nameInput = context.requireOne(this.name, String.class);
        Optional<DimensionType> dimensionInput = context.getOne(this.dimension, DimensionType.class);
        Optional<GeneratorType> generatorInput = context.getOne(this.generator, GeneratorType.class);
        Optional<GameMode> gamemodeInput = context.getOne(this.gamemode, GameMode.class);
        Optional<Difficulty> difficultyInput = context.getOne(this.difficulty, Difficulty.class);
        Collection<WorldGeneratorModifier> modifiers = context.getAll(this.modifier, WorldGeneratorModifier.class);
        Optional<Long> seedInput = context.getOne(this.seed, Long.class);
        boolean genStructures = !context.hasAny("n");
        boolean loadOnStartup = !context.hasAny("l") || context.getOne("l", Boolean.class).orElse(true);
        boolean keepSpawnLoaded = !context.hasAny("k") || context.getOne("k", Boolean.class).orElse(true);
        boolean allowCommands = !context.hasAny("c") || context.getOne("c", Boolean.class).orElse(true);
        boolean bonusChest = !context.hasAny("b") || context.getOne("b", Boolean.class).orElse(true);

        if (Sponge.getServer().getAllWorldProperties().stream().anyMatch(x -> x.getWorldName().equalsIgnoreCase(nameInput))) {
            return context.errorResult("command.world.create.exists", nameInput);
        }

        // Does the world exist?
        Path worldPath = Sponge.getGame().getGameDirectory().resolve("world");
        Path worldDir = worldPath.resolve(nameInput);
        if (!context.hasAny("i") && Files.exists(worldDir)) {
            context.errorResult("command.world.import.noexist", nameInput);
        }

        if (context.hasAny("i") && Files.exists(worldDir)) {
            onImport(context, worldDir, nameInput);
        }


        WorldArchetype.Builder worldSettingsBuilder = WorldArchetype.builder().enabled(true);

        if (context.hasAny(this.preset)) {
            WorldArchetype preset1 = context.requireOne(this.preset, WorldArchetype.class);
            worldSettingsBuilder.from(preset1);
            dimensionInput.ifPresent(worldSettingsBuilder::dimension);
            generatorInput.ifPresent(worldSettingsBuilder::generator);
            gamemodeInput.ifPresent(worldSettingsBuilder::gameMode);
            difficultyInput.ifPresent(worldSettingsBuilder::difficulty);
            if (!modifiers.isEmpty()) {
                modifiers.addAll(preset1.getGeneratorModifiers());
                worldSettingsBuilder.generatorModifiers(modifiers.toArray(new WorldGeneratorModifier[0]));
            }
        } else {
            worldSettingsBuilder
                .dimension(dimensionInput.orElse(DimensionTypes.OVERWORLD))
                .generator(generatorInput.orElse(GeneratorTypes.DEFAULT))
                .gameMode(gamemodeInput.orElse(GameModes.SURVIVAL))
                .difficulty(difficultyInput.orElse(Difficulties.NORMAL));

                if (!modifiers.isEmpty()) {
                    worldSettingsBuilder.generatorModifiers(modifiers.toArray(new WorldGeneratorModifier[0]));
                }
        }

        worldSettingsBuilder.loadsOnStartup(loadOnStartup)
        .keepsSpawnLoaded(keepSpawnLoaded)
        .usesMapFeatures(genStructures)
        .commandsAllowed(allowCommands)
        .generateBonusChest(bonusChest);

        seedInput.ifPresent(worldSettingsBuilder::seed);

        WorldArchetype wa = worldSettingsBuilder.build(nameInput.toLowerCase(), nameInput);

        context.sendMessage("command.world.create.begin", nameInput);
        context.sendMessage("command.world.create.newparams",
                wa.getDimensionType().getName(),
                wa.getGeneratorType().getName(),
                modifierString(context, modifiers),
                wa.getGameMode().getName(),
                wa.getDifficulty().getName());
        context.sendMessage("command.world.create.newparams2",
                String.valueOf(loadOnStartup),
                String.valueOf(keepSpawnLoaded),
                String.valueOf(genStructures),
                String.valueOf(allowCommands),
                String.valueOf(bonusChest));

        WorldProperties worldProperties;
        try {
            worldProperties = Sponge.getGame().getServer().createWorldProperties(nameInput, wa);
        } catch (IOException e) {
            e.printStackTrace();
            return context.errorResultLiteral(Text.of(TextColors.RED, e.getMessage()));
        }

        if (this.worldBorderDefault != null && this.worldBorderDefault > 0) {
            worldProperties.setWorldBorderDiameter(this.worldBorderDefault);
        }

        worldProperties.setDifficulty(wa.getDifficulty());

        if (!Sponge.getServer().saveWorldProperties(worldProperties)) {
            return context.errorResult("command.world.create.couldnotsave", nameInput);
        }

        Optional<World> world = Sponge.getGame().getServer().loadWorld(worldProperties);

        if (world.isPresent()) {
            world.get().getProperties().setDifficulty(wa.getDifficulty());
            context.sendMessage("command.world.create.success", nameInput);
            return context.successResult();
        } else {
            return context.errorResult("command.world.create.worldfailedtoload", nameInput);
        }
    }

    private OutputStream getOutput(boolean gzip, Path file) throws IOException {
        OutputStream os = Files.newOutputStream(file);
        if (gzip) {
            return new GZIPOutputStream(os, true);
        }

        return os;
    }

    private void onImport(ICommandContext<? extends CommandSource> context, Path world, String name) {
        // Get the file
        Path level = world.resolve("level.dat");
        Path levelSponge = world.resolve("level_sponge.dat");

        if (Files.exists(level)) {
            DataContainer dc;
            boolean gz = false;
            try {
                try (InputStream is = Files.newInputStream(level, StandardOpenOption.READ)) {
                    // Open it, get the Dimension ID
                    dc = DataFormats.NBT.readFrom(is);
                } catch (EOFException ex) {
                    try (GZIPInputStream gzip = new GZIPInputStream(Files.newInputStream(level, StandardOpenOption.READ))) {
                        dc = DataFormats.NBT.readFrom(gzip);
                        gz = true;
                    }
                }

                Files.copy(level, world.resolve("level.dat.nbak"), StandardCopyOption.REPLACE_EXISTING);
                dc.set(this.levelName, name);
                try (OutputStream os = getOutput(gz, level)) {
                    DataFormats.NBT.writeTo(os, dc);
                    os.flush();
                }
            } catch (IOException e) {
                e.printStackTrace();
                context.getServiceCollection().logger().warn("Could not read the level.dat. Ignoring.");
            }
        }

        if (Files.exists(levelSponge)) {
            DataContainer dc;
            boolean gz = false;
            try {
                try (InputStream is = Files.newInputStream(levelSponge, StandardOpenOption.READ)) {
                    // Open it, get the Dimension ID
                    dc = DataFormats.NBT.readFrom(is);
                } catch (EOFException ex) {
                    try (GZIPInputStream gzip = new GZIPInputStream(Files.newInputStream(levelSponge, StandardOpenOption.READ))) {
                        dc = DataFormats.NBT.readFrom(gzip);
                        gz = true;
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
                context.getServiceCollection().logger().warn("Could not read the level_sponge.dat. Ignoring.");
                return;
            }

            // For each world, get the dim ID.
            Set<Integer> si = Sponge.getServer().getAllWorldProperties().stream()
                    .map(x -> x.getAdditionalProperties().getInt(this.toId).orElse(0))
                    .collect(Collectors.toSet());

            if (!dc.getInt(this.toId).map(si::contains).orElse(false)) {
                for (int i = 2; i < Integer.MAX_VALUE; i++) {
                    if (!si.contains(i)) {
                        dc.set(this.toId, i);
                        break;
                    }
                }
            }

            UUID uuid = UUID.randomUUID();
            dc.set(this.uuidLeast, uuid.getLeastSignificantBits());
            dc.set(this.uuidMost, uuid.getMostSignificantBits());

            try {
                Files.copy(levelSponge, world.resolve("level_sponge.dat.nbak"), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                e.printStackTrace();
                context.getServiceCollection().logger().warn("Could not backup the level_sponge.dat. Ignoring.");
                return;
            }

            try (OutputStream os = getOutput(gz, levelSponge)) {
                DataFormats.NBT.writeTo(os, dc);
                os.flush();
            } catch (IOException e) {
                e.printStackTrace();
                context.getServiceCollection().logger().warn("Could not save the level_sponge.dat. Ignoring.");
            }
        }
    }

    static String modifierString(ICommandContext<? extends CommandSource> context, Collection<WorldGeneratorModifier> cw) {
        if (cw.isEmpty()) {
            return context.getMessageString("command.world.create.nomodifiers");
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

    @Override public void onReload(INucleusServiceCollection serviceCollection) {
        this.worldBorderDefault = serviceCollection
                .moduleDataProvider()
                .getModuleConfig(WorldConfig.class)
                .getWorldBorderDefault()
                .orElse(null);
    }

    @NonnullByDefault
    public static class ExtendedDimensionArgument extends CommandElement {

        private final IMessageProviderService messageProviderService;

        private static HashMap<String, DimensionType> replacement = new HashMap<String, DimensionType>() {{
            put("dim0", DimensionTypes.OVERWORLD);
            put("dim-1", DimensionTypes.NETHER);
            put("dim1", DimensionTypes.THE_END);
        }};

        private ExtendedDimensionArgument(@Nullable Text key, IMessageProviderService messageProviderService) {
            super(key);
            this.messageProviderService = messageProviderService;
        }

        @Nullable @Override protected Object parseValue(CommandSource source, CommandArgs args) throws ArgumentParseException {
            final String arg = args.next();
            if (replacement.containsKey(arg.toLowerCase())) {
                return replacement.get(arg.toLowerCase());
            }

            String arg2 = arg;
            if (!arg2.contains(":")) {
                arg2 = "minecraft:" + arg2;
            }

            return Sponge.getRegistry().getType(DimensionType.class, arg2)
                .orElseThrow(() -> args.createError(this.messageProviderService.getMessageFor(source, "args.dimensiontype.notfound", arg)));
        }

        @Override public List<String> complete(CommandSource src, CommandArgs args, CommandContext context) {
            List<String> ids = Sponge.getRegistry().getAllOf(DimensionType.class).stream().map(CatalogType::getId).collect(Collectors.toList());
            try {
                String a = args.peek();
                return ids.stream().filter(x -> x.startsWith(a)).collect(Collectors.toList());
            } catch (Exception e) {
                return ids;
            }
        }
    }
}
