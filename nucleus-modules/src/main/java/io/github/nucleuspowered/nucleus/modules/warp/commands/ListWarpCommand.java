/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.warp.commands;

import io.github.nucleuspowered.nucleus.core.Util;
import io.github.nucleuspowered.nucleus.api.module.warp.data.Warp;
import io.github.nucleuspowered.nucleus.api.module.warp.data.WarpCategory;
import io.github.nucleuspowered.nucleus.modules.warp.WarpPermissions;
import io.github.nucleuspowered.nucleus.modules.warp.config.WarpConfig;
import io.github.nucleuspowered.nucleus.modules.warp.services.WarpService;
import io.github.nucleuspowered.nucleus.core.scaffold.command.ICommandContext;
import io.github.nucleuspowered.nucleus.core.scaffold.command.ICommandExecutor;
import io.github.nucleuspowered.nucleus.core.scaffold.command.ICommandResult;
import io.github.nucleuspowered.nucleus.core.scaffold.command.annotation.Command;
import io.github.nucleuspowered.nucleus.core.services.INucleusServiceCollection;
import io.github.nucleuspowered.nucleus.core.services.interfaces.IReloadableService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.LinearComponents;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextDecoration;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.api.adventure.SpongeComponents;
import org.spongepowered.api.command.exception.CommandException;
import org.spongepowered.api.command.parameter.managed.Flag;
import org.spongepowered.api.world.Location;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Command(
        aliases = {"list", "#warps"},
        basePermission = WarpPermissions.BASE_WARP_LIST,
        commandDescriptionKey = "warp.list",
        parentCommand = WarpCommand.class,
        associatedPermissions = WarpPermissions.PERMISSIONS_WARPS
)
public class ListWarpCommand implements ICommandExecutor, IReloadableService.Reloadable {

    private static final Component DASH = Component.text("-", NamedTextColor.GREEN);
    private boolean isDescriptionInList = true;
    private double defaultCost = 0;
    private String defaultName = "unknown";
    private boolean isSeparatePerms = true;
    private boolean isCategorise = false;

    @Override
    public Flag[] flags(final INucleusServiceCollection serviceCollection) {
        return new Flag[] {
                Flag.of("u")
        };
    }

    @Override public ICommandResult execute(final ICommandContext context) throws CommandException {
        final WarpService service = context.getServiceCollection().getServiceUnchecked(WarpService.class);
        if (service.getWarpNames().isEmpty()) {
            return context.errorResult("command.warps.list.nowarps");
        }

        return !context.hasFlag("u") && this.isCategorise ? this.categories(service, context) : this.noCategories(service, context);
    }

    private boolean canView(final ICommandContext context, final String warp) {
        return !this.isSeparatePerms || context.testPermission(WarpPermissions.getWarpPermission(warp));
    }

    private ICommandResult categories(final WarpService service, final ICommandContext context) {
        // Get the warp list.
        final Map<WarpCategory, List<Warp>> warps = service.getWarpsWithCategories(x -> this.canView(context, x.getNamedLocation().getName()));
        this.createMain(context, warps);
        return context.successResult();
    }

    private void createMain(final ICommandContext context, final Map<WarpCategory, List<Warp>> warps) {
        final List<Component> lt = warps.keySet().stream().filter(Objects::nonNull)
                .sorted(Comparator.comparing(WarpCategory::getId))
                .map(s -> {
                    final TextComponent.Builder t = Component.text().content("> ").color(NamedTextColor.GREEN)
                            .style(Style.style(TextDecoration.ITALIC))
                            .append(s.getDisplayName())
                            .clickEvent(SpongeComponents.executeCallback(source -> this.createSub(context, s, warps)));
                    s.getDescription().ifPresent(x -> t.append(Component.text(" - ")).append(x));
                    return t.build();
                })
                .collect(Collectors.toList());

        // Uncategorised
        if (warps.containsKey(null)) {
            lt.add(Component.text().content("> " + this.defaultName).color(NamedTextColor.GREEN).style(Style.style(TextDecoration.ITALIC))
                .clickEvent(SpongeComponents.executeCallback(source -> this.createSub(context, null, warps))).build());
        }

        Util.getPaginationBuilder(context.audience())
            .header(context.getMessage("command.warps.list.headercategory"))
            .title(context.getMessage("command.warps.list.maincategory")).padding(ListWarpCommand.DASH)
            .contents(lt)
            .sendTo(context.audience());
    }

    private void createSub(final ICommandContext context,
            @Nullable final WarpCategory category, final Map<WarpCategory, List<Warp>> warpDataList) {
        final boolean econExists = context.getServiceCollection().economyServiceProvider().serviceExists();
        final Component name = category == null ? Component.text(this.defaultName) : category.getDisplayName();

        final List<Component> lt = warpDataList.get(category).stream().sorted(Comparator.comparing(x -> x.getNamedLocation().getName()))
            .map(s -> this.createWarp(s, s.getNamedLocation().getName(), econExists, this.defaultCost, context)).collect(Collectors.toList());

        Util.getPaginationBuilder(context.audience())
            .title(context.getMessage("command.warps.list.category", name)).padding(ListWarpCommand.DASH)
            .contents(lt)
            .footer(context.getMessage("command.warps.list.back")
                .clickEvent(SpongeComponents.executeCallback(s -> this.createMain(context, warpDataList))))
            .sendTo(context.audience());
    }

    private ICommandResult noCategories(final WarpService service, final ICommandContext context) {
        // Get the warp list.
        final Set<String> ws = service.getWarpNames();
        final boolean econExists = context.getServiceCollection().economyServiceProvider().serviceExists();
        final List<Component> lt = ws.stream().filter(s -> this.canView(context, s.toLowerCase())).sorted(String::compareTo).map(s -> {
            final Optional<Warp> wd = service.getWarp(s);
            return this.createWarp(wd.orElse(null), s, econExists, this.defaultCost, context);
        }).collect(Collectors.toList());

        Util.getPaginationBuilder(context.audience())
            .title(context.getMessage("command.warps.list.header")).padding(ListWarpCommand.DASH)
            .contents(lt)
            .sendTo(context.audience());

        return context.successResult();
    }

    private TextComponent createWarp(@Nullable final Warp data, final String name, final boolean econExists, final double defaultCost,
            final ICommandContext context) {
        if (data == null) {
            return Component.text("No warp exists with the name " + name, NamedTextColor.RED);
        }

        final String pos = data.getNamedLocation().getLocation().map(Location::blockPosition).orElseGet(() -> data.getNamedLocation().getPosition().toInt()).toString();
        final String worldName = data.getNamedLocation().getWorldResourceKey().asString();

        final TextComponent.Builder inner = Component.text().content(name).color(NamedTextColor.GREEN).style(Style.style(TextDecoration.ITALIC))
                .clickEvent(ClickEvent.runCommand("/warp \"" + name + "\""));

        final TextComponent.Builder tb;
        final Optional<Component> description = data.getDescription();
        if (this.isDescriptionInList) {
            final TextComponent.Builder hoverBuilder = Component.text()
                    .append(context.getMessage("command.warps.warpprompt", name))
                    .append(Component.newline())
                    .append(context.getMessage("command.warps.warplochover",
                            worldName,
                            pos));

            if (econExists) {
                final double cost = data.getCost().orElse(defaultCost);
                if (cost > 0) {
                    hoverBuilder
                        .append(Component.newline())
                        .append(context.getMessage("command.warps.list.costhover",
                            context.getServiceCollection().economyServiceProvider().getCurrencySymbol(cost)));
                }
            }

            tb = Component.text().append(inner.hoverEvent(HoverEvent.showText(hoverBuilder.build())).build());
            description.ifPresent(text -> tb.append(Component.text(" - ", NamedTextColor.WHITE)).append(text));
        } else {
            if (description.isPresent()) {
                inner.hoverEvent(HoverEvent.showText(
                        LinearComponents.linear(
                                context.getMessage("command.warps.warpprompt", name),
                                Component.newline(),
                                description.get()
                        )));
            } else {
                inner.hoverEvent(HoverEvent.showText(context.getMessage("command.warps.warpprompt", name)));
            }

            tb = Component.text().append(inner.build())
                            .append(context.getMessage("command.warps.warploc",
                                    worldName,
                                    pos
                            ));

            if (econExists) {
                final double cost = data.getCost().orElse(defaultCost);
                if (cost > 0) {
                    tb.append(context.getMessage("command.warps.list.cost",
                            context.getServiceCollection().economyServiceProvider().getCurrencySymbol(cost)));
                }
            }
        }

        return tb.build();
    }

    @Override public void onReload(final INucleusServiceCollection serviceCollection) {
        final WarpConfig warpConfig = serviceCollection.configProvider().getModuleConfig(WarpConfig.class);
        this.defaultName = warpConfig.getDefaultName();
        this.defaultCost = warpConfig.getDefaultWarpCost();
        this.isDescriptionInList = warpConfig.isDescriptionInList();
        this.isCategorise = warpConfig.isCategoriseWarps();
        this.isSeparatePerms = warpConfig.isSeparatePermissions();
    }
}
