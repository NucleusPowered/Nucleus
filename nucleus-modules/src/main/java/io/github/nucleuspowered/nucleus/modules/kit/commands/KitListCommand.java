/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.kit.commands;

import com.google.common.collect.Lists;
import io.github.nucleuspowered.nucleus.api.module.kit.data.Kit;
import io.github.nucleuspowered.nucleus.modules.kit.KitKeys;
import io.github.nucleuspowered.nucleus.modules.kit.KitPermissions;
import io.github.nucleuspowered.nucleus.modules.kit.services.KitService;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandContext;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandExecutor;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandResult;
import io.github.nucleuspowered.nucleus.scaffold.command.annotation.Command;
import io.github.nucleuspowered.nucleus.services.interfaces.IEconomyServiceProvider;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.service.pagination.PaginationList;
import org.spongepowered.api.service.pagination.PaginationService;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.action.TextActions;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.text.format.TextStyles;
import org.spongepowered.api.util.annotation.NonnullByDefault;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

@Command(
        aliases = { "list", "ls", "#kits" },
        async = true,
        basePermission = KitPermissions.BASE_KIT_LIST,
        commandDescriptionKey = "kit.list",
        parentCommand = KitCommand.class,
        associatedPermissions = KitPermissions.KIT_SHOWHIDDEN
)
@NonnullByDefault
public class KitListCommand implements ICommandExecutor<CommandSource> {

    @Override public ICommandResult execute(ICommandContext<? extends CommandSource> context) throws CommandException {
        KitService service = context.getServiceCollection().getServiceUnchecked(KitService.class);
        Set<String> kits = service.getKitNames();
        if (kits.isEmpty()) {
            return context.errorResult("command.kit.list.empty");
        }

        PaginationService paginationService = Sponge.getServiceManager().provideUnchecked(PaginationService.class);
        ArrayList<Text> kitText = Lists.newArrayList();

        Map<String, Instant> redeemed =
                context.is(Player.class) ? context.getServiceCollection()
                        .storageManager()
                        .getUserService()
                        .getOrNewOnThread(context.getIfPlayer().getUniqueId())
                        .getNullable(KitKeys.REDEEMED_KITS) : null;

        final boolean showHidden = context.testPermission(KitPermissions.KIT_SHOWHIDDEN);
        service.getKitNames(showHidden).stream()
                .filter(kit -> context.testPermission(KitPermissions.getKitPermission(kit.toLowerCase())))
                .forEach(kit -> kitText.add(createKit(context, redeemed, kit, service.getKit(kit).get())));

        PaginationList.Builder paginationBuilder = paginationService.builder().contents(kitText)
                .title(context.getMessage("command.kit.list.kits"))
                .padding(Text.of(TextColors.GREEN, "-"));
        paginationBuilder.sendTo(context.getCommandSource());

        return context.successResult();
    }

    private Text createKit(ICommandContext<? extends CommandSource> context, @Nullable Map<String, Instant> user, String kitName, Kit kitObj) {
        Text.Builder tb = Text.builder(kitName);

        if (user != null) {
            Instant lastRedeem = user.get(kitName.toLowerCase());
            if (lastRedeem != null) {
                // If one time used...
                if (kitObj.isOneTime() && !context.testPermission(KitPermissions.KIT_EXEMPT_ONETIME)) {
                    return tb.color(TextColors.RED)
                            .onHover(TextActions.showText(context.getMessage("command.kit.list.onetime", kitName)))
                            .style(TextStyles.STRIKETHROUGH).build();
                }

                // If an intervalOld is used...
                Duration interval = kitObj.getCooldown().orElse(Duration.ZERO);
                if (!interval.isZero() && !context.testPermission(KitPermissions.KIT_EXEMPT_COOLDOWN)) {

                    // Get the next time the kit can be used.
                    Instant next = lastRedeem.plus(interval);
                    if (next.isAfter(Instant.now())) {
                        // Get the time to next usage.
                        String time = context.getTimeString(Duration.between(Instant.now(), next));
                        return tb.color(TextColors.RED)
                                .onHover(TextActions.showText(context.getMessage("command.kit.list.interval", kitName, time)))
                                .style(TextStyles.STRIKETHROUGH).build();
                    }
                }
            }
        }

        // Can use.
        Text.Builder builder = tb.color(TextColors.AQUA).onClick(TextActions.runCommand("/kit \"" + kitName + "\""))
                .onHover(TextActions.showText(context.getMessage("command.kit.list.text", kitName)))
                .style(TextStyles.ITALIC);
        IEconomyServiceProvider economyServiceProvider = context.getServiceCollection().economyServiceProvider();
        if (kitObj.getCost() > 0 && economyServiceProvider.serviceExists() && !context.testPermission(KitPermissions.KIT_EXEMPT_COST)) {
            builder = Text.builder().append(builder.build())
                .append(context.getMessage("command.kit.list.cost", economyServiceProvider.getCurrencySymbol(kitObj.getCost())));
        }

        return builder.build();
    }
}
