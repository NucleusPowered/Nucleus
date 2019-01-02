/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.kit.commands.kit;

import com.google.common.collect.Lists;
import io.github.nucleuspowered.nucleus.Nucleus;
import io.github.nucleuspowered.nucleus.Util;
import io.github.nucleuspowered.nucleus.api.nucleusdata.Kit;
import io.github.nucleuspowered.nucleus.internal.CommandPermissionHandler;
import io.github.nucleuspowered.nucleus.internal.annotations.RunAsync;
import io.github.nucleuspowered.nucleus.internal.annotations.command.NoModifiers;
import io.github.nucleuspowered.nucleus.internal.annotations.command.Permissions;
import io.github.nucleuspowered.nucleus.internal.annotations.command.RegisterCommand;
import io.github.nucleuspowered.nucleus.internal.permissions.SuggestedLevel;
import io.github.nucleuspowered.nucleus.modules.kit.KitKeys;
import io.github.nucleuspowered.nucleus.modules.kit.commands.KitFallbackBase;
import io.github.nucleuspowered.nucleus.modules.kit.services.KitHandler;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.cause.Cause;
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

@Permissions(prefix = "kit", suggestedLevel = SuggestedLevel.USER)
@RegisterCommand(value = {"list", "ls"}, subcommandOf = KitCommand.class, rootAliasRegister = "kits")
@RunAsync
@NoModifiers
@NonnullByDefault
public class KitListCommand extends KitFallbackBase<CommandSource> {

    private final CommandPermissionHandler kitPermissionHandler = Nucleus.getNucleus().getPermissionRegistry()
            .getPermissionsForNucleusCommand(KitCommand.class);

    @Override
    public CommandResult executeCommand(final CommandSource src, CommandContext args, Cause cause) {
        Set<String> kits = KIT_HANDLER.getKitNames();
        if (kits.isEmpty()) {
            src.sendMessage(Nucleus.getNucleus().getMessageProvider().getTextMessageWithFormat("command.kit.list.empty"));
            return CommandResult.empty();
        }

        PaginationService paginationService = Sponge.getServiceManager().provideUnchecked(PaginationService.class);
        ArrayList<Text> kitText = Lists.newArrayList();

        Map<String, Instant> redeemed =
            src instanceof Player ? Nucleus.getNucleus().getStorageManager().getUserService()
                    .getOrNewOnThread(((Player) src).getUniqueId())
                    .getNullable(KitKeys.REDEEMED_KITS) : null;

        final boolean showHidden = this.kitPermissionHandler.testSuffix(src, "showhidden");
        KIT_HANDLER.getKitNames(showHidden).stream()
                .filter(kit -> hasPermission(src, KitHandler.getPermissionForKit(kit.toLowerCase())))
                .forEach(kit -> kitText.add(createKit(src, redeemed, kit, KIT_HANDLER.getKit(kit).get())));

        PaginationList.Builder paginationBuilder = paginationService.builder().contents(kitText)
                .title(Nucleus.getNucleus().getMessageProvider().getTextMessageWithFormat("command.kit.list.kits")).padding(Text.of(TextColors.GREEN, "-"));
        paginationBuilder.sendTo(src);

        return CommandResult.success();
    }

    private Text createKit(CommandSource source, @Nullable Map<String, Instant> user, String kitName, Kit kitObj) {
        Text.Builder tb = Text.builder(kitName);

        if (user != null) {
            Instant lastRedeem = user.get(kitName.toLowerCase());
            if (lastRedeem != null) {
                // If one time used...
                if (kitObj.isOneTime() && !this.kitPermissionHandler.testSuffix(source, "exempt.onetime")) {
                    return tb.color(TextColors.RED)
                            .onHover(TextActions.showText(
                                    Nucleus.getNucleus().getMessageProvider().getTextMessageWithFormat("command.kit.list.onetime", kitName)))
                            .style(TextStyles.STRIKETHROUGH).build();
                }

                // If an intervalOld is used...
                Duration interval = kitObj.getCooldown().orElse(Duration.ZERO);
                if (!interval.isZero() && !this.kitPermissionHandler.testCooldownExempt(source)) {

                    // Get the next time the kit can be used.
                    Instant next = lastRedeem.plus(interval);
                    if (next.isAfter(Instant.now())) {
                        // Get the time to next usage.
                        String time = Util.getTimeToNow(next);
                        return tb.color(TextColors.RED)
                                .onHover(TextActions.showText(
                                        Nucleus.getNucleus().getMessageProvider()
                                                .getTextMessageWithFormat("command.kit.list.interval", kitName, time)))
                                .style(TextStyles.STRIKETHROUGH).build();
                    }
                }
            }
        }

        // Can use.
        Text.Builder builder = tb.color(TextColors.AQUA).onClick(TextActions.runCommand("/kit \"" + kitName + "\""))
                .onHover(TextActions.showText(Nucleus.getNucleus().getMessageProvider().getTextMessageWithFormat("command.kit.list.text", kitName)))
                .style(TextStyles.ITALIC);
        if (kitObj.getCost() > 0 && Nucleus.getNucleus().getEconHelper().economyServiceExists() && !this.kitPermissionHandler.testCostExempt(source)) {
            builder = Text.builder().append(builder.build())
                .append(Nucleus.getNucleus().getMessageProvider().getTextMessageWithFormat("command.kit.list.cost",
                        Nucleus.getNucleus().getEconHelper().getCurrencySymbol(kitObj.getCost())));
        }

        return builder.build();
    }

}
