/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.misc.commands;

import io.github.nucleuspowered.nucleus.Util;
import io.github.nucleuspowered.nucleus.modules.misc.MiscPermissions;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandContext;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandExecutor;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandResult;
import io.github.nucleuspowered.nucleus.scaffold.command.NucleusParameters;
import io.github.nucleuspowered.nucleus.scaffold.command.annotation.Command;
import io.github.nucleuspowered.nucleus.services.INucleusServiceCollection;
import io.github.nucleuspowered.nucleus.util.DataScanner;
import io.github.nucleuspowered.nucleus.util.TypeTokens;
import org.spongepowered.api.block.BlockState;
import org.spongepowered.api.block.BlockType;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.block.trait.BlockTrait;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.command.args.CommandFlags;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.data.Property;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.util.annotation.NonnullByDefault;
import org.spongepowered.api.util.blockray.BlockRay;
import org.spongepowered.api.util.blockray.BlockRayHit;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@NonnullByDefault
@Command(
        aliases = "blockinfo",
        basePermission = MiscPermissions.BASE_BLOCKINFO,
        commandDescriptionKey = "blockinfo",
        associatedPermissions = MiscPermissions.BLOCKINFO_EXTENDED
)
public class BlockInfoCommand implements ICommandExecutor<CommandSource> {

    @Override
    public CommandElement[] parameters(INucleusServiceCollection serviceCollection) {
        return new CommandElement[] {
            GenericArguments.flags()
                    .setUnknownShortFlagBehavior(CommandFlags.UnknownFlagBehavior.IGNORE)
                    .permissionFlag(MiscPermissions.BLOCKINFO_EXTENDED, "e", "-extended")
                    .buildWith(NucleusParameters.OPTIONAL_LOCATION)
        };
    }

    @Override
    public ICommandResult execute(ICommandContext<? extends CommandSource> context) throws CommandException {
        Location<World> loc = null;
        if (context.hasAny(NucleusParameters.Keys.LOCATION)) {
            // get the location
            loc = context.getOne(NucleusParameters.Keys.LOCATION, TypeTokens.LOCATION_WORLD)
                    .filter(x -> x.getBlockType() != BlockTypes.AIR).orElse(null);
        } else {
            if (!context.is(Player.class)) {
                return context.errorResult("command.blockinfo.player");
            }
            BlockRay<World> bl =
                    BlockRay.from(context.getIfPlayer())
                            .distanceLimit(10).stopFilter(BlockRay.continueAfterFilter(BlockRay.onlyAirFilter(), 1)).build();
            Optional<BlockRayHit<World>> ob = bl.end();

            // If the last block is not air...
            if (ob.isPresent() && ob.get().getLocation().getBlockType() != BlockTypes.AIR) {
                BlockRayHit<World> brh = ob.get();
                loc = brh.getLocation();
            }
        }

        if (loc != null) {
            // get the information.
            BlockState b = loc.getBlock();
            BlockType it = b.getType();

            List<Text> lt = new ArrayList<>();
            lt.add(context.getMessage("command.blockinfo.id", it.getId(), it.getTranslation().get()));
            lt.add(context.getMessage("command.iteminfo.extendedid", b.getId()));

            if (context.hasAny("e") || context.hasAny("extended")) {
                Collection<Property<?, ?>> cp = b.getApplicableProperties();
                if (!cp.isEmpty()) {
                    cp.forEach(x -> {
                        if (x.getValue() != null) {
                            DataScanner.getInstance(context.getServiceCollection().messageProvider())
                                    .getText(context.getCommandSourceAsPlayerUnchecked(), "command.blockinfo.property.item", x.getKey().toString(), x.getValue()).ifPresent(lt::add);
                        }
                    });
                }

                Collection<BlockTrait<?>> cb = b.getTraits();
                if (!cb.isEmpty()) {
                    cb.forEach(x -> b.getTraitValue(x).flatMap(
                            v -> DataScanner.getInstance(context.getServiceCollection().messageProvider())
                                    .getText(context.getCommandSourceAsPlayerUnchecked(), "command.blockinfo.traits.item", x.getName(), v))
                                    .ifPresent(lt::add));
                }
            }

            Util.getPaginationBuilder(context.getCommandSource()).contents(lt).padding(Text.of(TextColors.GREEN, "-"))
                    .title(context.getMessage("command.blockinfo.list.header",
                            String.valueOf(loc.getBlockX()),
                            String.valueOf(loc.getBlockY()),
                            String.valueOf(loc.getBlockZ())))
                    .sendTo(context.getCommandSource());

            return context.successResult();
        }

        return context.errorResult("command.blockinfo.none");
    }
}
