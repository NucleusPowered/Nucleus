/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.warp.commands;

import com.google.common.collect.Lists;
import io.github.nucleuspowered.nucleus.Nucleus;
import io.github.nucleuspowered.nucleus.Util;
import io.github.nucleuspowered.nucleus.api.nucleusdata.WarpCategory;
import io.github.nucleuspowered.nucleus.argumentparsers.WarpCategoryArgument;
import io.github.nucleuspowered.nucleus.internal.annotations.RunAsync;
import io.github.nucleuspowered.nucleus.internal.annotations.command.NoModifiers;
import io.github.nucleuspowered.nucleus.internal.annotations.command.Permissions;
import io.github.nucleuspowered.nucleus.internal.annotations.command.RegisterCommand;
import io.github.nucleuspowered.nucleus.internal.annotations.command.Scan;
import io.github.nucleuspowered.nucleus.internal.command.AbstractCommand;
import io.github.nucleuspowered.nucleus.modules.warp.services.WarpService;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.text.serializer.TextSerializers;
import org.spongepowered.api.util.annotation.NonnullByDefault;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Scan
@Permissions(prefix = "warp")
@RegisterCommand(value = {"category"}, subcommandOf = WarpCommand.class, hasExecutor = false)
@NonnullByDefault
public class CategoryCommand extends AbstractCommand<CommandSource> {

    private static final String key = "category";
    private static final String displayname = "displayname";
    private static final String description = "description";

    @Override protected CommandResult executeCommand(CommandSource src, CommandContext args, Cause cause) {
        return CommandResult.empty();
    }

    @Permissions(prefix = "warp.category", mainOverride = "list")
    @RunAsync
    @NoModifiers
    @RegisterCommand(value = {"list"}, subcommandOf = CategoryCommand.class)
    public static class ListCategoryCommand extends AbstractCommand<CommandSource> {

        private final WarpService handler = getServiceUnchecked(WarpService.class);

        @Override protected CommandResult executeCommand(CommandSource src, CommandContext args, Cause cause) {
            // Get all the categories.
            Util.getPaginationBuilder(src).contents(
                    this.handler.getWarpsWithCategories().keySet().stream().filter(Objects::nonNull)
                    .sorted(Comparator.comparing(WarpCategory::getId)).map(x -> {
                List<Text> t = Lists.newArrayList();
                t.add(Nucleus.getNucleus().getMessageProvider().getTextMessageWithTextFormat("command.warp.category.listitem.simple",
                        Text.of(x.getId()), x.getDisplayName()));
                x.getDescription().ifPresent(y ->
                        t.add(Nucleus.getNucleus().getMessageProvider().getTextMessageWithTextFormat("command.warp.category.listitem.description", y)));
                return t;
            }).flatMap(Collection::stream).collect(Collectors.toList()))
            .title(Nucleus.getNucleus().getMessageProvider().getTextMessageWithFormat("command.warp.category.listitem.title"))
            .padding(Text.of("-", TextColors.GREEN))
            .sendTo(src);

            return CommandResult.success();
        }
    }

    @Permissions(prefix = "warp.category", mainOverride = "displayname")
    @RunAsync
    @NoModifiers
    @RegisterCommand(value = {"setdisplayname"}, subcommandOf = CategoryCommand.class)
    public static class CategoryDisplayNameCommand extends AbstractCommand<CommandSource> {

        private final WarpService handler = getServiceUnchecked(WarpService.class);

        @Override public CommandElement[] getArguments() {
            return new CommandElement[] {
                    new WarpCategoryArgument(Text.of(key), this.handler),
                    GenericArguments.onlyOne(GenericArguments.string(Text.of(displayname)))
            };
        }

        @Override protected CommandResult executeCommand(CommandSource src, CommandContext args, Cause cause) {
            WarpCategory category = args.<WarpCategory>getOne(key).get();
            String displayName = args.<String>getOne(displayname).get();
            this.handler.setWarpCategoryDisplayName(category.getId(), TextSerializers.FORMATTING_CODE.deserialize(args.<String>getOne(displayname).get()));
            src.sendMessage(Nucleus.getNucleus().getMessageProvider().getTextMessageWithFormat("command.warp.category.displayname.set", category.getId(),
                    displayName));
            return CommandResult.success();
        }
    }

    @Permissions(prefix = "warp.category", mainOverride = "displayname")
    @RunAsync
    @NoModifiers
    @RegisterCommand(value = {"removedisplayname"}, subcommandOf = CategoryCommand.class)
    public static class CategoryRemoveDisplayNameCommand extends AbstractCommand<CommandSource> {

        private final WarpService handler = getServiceUnchecked(WarpService.class);

        @Override public CommandElement[] getArguments() {
            return new CommandElement[] {
                    new WarpCategoryArgument(Text.of(key), this.handler)
            };
        }

        @Override protected CommandResult executeCommand(CommandSource src, CommandContext args, Cause cause) {
            WarpCategory category = args.<WarpCategory>getOne(key).get();
            this.handler.setWarpCategoryDisplayName(category.getId(), null);
            src.sendMessage(Nucleus.getNucleus().getMessageProvider().getTextMessageWithFormat("command.warp.category.displayname.removed", category.getId()));
            return CommandResult.success();
        }
    }

    @Permissions(prefix = "warp.category", mainOverride = "description")
    @RunAsync
    @NoModifiers
    @RegisterCommand(value = {"setdescription"}, subcommandOf = CategoryCommand.class)
    public static class CategoryDescriptionCommand extends AbstractCommand<CommandSource> {

        private final WarpService handler = getServiceUnchecked(WarpService.class);

        @Override public CommandElement[] getArguments() {
            return new CommandElement[] {
                    new WarpCategoryArgument(Text.of(key), this.handler),
                    GenericArguments.onlyOne(GenericArguments.string(Text.of(description)))
            };
        }

        @Override protected CommandResult executeCommand(CommandSource src, CommandContext args, Cause cause) {
            WarpCategory category = args.<WarpCategory>getOne(key).get();
            String d = args.<String>getOne(description).get();
            this.handler.setWarpCategoryDescription(category.getId(), TextSerializers.FORMATTING_CODE.deserialize(d));
            src.sendMessage(Nucleus.getNucleus().getMessageProvider().getTextMessageWithFormat("command.warp.category.description.set", category.getId(), d));
            return CommandResult.success();
        }
    }

    @Permissions(prefix = "warp.category", mainOverride = "description")
    @RunAsync
    @NoModifiers
    @RegisterCommand(value = {"removedescription"}, subcommandOf = CategoryCommand.class)
    public static class CategoryRemoveDescriptionCommand extends AbstractCommand<CommandSource> {

        private final WarpService handler = getServiceUnchecked(WarpService.class);

        @Override public CommandElement[] getArguments() {
            return new CommandElement[] {
                    new WarpCategoryArgument(Text.of(key), this.handler)
            };
        }

        @Override protected CommandResult executeCommand(CommandSource src, CommandContext args, Cause cause) {
            WarpCategory category = args.<WarpCategory>getOne(key).get();
            this.handler.setWarpCategoryDescription(category.getId(), null);
            src.sendMessage(Nucleus.getNucleus().getMessageProvider().getTextMessageWithFormat("command.warp.category.description.removed", category.getId()));
            return CommandResult.success();
        }
    }
}
