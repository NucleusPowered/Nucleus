/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.admin.commands;

import io.github.nucleuspowered.nucleus.modules.admin.AdminPermissions;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandContext;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandExecutor;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandResult;
import io.github.nucleuspowered.nucleus.scaffold.command.annotation.Command;
import io.github.nucleuspowered.nucleus.scaffold.command.annotation.CommandModifier;
import io.github.nucleuspowered.nucleus.scaffold.command.annotation.EssentialsEquivalent;
import io.github.nucleuspowered.nucleus.scaffold.command.modifier.CommandModifiers;
import io.github.nucleuspowered.nucleus.services.INucleusServiceCollection;
import io.github.nucleuspowered.nucleus.util.TypeTokens;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.event.CauseStackManager;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.action.TextActions;
import org.spongepowered.api.util.annotation.NonnullByDefault;
import org.spongepowered.api.world.BlockChangeFlags;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

@NonnullByDefault
@Command(aliases = {"blockzap", "zapblock"},
        basePermission = AdminPermissions.BASE_BLOCKZAP,
        commandDescriptionKey = "blockzap",
        modifiers = {
                @CommandModifier(value = CommandModifiers.HAS_WARMUP, exemptPermission = AdminPermissions.EXEMPT_WARMUP_BLOCKZAP),
                @CommandModifier(value = CommandModifiers.HAS_COOLDOWN, exemptPermission = AdminPermissions.EXEMPT_COOLDOWN_BLOCKZAP),
                @CommandModifier(value = CommandModifiers.HAS_COST, exemptPermission = AdminPermissions.EXEMPT_COST_BLOCKZAP)
        }
)
@EssentialsEquivalent(value = "break", isExact = false, notes = "Requires co-ordinates, whereas Essentials required you to look at the block.")
public class BlockZapCommand implements ICommandExecutor<CommandSource> {

    private final String locationKey = "location";

    @Override public CommandElement[] parameters(INucleusServiceCollection serviceCollection) {
        return new CommandElement[] {
            GenericArguments.onlyOne(GenericArguments.location(Text.of(this.locationKey)))
        };
    }

    @Override public ICommandResult execute(ICommandContext<? extends CommandSource> context) throws CommandException {
        Location<World> location = context.requireOne(this.locationKey, TypeTokens.LOCATION_WORLD);
        if (location.getBlockType() == BlockTypes.AIR) {
            return context.errorResult("command.blockzap.alreadyair", location.getPosition().toString(), location.getExtent().getName());
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

        try (CauseStackManager.StackFrame frame = Sponge.getCauseStackManager().pushCauseFrame()) {
            frame.pushCause(context.getCommandSource());
            if (location.setBlock(BlockTypes.AIR.getDefaultState(), BlockChangeFlags.ALL)) {
                context.sendMessage("command.blockzap.success", Text.of(location.getPosition().toString()), Text.of(location.getExtent().getName()), itemText);
                return context.successResult();
            }
        }

        return context.errorResult("command.blockzap.fail", location.getPosition().toString(), location.getExtent().getName());
    }
}
