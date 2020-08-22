/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.kit.commands;

import io.github.nucleuspowered.nucleus.Util;
import io.github.nucleuspowered.nucleus.modules.kit.KitPermissions;
import io.github.nucleuspowered.nucleus.modules.kit.services.KitService;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandContext;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandExecutor;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandResult;
import io.github.nucleuspowered.nucleus.scaffold.command.annotation.Command;
import io.github.nucleuspowered.nucleus.services.INucleusServiceCollection;
import io.github.nucleuspowered.nucleus.services.interfaces.IMessageProviderService;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.filter.Getter;
import org.spongepowered.api.event.filter.cause.Root;
import org.spongepowered.api.event.item.inventory.InteractInventoryEvent;
import org.spongepowered.api.item.inventory.Container;
import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.api.item.inventory.property.InventoryTitle;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.util.annotation.NonnullByDefault;

@NonnullByDefault
@Command(
        aliases = { "create", "add" },
        basePermission = KitPermissions.BASE_KIT_CREATE,
        commandDescriptionKey = "kit.create",
        parentCommand = KitCommand.class
)
public class KitCreateCommand implements ICommandExecutor<CommandSource> {

    private final String flag = "clone";
    private final String name = "name";

    @Override
    public CommandElement[] parameters(INucleusServiceCollection serviceCollection) {
        return new CommandElement[] {
                GenericArguments.flags().valueFlag(serviceCollection
                                .commandElementSupplier()
                                .createPermissionParameter(GenericArguments.markTrue(Text.of(this.flag)),
                                        KitPermissions.BASE_KIT_EDIT, false), "c", "-clone")
                    .buildWith(GenericArguments.onlyOne(GenericArguments.string(Text.of(this.name))))
        };
    }

    @Override
    public ICommandResult execute(ICommandContext<? extends CommandSource> context) throws CommandException {
        KitService service = context.getServiceCollection().getServiceUnchecked(KitService.class);
        String kitName = context.requireOne(this.name, String.class);

        if (service.getKitNames().stream().anyMatch(kitName::equalsIgnoreCase)) {
            return context.errorResult("command.kit.add.alreadyexists", kitName);
        }

        if (context.is(Player.class) && context.testPermission(KitPermissions.BASE_KIT_EDIT)) {
            // if we have a clone request, clone.
            if (context.hasAny(this.flag)) {
                service.saveKit(service.createKit(kitName).updateKitInventory(context.getCommandSourceAsPlayerUnchecked()));
                context.sendMessage("command.kit.add.success", kitName);
            } else {
                final Player player = context.getIfPlayer();
                Inventory inventory = Util.getKitInventoryBuilder()
                        .property(InventoryTitle.PROPERTY_NAME,
                                InventoryTitle.of(context.getMessage("command.kit.create.title", kitName)))
                        .build(context.getServiceCollection().pluginContainer());
                Container container = player.openInventory(inventory)
                        .orElseThrow(() -> context.createException("command.kit.create.notcreated"));
                Sponge.getEventManager().registerListeners(context.getServiceCollection().pluginContainer(), new TemporaryEventListener(
                        service,
                        context.getServiceCollection().messageProvider(),
                        inventory,
                        container,
                        kitName));
            }
        } else {
            try {
                service.saveKit(service.createKit(kitName));
                context.sendMessage("command.kit.addempty.success", kitName);
            } catch (IllegalArgumentException ex) {
                return context.errorResult("command.kit.create.failed", kitName);
            }
        }

        return context.successResult();
    }

    public static class TemporaryEventListener {

        private final Inventory inventory;
        private final Container container;
        private final String kitName;
        private final KitService handler;
        private final IMessageProviderService messageProviderService;
        private boolean run = false;

        private TemporaryEventListener(KitService handler,
                IMessageProviderService messageProviderService,
                Inventory inventory,
                Container container,
                String kitName) {
            this.messageProviderService = messageProviderService;
            this.handler = handler;
            this.inventory = inventory;
            this.container = container;
            this.kitName = kitName;
        }

        @Listener
        public void onClose(InteractInventoryEvent.Close event, @Root Player player, @Getter("getTargetInventory") Container container) {
            if (!this.run && this.container.equals(container)) {
                this.run = true;
                Sponge.getEventManager().unregisterListeners(this);

                if (this.handler.getKitNames().stream().noneMatch(this.kitName::equalsIgnoreCase)) {
                    this.handler.saveKit(this.handler.createKit(this.kitName).updateKitInventory(this.inventory));
                    this.messageProviderService.sendMessageTo(player, "command.kit.add.success", this.kitName);
                } else {
                    this.messageProviderService.sendMessageTo(player, "command.kit.add.alreadyexists", this.kitName);
                }

                // Now return the items to the subject.
                this.inventory.slots().forEach(x -> x.poll().ifPresent(item -> player.getInventory().offer(item)));
            }
        }
    }
}
