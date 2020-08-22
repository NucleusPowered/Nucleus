/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.warp.commands;

import io.github.nucleuspowered.nucleus.Util;
import io.github.nucleuspowered.nucleus.api.module.warp.data.Warp;
import io.github.nucleuspowered.nucleus.api.module.warp.data.WarpCategory;
import io.github.nucleuspowered.nucleus.modules.warp.WarpPermissions;
import io.github.nucleuspowered.nucleus.modules.warp.config.WarpConfig;
import io.github.nucleuspowered.nucleus.modules.warp.services.WarpService;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandContext;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandExecutor;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandResult;
import io.github.nucleuspowered.nucleus.scaffold.command.annotation.Command;
import io.github.nucleuspowered.nucleus.services.INucleusServiceCollection;
import io.github.nucleuspowered.nucleus.services.interfaces.IReloadableService;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.action.TextActions;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.text.format.TextStyles;
import org.spongepowered.api.util.annotation.NonnullByDefault;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.storage.WorldProperties;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

@NonnullByDefault
@Command(
        aliases = {"list", "#warps"},
        basePermission = WarpPermissions.BASE_WARP_LIST,
        commandDescriptionKey = "warp.list",
        async = true,
        parentCommand = WarpCommand.class,
        associatedPermissions = WarpPermissions.PERMISSIONS_WARPS
)
public class ListWarpCommand implements ICommandExecutor<CommandSource>, IReloadableService.Reloadable {

    private boolean isDescriptionInList = true;
    private double defaultCost = 0;
    private String defaultName = "unknown";
    private boolean isSeparatePerms = true;
    private boolean isCategorise = false;

    @Override public CommandElement[] parameters(INucleusServiceCollection serviceCollection) {
        return new CommandElement[] {
            GenericArguments.flags().flag("u").buildWith(GenericArguments.none())
        };
    }

    @Override public ICommandResult execute(ICommandContext<? extends CommandSource> context) throws CommandException {
        WarpService service = context.getServiceCollection().getServiceUnchecked(WarpService.class);
        if (service.getWarpNames().isEmpty()) {
            return context.errorResult("command.warps.list.nowarps");
        }

        return !context.hasAny("u") && this.isCategorise ? categories(service, context) : noCategories(service, context);
    }

    private boolean canView(ICommandContext<? extends CommandSource> context, String warp) {
        return !this.isSeparatePerms || context.testPermission(WarpPermissions.getWarpPermission(warp));
    }

    private ICommandResult categories(final WarpService service, final ICommandContext<? extends CommandSource> context) {
        // Get the warp list.
        Map<WarpCategory, List<Warp>> warps = service.getWarpsWithCategories(x -> canView(context, x.getName()));
        createMain(context, warps);
        return context.successResult();
    }

    private void createMain(final ICommandContext<? extends CommandSource> context, final Map<WarpCategory, List<Warp>> warps) {
        List<Text> lt = warps.keySet().stream().filter(Objects::nonNull)
                .sorted(Comparator.comparing(WarpCategory::getId))
                .map(s -> {
                    Text.Builder t = Text.builder("> ").color(TextColors.GREEN).style(TextStyles.ITALIC)
                            .append(s.getDisplayName())
                            .onClick(TextActions.executeCallback(source -> createSub(context, s, warps)));
                    s.getDescription().ifPresent(x -> t.append(Text.of(" - ")).append(Text.of(TextColors.RESET, TextStyles.NONE, x)));
                    return t.build();
                })
                .collect(Collectors.toList());

        // Uncategorised
        if (warps.containsKey(null)) {
            lt.add(Text.builder("> " + this.defaultName).color(TextColors.GREEN).style(TextStyles.ITALIC)
                .onClick(TextActions.executeCallback(source -> createSub(context, null, warps))).build());
        }

        Util.getPaginationBuilder(context.getCommandSourceUnchecked())
            .header(context.getMessage("command.warps.list.headercategory"))
            .title(context.getMessage("command.warps.list.maincategory")).padding(Text.of(TextColors.GREEN, "-"))
            .contents(lt)
            .sendTo(context.getCommandSourceUnchecked());
    }

    private void createSub(final ICommandContext<? extends CommandSource> context,
            @Nullable final WarpCategory category, final Map<WarpCategory, List<Warp>> warpDataList) {
        final boolean econExists = context.getServiceCollection().economyServiceProvider().serviceExists();
        Text name = category == null ? Text.of(this.defaultName) : category.getDisplayName();

        List<Text> lt = warpDataList.get(category).stream().sorted(Comparator.comparing(Warp::getName))
            .map(s -> createWarp(s, s.getName(), econExists, this.defaultCost, context)).collect(Collectors.toList());

        Util.getPaginationBuilder(context.getCommandSourceUnchecked())
            .title(context.getMessage("command.warps.list.category", name)).padding(Text.of(TextColors.GREEN, "-"))
            .contents(lt)
            .footer(context.getMessage("command.warps.list.back").toBuilder()
                .onClick(TextActions.executeCallback(s -> createMain(context, warpDataList))).build())
            .sendTo(context.getCommandSourceUnchecked());
    }

    private ICommandResult noCategories(final WarpService service, final ICommandContext<? extends CommandSource> context) {
        // Get the warp list.
        Set<String> ws = service.getWarpNames();
        final boolean econExists = context.getServiceCollection().economyServiceProvider().serviceExists();
        List<Text> lt = ws.stream().filter(s -> canView(context, s.toLowerCase())).sorted(String::compareTo).map(s -> {
            Optional<Warp> wd = service.getWarp(s);
            return createWarp(wd.orElse(null), s, econExists, this.defaultCost, context);
        }).collect(Collectors.toList());

        Util.getPaginationBuilder(context.getCommandSourceUnchecked())
            .title(context.getMessage("command.warps.list.header")).padding(Text.of(TextColors.GREEN, "-"))
            .contents(lt)
            .sendTo(context.getCommandSourceUnchecked());

        return context.successResult();
    }

    private Text createWarp(@Nullable Warp data, String name, boolean econExists, double defaultCost,
            ICommandContext<? extends CommandSource> context) {
        if (data == null || !data.getWorldProperties().map(WorldProperties::isEnabled).orElse(false)) {
            return Text.builder(name).color(TextColors.RED)
                    .onHover(TextActions.showText(
                            context.getMessage("command.warps.unavailable"))).build();
        }

        String pos = data.getLocation().map(Location::getBlockPosition).orElseGet(() -> data.getPosition().toInt()).toString();
        String worldName = data.getWorldProperties().get().getWorldName();

        Text.Builder inner = Text.builder(name).color(TextColors.GREEN).style(TextStyles.ITALIC)
                .onClick(TextActions.runCommand("/warp \"" + name + "\""));

        Text.Builder tb;
        Optional<Text> description = data.getDescription();
        if (this.isDescriptionInList) {
            Text.Builder hoverBuilder = Text.builder()
                    .append(context.getMessage("command.warps.warpprompt", name))
                    .append(Text.NEW_LINE)
                    .append(context.getMessage("command.warps.warplochover",
                            worldName,
                            pos));

            if (econExists) {
                double cost = data.getCost().orElse(defaultCost);
                if (cost > 0) {
                    hoverBuilder
                        .append(Text.NEW_LINE)
                        .append(context.getMessage("command.warps.list.costhover",
                            context.getServiceCollection().economyServiceProvider().getCurrencySymbol(cost)));
                }
            }

            tb = Text.builder().append(inner.onHover(TextActions.showText(hoverBuilder.build())).build());
            description.ifPresent(text -> tb.append(Text.of(TextColors.WHITE, " - ")).append(text));
        } else {
            if (description.isPresent()) {
                inner.onHover(TextActions.showText(
                        Text.of(
                                context.getMessage("command.warps.warpprompt", name),
                                Text.NEW_LINE,
                                description.get()
                        )));
            } else {
                inner.onHover(TextActions.showText(context.getMessage("command.warps.warpprompt", name)));
            }

            tb = Text.builder().append(inner.build())
                            .append(context.getMessage("command.warps.warploc",
                                    worldName,
                                    pos
                            ));

            if (econExists) {
                double cost = data.getCost().orElse(defaultCost);
                if (cost > 0) {
                    tb.append(context.getMessage("command.warps.list.cost",
                            context.getServiceCollection().economyServiceProvider().getCurrencySymbol(cost)));
                }
            }
        }

        return tb.build();
    }

    @Override public void onReload(INucleusServiceCollection serviceCollection) {
        WarpConfig warpConfig = serviceCollection.moduleDataProvider().getModuleConfig(WarpConfig.class);
        this.defaultName = warpConfig.getDefaultName();
        this.defaultCost = warpConfig.getDefaultWarpCost();
        this.isDescriptionInList = warpConfig.isDescriptionInList();
        this.isCategorise = warpConfig.isCategoriseWarps();
        this.isSeparatePerms = warpConfig.isSeparatePermissions();
    }
}
