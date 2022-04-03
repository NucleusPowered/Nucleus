/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.item.commands;

import com.google.inject.Inject;
import io.github.nucleuspowered.nucleus.modules.item.ItemPermissions;
import io.github.nucleuspowered.nucleus.core.scaffold.command.ICommandContext;
import io.github.nucleuspowered.nucleus.core.scaffold.command.ICommandExecutor;
import io.github.nucleuspowered.nucleus.core.scaffold.command.ICommandResult;
import io.github.nucleuspowered.nucleus.core.scaffold.command.annotation.Command;
import io.github.nucleuspowered.nucleus.core.scaffold.command.annotation.CommandModifier;
import io.github.nucleuspowered.nucleus.core.scaffold.command.modifier.CommandModifiers;
import io.github.nucleuspowered.nucleus.core.services.INucleusServiceCollection;
import io.github.nucleuspowered.nucleus.core.services.interfaces.ICommandElementSupplier;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.spongepowered.api.command.exception.CommandException;
import org.spongepowered.api.command.parameter.Parameter;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.data.type.HandTypes;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

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
public class UnsignBookCommand implements ICommandExecutor {

    private final Parameter.Value<UUID> user;

    @Inject
    public UnsignBookCommand(final ICommandElementSupplier elementSupplier) {
        this.user = elementSupplier.createOnlyOtherUserPermissionElement(ItemPermissions.OTHERS_UNSIGNBOOK);
    }

    @Override
    public Parameter[] parameters(final INucleusServiceCollection serviceCollection) {
        return new Parameter[] {
                this.user
        };
    }

    @Override
    public ICommandResult execute(final ICommandContext context) throws CommandException {
        final User target = context.getUserFromArgs();
        final boolean isSelf = context.is(target);

        // Very basic for now, unsign book in hand.
        final ItemStack bookToUnsign = target.itemInHand(HandTypes.MAIN_HAND);
        if (bookToUnsign.type().equals(ItemTypes.WRITTEN_BOOK.get())) {
            final ItemStack unsignedBook = ItemStack.builder()
                    .itemType(ItemTypes.WRITABLE_BOOK)
                    .add(Keys.PLAIN_PAGES, this.from(bookToUnsign))
                    .quantity(bookToUnsign.quantity())
                    .build();
            target.setItemInHand(HandTypes.MAIN_HAND, unsignedBook);

            if (isSelf) {
                context.sendMessage("command.unsignbook.success.self");
            } else {
                context.sendMessage("command.unsignbook.success.other", target.name());
            }
            return context.successResult();
        }

        if (isSelf) {
            return context.errorResult("command.unsignbook.notinhand.self");
        } else {
            return context.errorResult("command.unsignbook.notinhand.other", target.name());
        }
    }

    private List<String> from(final ItemStack bookToUnsign) {
        return bookToUnsign.get(Keys.PAGES)
                .map(x -> x.stream().map(y -> LegacyComponentSerializer.legacyAmpersand().serialize(y)).collect(Collectors.toList()))
                .orElseGet(ArrayList::new);
    }

}
