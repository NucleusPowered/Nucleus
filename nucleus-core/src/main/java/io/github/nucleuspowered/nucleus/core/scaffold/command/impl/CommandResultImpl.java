/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.core.scaffold.command.impl;

import com.google.common.base.Preconditions;
import io.github.nucleuspowered.nucleus.core.scaffold.command.ICommandContext;
import io.github.nucleuspowered.nucleus.core.scaffold.command.ICommandResult;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.api.command.CommandResult;

import java.util.Optional;

public class CommandResultImpl implements ICommandResult {

    public static final ICommandResult SUCCESS = new CommandResultImpl(true, false);

    public static final ICommandResult WILL_CONTINUE = new CommandResultImpl(false, true);

    public static final ICommandResult FAILURE = new CommandResultImpl(false, false);

    @Nullable private final String key;
    @Nullable private final Object[] args;
    private final boolean success;
    private final boolean willContinue;

    private CommandResultImpl(final boolean success, final boolean willContinue) {
        this(success, willContinue, null, null);
    }

    public CommandResultImpl(final String key, final Object[] args) {
        this(
                false,
                false,
                Preconditions.checkNotNull(key),
                Preconditions.checkNotNull(args)
        );
    }

    private CommandResultImpl(
            final boolean success,
            final boolean willContinue,
            @Nullable final String key,
            @Nullable final Object[] args) {
        this.key = key;
        this.args = args;
        this.success = success;
        this.willContinue = willContinue;
    }

    @Override
    public boolean isSuccess() {
        return this.success;
    }

    @Override
    public boolean isWillContinue() {
        return this.willContinue;
    }

    @Override
    public Optional<Component> getErrorMessage(final ICommandContext source) {
        return Optional.of(source.getMessage(this.key, this.args));
    }

    @Override
    public CommandResult getResult(final ICommandContext source) {
        return this.success ? CommandResult.success() :
                this.getErrorMessage(source).map(CommandResult::error).orElseGet(() -> CommandResult.error(Component.text("Unknown error occurred", Style.style(NamedTextColor.RED))));
    }

    public static final class Literal extends CommandResultImpl {

        private final Component literal;

        public Literal(final Component literal) {
            super(false, false);
            this.literal = literal;
        }

        @Override
        public Optional<Component> getErrorMessage(final ICommandContext source) {
            return Optional.of(this.literal);
        }
    }
}
