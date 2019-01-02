/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.warp.commands;

import io.github.nucleuspowered.nucleus.Nucleus;
import io.github.nucleuspowered.nucleus.Util;
import io.github.nucleuspowered.nucleus.api.nucleusdata.Warp;
import io.github.nucleuspowered.nucleus.api.nucleusdata.WarpCategory;
import io.github.nucleuspowered.nucleus.internal.PermissionRegistry;
import io.github.nucleuspowered.nucleus.internal.annotations.RunAsync;
import io.github.nucleuspowered.nucleus.internal.annotations.command.Permissions;
import io.github.nucleuspowered.nucleus.internal.annotations.command.RegisterCommand;
import io.github.nucleuspowered.nucleus.internal.command.AbstractCommand;
import io.github.nucleuspowered.nucleus.internal.interfaces.Reloadable;
import io.github.nucleuspowered.nucleus.internal.messages.MessageProvider;
import io.github.nucleuspowered.nucleus.internal.permissions.SuggestedLevel;
import io.github.nucleuspowered.nucleus.modules.warp.config.WarpConfig;
import io.github.nucleuspowered.nucleus.modules.warp.config.WarpConfigAdapter;
import io.github.nucleuspowered.nucleus.modules.warp.services.WarpService;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.action.TextActions;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.text.format.TextStyles;
import org.spongepowered.api.util.annotation.NonnullByDefault;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Lists all the warps that a subject can access.
 */
@Permissions(prefix = "warp", suggestedLevel = SuggestedLevel.USER)
@RunAsync
@NonnullByDefault
@RegisterCommand(value = {"list"}, subcommandOf = WarpCommand.class, rootAliasRegister = "warps")
public class ListWarpCommand extends AbstractCommand<CommandSource> implements Reloadable {

    private final WarpService service = getServiceUnchecked(WarpService.class);
    private boolean isDescriptionInList = true;
    private double defaultCost = 0;
    private String defaultName = "unknown";
    private boolean isSeparatePerms = true;
    private boolean isCategorise = false;

    @Override public CommandElement[] getArguments() {
        return new CommandElement[] {
            GenericArguments.flags().flag("u").buildWith(GenericArguments.none())
        };
    }

    @Override
    public CommandResult executeCommand(final CommandSource src, CommandContext args, Cause cause) {
        if (this.service.getWarpNames().isEmpty()) {
            src.sendMessage(Nucleus.getNucleus().getMessageProvider().getTextMessageWithFormat("command.warps.list.nowarps"));
            return CommandResult.empty();
        }

        return !args.hasAny("u") && this.isCategorise ? categories(src) : noCategories(src);
    }

    private boolean canView(CommandSource src, String warp) {
        return !this.isSeparatePerms || hasPermission(src, PermissionRegistry.PERMISSIONS_PREFIX + "warps." + warp.toLowerCase());
    }

    private CommandResult categories(final CommandSource src) {
        // Get the warp list.
        Map<WarpCategory, List<Warp>> warps = this.service.getWarpsWithCategories(x -> canView(src, x.getName()));
        createMain(src, warps);
        return CommandResult.success();
    }

    private void createMain(final CommandSource src, final Map<WarpCategory, List<Warp>> warps) {
        List<Text> lt = warps.keySet().stream().filter(Objects::nonNull)
                .sorted(Comparator.comparing(WarpCategory::getId))
                .map(s -> {
                    Text.Builder t = Text.builder("> ").color(TextColors.GREEN).style(TextStyles.ITALIC)
                            .append(s.getDisplayName())
                            .onClick(TextActions.executeCallback(source -> createSub(source, s, warps)));
                    s.getDescription().ifPresent(x -> t.append(Text.of(" - ")).append(Text.of(TextColors.RESET, TextStyles.NONE, x)));
                    return t.build();
                })
                .collect(Collectors.toList());

        // Uncategorised
        if (warps.containsKey(null)) {
            lt.add(Text.builder("> " + this.defaultName).color(TextColors.GREEN).style(TextStyles.ITALIC)
                .onClick(TextActions.executeCallback(source -> createSub(source, null, warps))).build());
        }

        MessageProvider messageProvider = Nucleus.getNucleus().getMessageProvider();
        Util.getPaginationBuilder(src)
            .header(messageProvider.getTextMessageWithFormat("command.warps.list.headercategory"))
            .title(messageProvider.getTextMessageWithFormat("command.warps.list.maincategory")).padding(Text.of(TextColors.GREEN, "-"))
            .contents(lt)
            .sendTo(src);
    }

    private void createSub(final CommandSource src, @Nullable final WarpCategory category, final Map<WarpCategory, List<Warp>> warpDataList) {
        final boolean econExists = Nucleus.getNucleus().getEconHelper().economyServiceExists();
        Text name = category == null ? Text.of(this.defaultName) : category.getDisplayName();

        List<Text> lt = warpDataList.get(category).stream().sorted(Comparator.comparing(Warp::getName))
            .map(s -> createWarp(s, s.getName(), econExists, this.defaultCost)).collect(Collectors.toList());

        Util.getPaginationBuilder(src)
            .title(Nucleus.getNucleus().getMessageProvider().getTextMessageWithTextFormat("command.warps.list.category", name)).padding(Text.of(TextColors.GREEN, "-"))
            .contents(lt)
            .footer(Nucleus.getNucleus().getMessageProvider().getTextMessageWithFormat("command.warps.list.back").toBuilder()
                .onClick(TextActions.executeCallback(s -> createMain(s, warpDataList))).build())
            .sendTo(src);
    }

    private CommandResult noCategories(final CommandSource src) {
        // Get the warp list.
        Set<String> ws = this.service.getWarpNames();
        final boolean econExists = Nucleus.getNucleus().getEconHelper().economyServiceExists();
        List<Text> lt = ws.stream().filter(s -> canView(src, s.toLowerCase())).sorted(String::compareTo).map(s -> {
            Optional<Warp> wd = this.service.getWarp(s);
            return createWarp(wd.orElse(null), s, econExists, this.defaultCost);
        }).collect(Collectors.toList());

        Util.getPaginationBuilder(src)
            .title(Nucleus.getNucleus().getMessageProvider().getTextMessageWithFormat("command.warps.list.header")).padding(Text.of(TextColors.GREEN, "-"))
            .contents(lt)
            .sendTo(src);

        return CommandResult.success();
    }

    private Text createWarp(@Nullable Warp data, String name, boolean econExists, double defaultCost) {
        if (data == null || !data.getLocation().isPresent()) {
            return Text.builder(name).color(TextColors.RED).onHover(TextActions.showText(Nucleus.getNucleus().getMessageProvider().getTextMessageWithFormat
                    ("command.warps.unavailable"))).build();
        }

        Location<World> world = data.getLocation().get();

        Text.Builder inner = Text.builder(name).color(TextColors.GREEN).style(TextStyles.ITALIC)
                .onClick(TextActions.runCommand("/warp \"" + name + "\""));

        Text.Builder tb;
        Optional<Text> description = data.getDescription();
        if (this.isDescriptionInList) {
            Text.Builder hoverBuilder = Text.builder()
                    .append(Nucleus.getNucleus().getMessageProvider().getTextMessageWithFormat("command.warps.warpprompt", name))
                    .append(Text.NEW_LINE)
                    .append(Nucleus.getNucleus().getMessageProvider().getTextMessageWithFormat("command.warps.warplochover", world.getExtent().getName(),
                            world.getBlockPosition().toString()));

            if (econExists) {
                double cost = data.getCost().orElse(defaultCost);
                if (cost > 0) {
                    hoverBuilder
                        .append(Text.NEW_LINE)
                        .append(Nucleus.getNucleus().getMessageProvider().getTextMessageWithFormat("command.warps.list.costhover", Nucleus.getNucleus().getEconHelper()
                        .getCurrencySymbol(cost)));
                }
            }

            tb = Text.builder().append(inner.onHover(TextActions.showText(hoverBuilder.build())).build());
            description.ifPresent(text -> tb.append(Text.of(TextColors.WHITE, " - ")).append(text));
        } else {
            if (description.isPresent()) {
                inner.onHover(TextActions.showText(
                        Text.of(
                                Nucleus.getNucleus().getMessageProvider().getTextMessageWithFormat("command.warps.warpprompt", name),
                                Text.NEW_LINE,
                                description.get()
                        )));
            } else {
                inner.onHover(TextActions.showText(Nucleus.getNucleus().getMessageProvider().getTextMessageWithFormat("command.warps.warpprompt", name)));
            }

            tb = Text.builder().append(inner.build())
                            .append(Nucleus.getNucleus().getMessageProvider().getTextMessageWithFormat("command.warps.warploc",
                                    world.getExtent().getName(), world.getBlockPosition().toString()
                            ));

            if (econExists) {
                double cost = data.getCost().orElse(defaultCost);
                if (cost > 0) {
                    tb.append(Nucleus.getNucleus().getMessageProvider().getTextMessageWithFormat("command.warps.list.cost",
                            Nucleus.getNucleus().getEconHelper().getCurrencySymbol(cost)));
                }
            }
        }

        return tb.build();
    }

    @Override public void onReload() {
        WarpConfig warpConfig = getServiceUnchecked(WarpConfigAdapter.class).getNodeOrDefault();
        this.defaultName = warpConfig.getDefaultName();
        this.defaultCost = warpConfig.getDefaultWarpCost();
        this.isDescriptionInList = warpConfig.isDescriptionInList();
        this.isCategorise = warpConfig.isCategoriseWarps();
        this.isSeparatePerms = warpConfig.isSeparatePermissions();
    }
}
