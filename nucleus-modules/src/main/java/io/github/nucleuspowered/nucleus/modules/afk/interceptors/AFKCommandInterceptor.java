/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.afk.interceptors;

import io.github.nucleuspowered.nucleus.api.text.NucleusTextTemplate;
import io.github.nucleuspowered.nucleus.modules.afk.config.AFKConfig;
import io.github.nucleuspowered.nucleus.modules.afk.events.AFKEvents;
import io.github.nucleuspowered.nucleus.modules.afk.services.AFKHandler;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandContext;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandExecutor;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandInterceptor;
import io.github.nucleuspowered.nucleus.scaffold.command.ICommandResult;
import io.github.nucleuspowered.nucleus.scaffold.command.annotation.NotifyIfAFK;
import io.github.nucleuspowered.nucleus.scaffold.command.control.CommandControl;
import io.github.nucleuspowered.nucleus.services.INucleusServiceCollection;
import io.github.nucleuspowered.nucleus.services.interfaces.IReloadableService;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.event.CauseStackManager;
import org.spongepowered.api.text.Text;

import java.util.Objects;

import javax.annotation.Nullable;

public class AFKCommandInterceptor implements ICommandInterceptor, IReloadableService.Reloadable {

    @Nullable private NucleusTextTemplate message = null;
    private boolean send = false;

    @Override public void onPreCommand(Class<? extends ICommandExecutor<?>> commandClass, CommandControl commandControl,
            ICommandContext<? extends CommandSource> context) { }

    @Override public void onPostCommand(Class<? extends ICommandExecutor<?>> commandClass, CommandControl commandControl,
            ICommandContext<? extends CommandSource> context, ICommandResult result) {
        if (this.send && result.isSuccess() && commandClass.isAnnotationPresent(NotifyIfAFK.class)) {
            NotifyIfAFK annotation = commandClass.getAnnotation(NotifyIfAFK.class);
            final AFKHandler handler = context.getServiceCollection().getServiceUnchecked(AFKHandler.class);
            try (CauseStackManager.StackFrame frame = Sponge.getCauseStackManager().pushCauseFrame()) {
                for (String key : annotation.value()) {
                    context.getAll(key, Object.class)
                            .stream()
                            .filter(x -> x instanceof User)
                            .map(x -> ((User) x).getPlayer().orElse(null))
                            .filter(Objects::nonNull)
                            .filter(handler::isAFK)
                            .forEach(x -> {
                                Text messageToSend = this.message == null ? null : this.message.getForCommandSource(x);
                                AFKEvents.Notify event = new AFKEvents.Notify(x, messageToSend, context.getCause());
                                Sponge.getEventManager().post(event);
                                event.getMessage().ifPresent(message -> {
                                    try {
                                        context.getCommandSource().sendMessage(message);
                                    } catch (CommandException e) {
                                        e.printStackTrace();
                                    }
                                });
                            });
                }
            }
        }
    }

    @Override
    public void onReload(INucleusServiceCollection serviceCollection) {
        AFKConfig config =
                serviceCollection.moduleDataProvider().getModuleConfig(AFKConfig.class);
        if (config.isAlertSenderOnAfk()) {
            NucleusTextTemplate textTemplate = config.getMessages().getOnCommand();
            if (textTemplate == null || textTemplate.isEmpty()) { // NPE has occurred here in the past due to an empty message.
                this.message = null;
            } else {
                this.message = textTemplate;
            }

            this.send = true;
        } else {
            this.message = null;
            this.send = false;
        }
    }
}
