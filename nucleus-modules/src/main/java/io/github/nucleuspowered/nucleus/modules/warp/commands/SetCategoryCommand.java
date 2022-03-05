/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.warp.commands;

import com.google.inject.Inject;
import io.github.nucleuspowered.nucleus.api.module.warp.data.WarpCategory;
import io.github.nucleuspowered.nucleus.modules.warp.WarpPermissions;
import io.github.nucleuspowered.nucleus.modules.warp.services.WarpService;
import io.github.nucleuspowered.nucleus.core.scaffold.command.ICommandContext;
import io.github.nucleuspowered.nucleus.core.scaffold.command.ICommandExecutor;
import io.github.nucleuspowered.nucleus.core.scaffold.command.ICommandResult;
import io.github.nucleuspowered.nucleus.core.scaffold.command.annotation.Command;
import io.github.nucleuspowered.nucleus.core.services.INucleusServiceCollection;
import io.leangen.geantyref.TypeToken;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import org.spongepowered.api.command.CommandCompletion;
import org.spongepowered.api.command.exception.ArgumentParseException;
import org.spongepowered.api.command.exception.CommandException;
import org.spongepowered.api.command.parameter.ArgumentReader;
import org.spongepowered.api.command.parameter.CommandContext;
import org.spongepowered.api.command.parameter.Parameter;
import org.spongepowered.api.command.parameter.managed.Flag;
import org.spongepowered.api.command.parameter.managed.ValueParameter;
import org.spongepowered.api.util.Tuple;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Command(
        aliases = {"setcategory"},
        basePermission = WarpPermissions.BASE_WARP_SETCATEGORY,
        commandDescriptionKey = "warp.setcategory",
        parentCommand = WarpCommand.class
)
public class SetCategoryCommand implements ICommandExecutor {

    private static final TypeToken<Tuple<String, Boolean>> TUPLE_TYPE_TOKEN = new TypeToken<Tuple<String, Boolean>>() {};

    private final Parameter.Value<Tuple<String, Boolean>> category;

    @Inject
    public SetCategoryCommand(final INucleusServiceCollection serviceCollection) {
        this.category = Parameter.builder(SetCategoryCommand.TUPLE_TYPE_TOKEN)
                .addParser(new SetCategoryWarpCategoryArgument(serviceCollection.getServiceUnchecked(WarpService.class)))
                .key("category")
                .optional()
                .build();
    }

    @Override public Flag[] flags(final INucleusServiceCollection serviceCollection) {
        return new Flag[] {
                Flag.of("r", "remove", "delete"),
                Flag.of("n", "new")
        };
    }

    @Override public Parameter[] parameters(final INucleusServiceCollection serviceCollection) {
        return new Parameter[] {
                serviceCollection.getServiceUnchecked(WarpService.class).warpElement(false),
                this.category
        };
    }

    @Override public ICommandResult execute(final ICommandContext context) throws CommandException {
        final String warpName = context.requireOne(context.getServiceCollection().getServiceUnchecked(WarpService.class).warpElement(false)).getNamedLocation().getName();
        final WarpService handler = context.getServiceCollection().getServiceUnchecked(WarpService.class);
        if (context.hasFlag("r")) {
            // Remove the category.
            if (handler.setWarpCategory(warpName, null)) {
                context.sendMessage("command.warp.category.removed", warpName);
                return context.successResult();
            }

            return context.errorResult("command.warp.category.noremove", warpName);
        }

        final Optional<Tuple<String, Boolean>> categoryOp = context.getOne(this.category);
        if (!categoryOp.isPresent()) {
            return context.errorResult("command.warp.category.required");
        }

        final Tuple<String, Boolean> category = categoryOp.get();
        if (!context.hasFlag("n") && !category.second()) {
            context.sendMessageText(context.getMessage("command.warp.category.requirenew", category.first())
                    .clickEvent(ClickEvent.runCommand("/warp setcategory -n " + warpName + " " + category.first()))
            );

            return context.failResult();
        }

        // Add the category.
        if (handler.setWarpCategory(warpName, category.first())) {
            context.sendMessage("command.warp.category.added", category.first(), warpName);
            return context.successResult();
        }

        return context.errorResult("command.warp.category.couldnotadd", Component.text(category.first()), Component.text(warpName));
    }

    private static final class SetCategoryWarpCategoryArgument implements ValueParameter<Tuple<String, Boolean>> {

        private final WarpService service;

        private SetCategoryWarpCategoryArgument(final WarpService service) {
            this.service = service;
        }

        @Override public List<CommandCompletion> complete(final CommandContext context, final String currentInput) {
            return this.service.getWarpsWithCategories()
                    .keySet()
                    .stream()
                    .filter(Objects::nonNull)
                    .map(WarpCategory::getId)
                    .filter(x -> x.startsWith(currentInput))
                    .map(CommandCompletion::of)
                    .collect(Collectors.toList());
        }

        @Override
        public Optional<? extends Tuple<String, Boolean>> parseValue(final Parameter.Key<? super Tuple<String, Boolean>> parameterKey,
                final ArgumentReader.Mutable reader, final CommandContext.Builder context) throws ArgumentParseException {
            final String arg = reader.parseString();
            return Optional.of(Tuple.of(arg,
                    this.service.getWarpsWithCategories().keySet().stream().filter(Objects::nonNull).anyMatch(x -> x.getId().equals(arg))));
        }
    }
}
