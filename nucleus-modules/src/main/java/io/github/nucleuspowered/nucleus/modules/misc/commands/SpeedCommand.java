/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.misc.commands;

import io.github.nucleuspowered.nucleus.modules.misc.MiscPermissions;
import io.github.nucleuspowered.nucleus.modules.misc.config.MiscConfig;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandContext;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandExecutor;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandResult;
import io.github.nucleuspowered.nucleus.scaffold.command.annotation.Command;
import io.github.nucleuspowered.nucleus.scaffold.command.annotation.CommandModifier;
import io.github.nucleuspowered.nucleus.scaffold.command.annotation.EssentialsEquivalent;
import io.github.nucleuspowered.nucleus.scaffold.command.modifier.CommandModifiers;
import io.github.nucleuspowered.nucleus.services.INucleusServiceCollection;
import io.github.nucleuspowered.nucleus.services.interfaces.IReloadableService;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.data.DataTransactionResult;
import org.spongepowered.api.data.key.Key;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.data.value.mutable.Value;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.util.annotation.NonnullByDefault;

import java.util.HashMap;
import java.util.Map;

@NonnullByDefault
@EssentialsEquivalent(value = {"speed", "flyspeed", "walkspeed", "fspeed", "wspeed"}, isExact = false,
    notes = "This command either uses your current state or a specified argument to determine whether to alter fly or walk speed.")
@Command(
        aliases = "speed",
        basePermission = MiscPermissions.BASE_SPEED,
        commandDescriptionKey = "speed",
        modifiers = {
            @CommandModifier(value = CommandModifiers.HAS_WARMUP, exemptPermission = MiscPermissions.EXEMPT_WARMUP_SPEED),
            @CommandModifier(value = CommandModifiers.HAS_COOLDOWN, exemptPermission = MiscPermissions.EXEMPT_COOLDOWN_SPEED),
            @CommandModifier(value = CommandModifiers.HAS_COST, exemptPermission = MiscPermissions.EXEMPT_COST_SPEED)
        },
        associatedPermissions = {
                MiscPermissions.SPEED_EXEMPT_MAX,
                MiscPermissions.OTHERS_SPEED
        }
)
public class SpeedCommand implements ICommandExecutor<CommandSource>, IReloadableService.Reloadable { //extends AbstractCommand.SimpleTargetOtherPlayer

    private final String speedKey = "speed";
    private final String resetKey = "reset";
    private final String typeKey = "type";

    /**
     * As the standard flying speed is 0.05 and the standard walking speed is
     * 0.1, we multiply it by 20 and use integers. Standard walking speed is
     * therefore 2, standard flying speed - 1.
     */
    public static final int multiplier = 20;
    private int maxSpeed = 5;

    @Override
    public CommandElement[] parameters(INucleusServiceCollection serviceCollection) {
        Map<String, SpeedType> keysMap = new HashMap<>();
        keysMap.put("fly", SpeedType.FLYING);
        keysMap.put("flying", SpeedType.FLYING);
        keysMap.put("f", SpeedType.FLYING);

        keysMap.put("walk", SpeedType.WALKING);
        keysMap.put("w", SpeedType.WALKING);

        return new CommandElement[] {
                serviceCollection.commandElementSupplier().createOtherUserPermissionElement(true, MiscPermissions.OTHERS_SPEED),
                GenericArguments.optionalWeak(GenericArguments.onlyOne(GenericArguments.choices(Text.of(this.typeKey), keysMap, true))),
                GenericArguments.optional(
                        GenericArguments.firstParsing(
                            GenericArguments.integer(Text.of(this.speedKey)),
                            GenericArguments.literal(Text.of(this.resetKey), this.resetKey)
                        )
            )
        };
    }

    @Override
    public ICommandResult execute(ICommandContext<? extends CommandSource> context) throws CommandException {
        Player pl = context.getPlayerFromArgs();
        SpeedType key = context.getOne(this.typeKey, SpeedType.class)
                .orElseGet(() -> pl.get(Keys.IS_FLYING).orElse(false) ? SpeedType.FLYING : SpeedType.WALKING);
        Integer speed = context.getOne(this.speedKey, Integer.class).orElseGet(() -> {
            if (context.hasAny(this.resetKey)) {
                return key == SpeedType.WALKING ? 2 : 1;
            }

            return null;
        });

        if (speed == null) {
            Text t = Text.builder().append(context.getMessage("command.speed.walk")).append(Text.of(" "))
                    .append(Text.of(TextColors.YELLOW, Math.round(pl.get(Keys.WALKING_SPEED).orElse(0.1d) * 20)))
                    .append(Text.builder().append(Text.of(TextColors.GREEN, ", "))
                            .append(context.getMessage("command.speed.flying")).build())
                    .append(Text.of(" ")).append(Text.of(TextColors.YELLOW, Math.round(pl.get(Keys.FLYING_SPEED).orElse(0.05d) * 20)))
                    .append(Text.of(TextColors.GREEN, ".")).build();

            context.sendMessageText(t);

            // Don't trigger cooldowns
            return context.failResult();
        }

        if (speed < 0) {
            return context.errorResult("command.speed.negative");
        }

        if (!context.isConsoleAndBypass() && !context.testPermission(MiscPermissions.SPEED_EXEMPT_MAX) && this.maxSpeed < speed) {
            return context.errorResult("command.speed.max", String.valueOf(this.maxSpeed));
        }

        DataTransactionResult dtr = pl.offer(key.speedKey, (double) speed / (double) multiplier);

        if (dtr.isSuccessful()) {
            context.sendMessage("command.speed.success.base", key.name, String.valueOf(speed));

            if (!context.is(pl)) {
                context.sendMessage("command.speed.success.other", pl.getName(), key.name, String.valueOf(speed));
            }

            return context.successResult();
        }

        return context.errorResult("command.speed.fail", key.name);
    }

    @Override public void onReload(INucleusServiceCollection serviceCollection) {
        this.maxSpeed = serviceCollection.moduleDataProvider().getModuleConfig(MiscConfig.class).getMaxSpeed();
    }

    private enum SpeedType {
        WALKING(Keys.WALKING_SPEED, "loc:standard.walking"),
        FLYING(Keys.FLYING_SPEED, "loc:standard.flying");

        final Key<Value<Double>> speedKey;
        final String name;

        SpeedType(Key<Value<Double>> speedKey, String name) {
            this.speedKey = speedKey;
            this.name = name;
        }
    }
}
