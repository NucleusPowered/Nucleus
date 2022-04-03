/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.item.commands;

import com.google.inject.Inject;
import io.github.nucleuspowered.nucleus.modules.item.ItemPermissions;
import io.github.nucleuspowered.nucleus.modules.item.config.ItemConfig;
import io.github.nucleuspowered.nucleus.modules.item.config.SkullConfig;
import io.github.nucleuspowered.nucleus.core.scaffold.command.ICommandContext;
import io.github.nucleuspowered.nucleus.core.scaffold.command.ICommandExecutor;
import io.github.nucleuspowered.nucleus.core.scaffold.command.ICommandResult;
import io.github.nucleuspowered.nucleus.core.scaffold.command.annotation.Command;
import io.github.nucleuspowered.nucleus.core.scaffold.command.annotation.CommandModifier;
import io.github.nucleuspowered.nucleus.core.scaffold.command.annotation.EssentialsEquivalent;
import io.github.nucleuspowered.nucleus.core.scaffold.command.modifier.CommandModifiers;
import io.github.nucleuspowered.nucleus.core.services.INucleusServiceCollection;
import io.github.nucleuspowered.nucleus.core.services.interfaces.ICommandElementSupplier;
import io.github.nucleuspowered.nucleus.core.services.interfaces.IReloadableService;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.exception.CommandException;
import org.spongepowered.api.command.parameter.Parameter;
import org.spongepowered.api.command.parameter.managed.standard.VariableValueParameters;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.event.CauseStackManager;
import org.spongepowered.api.event.EventContextKeys;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.ItemStackSnapshot;
import org.spongepowered.api.item.inventory.query.QueryTypes;
import org.spongepowered.api.item.inventory.transaction.InventoryTransactionResult;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@EssentialsEquivalent({"skull", "playerskull", "head"})
@Command(
        aliases = {"skull"},
        basePermission = ItemPermissions.BASE_SKULL,
        commandDescriptionKey = "skull",
        modifiers = {
                @CommandModifier(value = CommandModifiers.HAS_COOLDOWN, exemptPermission = ItemPermissions.EXEMPT_COOLDOWN_SKULL),
                @CommandModifier(value = CommandModifiers.HAS_WARMUP, exemptPermission = ItemPermissions.EXEMPT_WARMUP_SKULL),
                @CommandModifier(value = CommandModifiers.HAS_COST, exemptPermission = ItemPermissions.EXEMPT_COST_SKULL)
        },
        associatedPermissions = ItemPermissions.OTHERS_SKULL
)
public class SkullCommand implements ICommandExecutor, IReloadableService.Reloadable {

    private final Parameter.Value<UUID> userParameter;
    private final Parameter.Value<Integer> amountParameter = Parameter.builder(Integer.class)
            .key("amount")
            .addParser(VariableValueParameters.integerRange().min(1).build())
            .optional()
            .build();

    private int amountLimit = Integer.MAX_VALUE;
    private boolean isUseMinecraftCommand = false;

    @Inject
    public SkullCommand(final ICommandElementSupplier supplier) {
        this.userParameter = supplier.createOnlyOtherUserPermissionElement(ItemPermissions.OTHERS_SKULL);
    }

    @Override
    public void onReload(final INucleusServiceCollection serviceCollection) {
        final SkullConfig config = serviceCollection.configProvider().getModuleConfig(ItemConfig.class).getSkullConfig();
        this.isUseMinecraftCommand = config.isUseMinecraftCommand();
        this.amountLimit = config.getSkullLimit();
    }

    @Override
    public Parameter[] parameters(final INucleusServiceCollection serviceCollection) {
        return new Parameter[] {
                this.userParameter,
                this.amountParameter
        };
    }

    @Override
    public ICommandResult execute(final ICommandContext context) throws CommandException {
        final User user = context.getUserFromArgs();
        final ServerPlayer player = context.getIfPlayer();
        final int amount = context.getOne(this.amountParameter).orElse(1);

        if (amount > this.amountLimit && !(context.isConsoleAndBypass() || context.testPermission(ItemPermissions.SKULL_EXEMPT_LIMIT))) {
            // fail
            return context.errorResult("command.skull.limit", this.amountLimit);
        }

        if (this.isUseMinecraftCommand) {
            try (final CauseStackManager.StackFrame frame = Sponge.server().causeStackManager().pushCauseFrame()) {
                frame.addContext(EventContextKeys.SUBJECT, Sponge.systemSubject());
                final CommandResult result = Sponge.server().commandManager().process(
                        String.format("minecraft:give %s skull %d 3 {SkullOwner:%s}", player.name(), amount, user.name()));
                if (result.isSuccess()) {
                    context.sendMessage("command.skull.success.plural", String.valueOf(amount), user.name());
                    return context.successResult();
                }

                return context.errorResult("command.skull.error", user.name());
            }
        }

        final int fullStacks = amount / 64;
        final int partialStack = amount % 64;

        // Create the Skull
        final ItemStack skullStack = ItemStack.builder().itemType(ItemTypes.PLAYER_HEAD).quantity(64).build();

        // Set it to subject skull type and set the owner to the specified subject
        if (skullStack.offer(Keys.GAME_PROFILE, user.profile()).isSuccessful()) {
            final List<ItemStack> itemStackList = new ArrayList<>();

            // If there were stacks, create as many as needed.
            if (fullStacks > 0) {
                itemStackList.add(skullStack);
                for (int i = 2; i <= fullStacks; i++) {
                    itemStackList.add(skullStack.copy());
                }
            }

            // Same with the partial stacks.
            if (partialStack > 0) {
                final ItemStack is = skullStack.copy();
                is.setQuantity(partialStack);
                itemStackList.add(is);
            }

            int accepted = 0;
            int failed = 0;

            final Inventory inventoryToOfferTo = player.inventory().query(QueryTypes.PLAYER_PRIMARY_HOTBAR_FIRST.get().toQuery());
            for (final ItemStack itemStack : itemStackList) {
                final int stackSize = itemStack.quantity();
                final InventoryTransactionResult itr = inventoryToOfferTo.offer(itemStack);
                final int currentFail = itr.rejectedItems().stream().mapToInt(ItemStackSnapshot::quantity).sum();
                failed += currentFail;
                accepted += stackSize - currentFail;
            }

            // What was accepted?
            if (accepted > 0) {
                if (failed > 0) {
                    context.sendMessage("command.skull.semifull", failed);
                }

                if (accepted == 1) {
                    context.sendMessage("command.skull.success.single", user.name());
                } else {
                    context.sendMessage("command.skull.success.plural", accepted, user.name());
                }

                return context.successResult();
            }

            return context.errorResult("command.skull.full", user.name());
        } else {
            return context.errorResult("command.skull.error", user.name());
        }
    }
}
