/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.argumentparsers;

import io.github.nucleuspowered.nucleus.Nucleus;
import io.github.nucleuspowered.nucleus.api.nucleusdata.WarpCategory;
import io.github.nucleuspowered.nucleus.modules.warp.services.WarpService;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.ArgumentParseException;
import org.spongepowered.api.command.args.CommandArgs;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.text.Text;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class WarpCategoryArgument extends CommandElement {

    private final WarpService handler;

    public WarpCategoryArgument(@Nullable Text key, WarpService handler) {
        super(key);
        this.handler = handler;
    }

    @Nullable @Override protected Object parseValue(@Nonnull CommandSource source, @Nonnull CommandArgs args) throws ArgumentParseException {
        String arg = args.next();
        return this.handler.getWarpsWithCategories().keySet().stream().filter(Objects::nonNull).filter(x -> x.getId().equals(arg)).findFirst()
                .orElseThrow(() -> args.createError(
                    Nucleus.getNucleus().getMessageProvider().getTextMessageWithFormat("args.warpcategory.noexist", arg)));
    }

    @Nonnull @Override public List<String> complete(@Nonnull CommandSource src, @Nonnull CommandArgs args, @Nonnull CommandContext context) {
        return this.handler.getWarpsWithCategories().keySet().stream().filter(Objects::nonNull).map(WarpCategory::getId).collect(Collectors.toList());
    }
}
