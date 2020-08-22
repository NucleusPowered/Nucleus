/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.item.commands;

import io.github.nucleuspowered.nucleus.modules.inventory.InventoryPermissions;
import io.github.nucleuspowered.nucleus.modules.item.ItemPermissions;
import io.github.nucleuspowered.nucleus.quickstart.annotation.RequireExistenceOf;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandContext;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandExecutor;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandResult;
import io.github.nucleuspowered.nucleus.scaffold.command.annotation.Command;
import io.github.nucleuspowered.nucleus.scaffold.command.annotation.CommandModifier;
import io.github.nucleuspowered.nucleus.scaffold.command.modifier.CommandModifiers;
import io.github.nucleuspowered.nucleus.services.INucleusServiceCollection;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.data.manipulator.mutable.item.PlainPagedData;
import org.spongepowered.api.data.type.HandTypes;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.serializer.TextSerializers;
import org.spongepowered.plugin.meta.util.NonnullByDefault;

import java.util.List;
import java.util.Optional;

@NonnullByDefault
@RequireExistenceOf("org.spongepowered.api.data.manipulator.mutable.item.PlainPagedData") // not in 7.1
@Command(
        aliases = {"unsignbook", "unsign"},
        basePermission = ItemPermissions.BASE_UNSIGNBOOK,
        commandDescriptionKey = "unsignbook",
        modifiers = {
                @CommandModifier(value = CommandModifiers.HAS_COOLDOWN, exemptPermission = ItemPermissions.EXEMPT_COOLDOWN_UNSIGNBOOK),
                @CommandModifier(value = CommandModifiers.HAS_WARMUP, exemptPermission = ItemPermissions.EXEMPT_WARMUP_UNSIGNBOOK),
                @CommandModifier(value = CommandModifiers.HAS_COST, exemptPermission = ItemPermissions.EXEMPT_COST_UNSIGNBOOK)
        },
        associatedPermissions = ItemPermissions.OTHERS_UNSIGNBOOK
)
public class UnsignBookCommand implements ICommandExecutor<CommandSource> {

    @Override
    public CommandElement[] parameters(INucleusServiceCollection serviceCollection) {
        return new CommandElement[] {
                serviceCollection.commandElementSupplier()
                    .createOnlyOtherUserPermissionElement(true, ItemPermissions.OTHERS_UNSIGNBOOK)
        };
    }

    @Override
    public ICommandResult execute(ICommandContext<? extends CommandSource> context) throws CommandException {
        User target = context.getUserFromArgs();
        boolean isSelf = context.is(target);

        // Very basic for now, unsign book in hand.
        Optional<ItemStack> bookToUnsign =
                target.getItemInHand(HandTypes.MAIN_HAND).filter(item -> item.getType().equals(ItemTypes.WRITTEN_BOOK));
        if (bookToUnsign.isPresent()) {
            ItemStack unsignedBook = ItemStack.builder()
                    .itemType(ItemTypes.WRITABLE_BOOK)
                    .itemData(bookToUnsign.get().get(Keys.BOOK_PAGES).map(this::from).orElseGet(this::createData))
                    .quantity(bookToUnsign.get().getQuantity())
                    .build();
            target.setItemInHand(HandTypes.MAIN_HAND, unsignedBook);

            if (isSelf) {
                context.sendMessage("command.unsignbook.success.self");
            } else {
                context.sendMessage("command.unsignbook.success.other", target.getName());
            }
            return context.successResult();
        }

        if (isSelf) {
            return context.errorResult("command.unsignbook.notinhand.self");
        } else {
            return context.errorResult("command.unsignbook.notinhand.other", target.getName());
        }
    }

    private PlainPagedData from(List<Text> texts) {
        PlainPagedData ppd = createData();
        for (Text text : texts) {
            ppd.addElement(TextSerializers.FORMATTING_CODE.serialize(text));
        }

        return ppd;
    }

    private PlainPagedData createData() {
        return Sponge.getDataManager().getManipulatorBuilder(PlainPagedData.class).get().create();
    }


}
