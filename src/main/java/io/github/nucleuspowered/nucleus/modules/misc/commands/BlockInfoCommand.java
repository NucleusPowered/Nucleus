/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.misc.commands;

import io.github.nucleuspowered.nucleus.internal.DataScanner;
import io.github.nucleuspowered.nucleus.internal.annotations.Permissions;
import io.github.nucleuspowered.nucleus.internal.annotations.RegisterCommand;
import io.github.nucleuspowered.nucleus.internal.annotations.RunAsync;
import io.github.nucleuspowered.nucleus.internal.permissions.PermissionInformation;
import io.github.nucleuspowered.nucleus.internal.permissions.SuggestedLevel;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.BlockState;
import org.spongepowered.api.block.BlockType;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.block.trait.BlockTrait;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.data.Property;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.service.pagination.PaginationService;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.util.blockray.BlockRay;
import org.spongepowered.api.util.blockray.BlockRayHit;
import org.spongepowered.api.world.World;

import java.util.*;

@Permissions
@RegisterCommand({"blockinfo"})
@RunAsync
public class BlockInfoCommand extends io.github.nucleuspowered.nucleus.internal.command.AbstractCommand<Player> {

    @Override
    public CommandElement[] getArguments() {
        return new CommandElement[] {GenericArguments.flags().permissionFlag(permissions.getPermissionWithSuffix("extended"), "e", "-extended")
                .buildWith(GenericArguments.none())};
    }

    @Override
    protected Map<String, PermissionInformation> permissionSuffixesToRegister() {
        Map<String, PermissionInformation> m = new HashMap<>();
        m.put("extended", new PermissionInformation(plugin.getMessageProvider().getMessageWithFormat("permission.blockinfo.extended"), SuggestedLevel.ADMIN));
        return m;
    }

    @Override
    public CommandResult executeCommand(Player player, CommandContext args) throws Exception {
        BlockRay<World> bl = BlockRay.from(player).distanceLimit(10).stopFilter(BlockRay.continueAfterFilter(BlockRay.onlyAirFilter(), 1)).build();
        Optional<BlockRayHit<World>> ob = bl.end();

        // If the last block is not air...
        if (ob.isPresent() && ob.get().getLocation().getBlockType() != BlockTypes.AIR) {
            BlockRayHit<World> brh = ob.get();

            // get the information.
            BlockState b = brh.getLocation().getBlock();
            BlockType it = b.getType();

            List<Text> lt = new ArrayList<>();
            lt.add(plugin.getMessageProvider().getTextMessageWithFormat("command.blockinfo.id", it.getId(), it.getTranslation().get()));
            lt.add(plugin.getMessageProvider().getTextMessageWithFormat("command.iteminfo.extendedid", b.getId()));

            if (args.hasAny("e") || args.hasAny("extended")) {
                Collection<Property<?, ?>> cp = b.getApplicableProperties();
                if (!cp.isEmpty()) {
                    cp.forEach(x -> {
                        if (x.getValue() != null) {
                            DataScanner.getText(player, "command.blockinfo.property.item", x.getKey().toString(), x.getValue()).ifPresent(lt::add);
                        }
                    });
                }

                Collection<BlockTrait<?>> cb = b.getTraits();
                if (!cb.isEmpty()) {
                    cb.forEach(x -> b.getTraitValue(x)
                            .ifPresent(v -> DataScanner.getText(player, "command.blockinfo.traits.item", x.getName(), v).ifPresent(lt::add)));
                }
            }

            Sponge.getServiceManager().provideUnchecked(PaginationService.class).builder().contents(lt).padding(Text.of(TextColors.GREEN, "-"))
                    .title(plugin.getMessageProvider().getTextMessageWithFormat("command.blockinfo.list.header", String.valueOf(brh.getBlockX()),
                            String.valueOf(brh.getBlockY()), String.valueOf(brh.getBlockZ())))
                    .sendTo(player);

            return CommandResult.success();
        }

        player.sendMessage(plugin.getMessageProvider().getTextMessageWithFormat("command.blockinfo.none"));
        return CommandResult.empty();
    }
}
