/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.admin.commands;

import io.github.nucleuspowered.nucleus.Nucleus;
import io.github.nucleuspowered.nucleus.internal.annotations.command.Permissions;
import io.github.nucleuspowered.nucleus.internal.annotations.command.RegisterCommand;
import io.github.nucleuspowered.nucleus.internal.command.AbstractCommand;
import io.github.nucleuspowered.nucleus.internal.command.ReturnMessageException;
import io.github.nucleuspowered.nucleus.internal.docgen.annotations.EssentialsEquivalent;
import io.github.nucleuspowered.nucleus.util.CauseStackHelper;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.action.TextActions;
import org.spongepowered.api.util.annotation.NonnullByDefault;
import org.spongepowered.api.world.BlockChangeFlags;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

@NonnullByDefault
@Permissions
@RegisterCommand({"blockzap", "zapblock"})
@EssentialsEquivalent(value = "break", isExact = false, notes = "Requires co-ordinates, whereas Essentials required you to look at the block.")
public class BlockZapCommand extends AbstractCommand<CommandSource> {

    private final String locationKey = "location";

    @Override public CommandElement[] getArguments() {
        return new CommandElement[] {
            GenericArguments.onlyOne(GenericArguments.location(Text.of(this.locationKey)))
        };
    }

    @Override public CommandResult executeCommand(CommandSource src, CommandContext args) throws Exception {
        Location<World> location = args.<Location<World>>getOne(this.locationKey).get();
        if (location.getBlockType() == BlockTypes.AIR) {
            throw new ReturnMessageException(Nucleus.getNucleus()
                    .getMessageProvider().getTextMessageWithFormat("command.blockzap.alreadyair", location.getPosition().toString(), location.getExtent().getName()));
        }

        Text itemText = Text.of(location.getBlock().getName());
        if (location.getBlockType().getItem().isPresent()) {
            ItemStack item = ItemStack.builder()
                    .fromBlockState(location.getBlock())
                    .build();
            itemText = item.get(Keys.DISPLAY_NAME).orElseGet(() -> Text.of(item)).toBuilder()
                    .onHover(TextActions.showItem(item.createSnapshot()))
                    .build();
        }

        if (CauseStackHelper.createFrameWithCausesWithReturn(c -> location.setBlock(BlockTypes.AIR.getDefaultState(), BlockChangeFlags.ALL), src)) {

            src.sendMessage(Nucleus.getNucleus()
                    .getMessageProvider().getTextMessageWithTextFormat("command.blockzap.success", Text.of(location.getPosition().toString()), Text.of(location.getExtent().getName()), itemText));
            return CommandResult.success();
        }

        throw new ReturnMessageException(Nucleus.getNucleus()
                .getMessageProvider().getTextMessageWithFormat("command.blockzap.fail", location.getPosition().toString(), location.getExtent().getName()));
    }
}
