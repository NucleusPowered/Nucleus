/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.message;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import io.github.nucleuspowered.nucleus.Util;
import io.github.nucleuspowered.nucleus.api.service.NucleusPrivateMessagingService;
import io.github.nucleuspowered.nucleus.dataservices.UserService;
import io.github.nucleuspowered.nucleus.internal.messages.MessageProvider;
import io.github.nucleuspowered.nucleus.internal.qsml.module.ConfigurableModule;
import io.github.nucleuspowered.nucleus.modules.message.commands.SocialSpyCommand;
import io.github.nucleuspowered.nucleus.modules.message.config.MessageConfigAdapter;
import io.github.nucleuspowered.nucleus.modules.message.handlers.MessageHandler;
import org.spongepowered.api.Game;
import org.spongepowered.api.text.Text;
import uk.co.drnaylor.quickstart.annotations.ModuleData;

import java.util.List;
import java.util.Optional;

@ModuleData(id = MessageModule.ID, name = "Message")
public class MessageModule extends ConfigurableModule<MessageConfigAdapter> {

    public static final String ID = "message";

    @Inject private Game game;

    @Override
    public MessageConfigAdapter createAdapter() {
        return new MessageConfigAdapter();
    }

    @Override
    protected void performPreTasks() throws Exception {
        super.performPreTasks();

        MessageHandler m = new MessageHandler(plugin);
        plugin.registerReloadable(m::onReload);
        serviceManager.registerService(MessageHandler.class, m);
        game.getServiceManager().setProvider(plugin, NucleusPrivateMessagingService.class, m);
    }

    @Override public void onEnable() {
        super.onEnable();

        createSeenModule(SocialSpyCommand.class, (cs, user) -> {
            Optional<UserService> userServiceOptional = plugin.getUserDataManager().get(user);
            boolean socialSpy = userServiceOptional.isPresent() && userServiceOptional.get().isSocialSpy();
            MessageProvider mp = plugin.getMessageProvider();
            List<Text> lt = Lists.newArrayList(
                mp.getTextMessageWithFormat("seen.socialspy",
                    mp.getMessageWithFormat("standard.yesno." + Boolean.toString(socialSpy).toLowerCase())));

            getConfigAdapter().ifPresent(x -> lt.add(
                mp.getTextMessageWithFormat("seen.socialspylevel", String.valueOf(Util.getPositiveIntOptionFromSubject(user, MessageHandler.socialSpyOption).orElse(0)))
            ));

            return lt;
        });
    }
}
