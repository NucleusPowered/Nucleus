/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.world.commands;

import com.flowpowered.math.vector.Vector3i;
import com.google.common.collect.Lists;
import io.github.nucleuspowered.nucleus.Util;
import io.github.nucleuspowered.nucleus.modules.world.WorldPermissions;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandContext;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandExecutor;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandResult;
import io.github.nucleuspowered.nucleus.scaffold.command.annotation.Command;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.data.DataQuery;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.util.annotation.NonnullByDefault;
import org.spongepowered.api.world.storage.WorldProperties;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;

@NonnullByDefault
@Command(
        aliases = {"list", "ls", "#worlds"},
        basePermission = WorldPermissions.BASE_WORLD_INFO,
        commandDescriptionKey = "world.list",
        parentCommand = WorldCommand.class,
        associatedPermissions = {
                WorldPermissions.WORLD_SEED
        }
)
public class ListWorldCommand implements ICommandExecutor<CommandSource> {

    // Use a space over EMPTY so pagination doesn't mess up.
    private final static Text SPACE = Text.of(" ");

    static void getWorldInfo(ICommandContext<? extends CommandSource> context, List<Text> listContent, WorldProperties x, boolean canSeeSeeds) {
        // Name of world
        if (!listContent.isEmpty()) {
            listContent.add(SPACE);
        }

        listContent.add(context.getMessage("command.world.list.worlditem", x.getWorldName()));

        // As requested by Pixelmon for use in their config.
        x.getAdditionalProperties().getInt(DataQuery.of("SpongeData", "dimensionId")).ifPresent(i ->
            listContent.add(context.getMessage("command.world.list.dimensionid", String.valueOf(i))));
        Vector3i spawnPosition = x.getSpawnPosition();
        listContent.add(context.getMessage("command.world.list.spawnpoint",
                String.valueOf(spawnPosition.getX()), String.valueOf(spawnPosition.getY()), String.valueOf(spawnPosition.getZ())));

        listContent.add(context.getMessage("command.world.list.uuid", x.getUniqueId().toString()));
        if (x.isEnabled()) {
            boolean worldLoaded = Sponge.getServer().getWorld(x.getUniqueId()).isPresent();
            String message =
                (worldLoaded ? "&a" : "&c") + context.getMessageString(worldLoaded ? "standard.true" : "standard.false");
            listContent.add(context.getMessage("command.world.list.enabled", message));
        } else {
            listContent.add(context.getMessage("command.world.list.disabled"));
        }

        if (canSeeSeeds) {
            listContent.add(context.getMessage("command.world.list.seed", String.valueOf(x.getSeed())));
        }

        listContent.add(context.getMessage("command.world.list.params",
            x.getDimensionType().getName(),
            x.getGeneratorType().getName(),
            CreateWorldCommand.modifierString(context, x.getGeneratorModifiers()),
            x.getGameMode().getName(),
            x.getDifficulty().getName()));

        listContent.add(context.getMessage("command.world.list.params2",
            String.valueOf(x.isHardcore()),
            String.valueOf(x.loadOnStartup()),
            String.valueOf(x.isPVPEnabled()),
            String.valueOf(x.doesKeepSpawnLoaded())
        ));
    }

    @Override public ICommandResult execute(ICommandContext<? extends CommandSource> context) throws CommandException {
        // Get all the worlds
        Collection<WorldProperties> cwp = Sponge.getServer().getAllWorldProperties();
        final List<Text> listContent = Lists.newArrayList();

        final boolean canSeeSeeds = context.testPermission(WorldPermissions.WORLD_SEED);
        cwp.stream().sorted(Comparator.comparing(WorldProperties::getWorldName)).forEach(x -> getWorldInfo(context, listContent, x, canSeeSeeds));

        Util.getPaginationBuilder(context.getCommandSourceUnchecked())
            .contents(listContent).title(context.getMessage("command.world.list.title"))
            .sendTo(context.getCommandSourceUnchecked());

        return context.successResult();
    }
}
