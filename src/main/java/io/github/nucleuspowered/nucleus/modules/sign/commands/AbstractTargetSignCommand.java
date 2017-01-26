/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.sign.commands;

import io.github.nucleuspowered.nucleus.argumentparsers.PositiveDoubleArgument;
import io.github.nucleuspowered.nucleus.internal.command.AbstractCommand;
import io.github.nucleuspowered.nucleus.internal.command.ReturnMessageException;
import io.github.nucleuspowered.nucleus.modules.sign.handlers.ActionSignHandler;
import io.github.nucleuspowered.nucleus.spongedata.manipulators.AbstractSignManipulator;
import org.spongepowered.api.block.tileentity.Sign;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.args.CommandFlags;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.util.blockray.BlockRay;
import org.spongepowered.api.util.blockray.BlockRayHit;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.util.Optional;

import javax.inject.Inject;

public abstract class AbstractTargetSignCommand extends AbstractCommand<Player> {

    @Inject protected ActionSignHandler actionSignHandler;

    @Override protected CommandResult executeCommand(Player src, CommandContext args) throws Exception {
        BlockRay<World> bw = BlockRay.from(src).distanceLimit(15).stopFilter(BlockRay.continueAfterFilter(BlockRay.onlyAirFilter(), 1)).build();
        Optional<BlockRayHit<World>> obh = bw.end();
        if (obh.isPresent()) {
            Location<World> lw = obh.get().getLocation();
            if (lw.getTileEntity().isPresent() && lw.getTileEntity().get() instanceof Sign) {
                return executeCommand(src, args, (Sign)lw.getTileEntity().get());
            }
        }

        throw new ReturnMessageException(plugin.getMessageProvider().getTextMessageWithFormat("command.sign.create.nosign"));
    }

    protected abstract CommandResult executeCommand(Player src, CommandContext args, Sign sign) throws Exception;

    public abstract static class Create extends AbstractTargetSignCommand {

        protected final String permissionKey = "permission";
        protected final String costKey = "cost";

        protected final CommandFlags.Builder getFlags() {
            return GenericArguments.flags()
                .valueFlag(GenericArguments.string(Text.of(permissionKey)), "p", "-" + permissionKey)
                .valueFlag(new PositiveDoubleArgument(Text.of(costKey)), "c", "-" + costKey);
        }

        @Override protected final CommandResult executeCommand(Player src, CommandContext args, Sign sign) throws Exception {
            Optional<Class<? extends AbstractSignManipulator<?, ?>>> a =
                actionSignHandler.getRegisteredClasses().stream().filter(x -> sign.get(x).isPresent()).findFirst();
            if (a.isPresent()) {
                throw new ReturnMessageException(plugin.getMessageProvider().getTextMessageWithFormat("command.sign.create.exists", a.get().getSimpleName()));
            }

            return executeCreateCommand(src, args, sign);
        }

        protected abstract CommandResult executeCreateCommand(Player src, CommandContext args, Sign sign) throws Exception;
    }
}
