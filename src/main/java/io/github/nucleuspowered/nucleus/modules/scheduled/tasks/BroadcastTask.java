/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.scheduled.tasks;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import io.github.nucleuspowered.nucleus.ChatUtil;
import io.github.nucleuspowered.nucleus.internal.scheduled.ScheduledTask;
import io.github.nucleuspowered.nucleus.modules.admin.config.AdminConfigAdapter;
import io.github.nucleuspowered.nucleus.modules.admin.config.BroadcastConfig;
import io.github.nucleuspowered.nucleus.util.DelayedTimeValue;
import org.spongepowered.api.Game;
import org.spongepowered.api.command.source.ConsoleSource;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.channel.MessageChannel;

import java.util.List;

public class BroadcastTask extends ScheduledTask {

    private String messageToBroadcast;
    @Inject
    private Game game;
    @Inject
    private AdminConfigAdapter adminConfigAdapter;
    @Inject
    private ChatUtil chatUtil;

    public BroadcastTask(String name, String messageToBroadcast, DelayedTimeValue delayedTimeValue) {
        super(name, delayedTimeValue);
        this.messageToBroadcast = messageToBroadcast;
    }

    @Override
    protected void runScheduledTask(Task task) {
        BroadcastConfig bc = adminConfigAdapter.getNodeOrDefault().getBroadcastMessage();
        ConsoleSource console = game.getServer().getConsole();
        List<Text> messages = Lists.newArrayList();
        ChatUtil.StyleTuple cst = ChatUtil.EMPTY;

        String prefix = bc.getPrefix();
        if (!prefix.trim().isEmpty()) {
            messages.add(chatUtil.getMessageFromTemplate(prefix, console, true));
        }

        messages.add(Text.of(cst.colour, cst.style, chatUtil.
                addUrlsToAmpersandFormattedString(this.messageToBroadcast)));

        String suffix = bc.getSuffix();
        if (!suffix.trim().isEmpty()) {
            messages.add(Text.of(cst.colour, cst.style,
                    chatUtil.getMessageFromTemplate(suffix, console, true)));
        }

        MessageChannel.TO_ALL.send(Text.joinWith(Text.of(" "), messages));
    }
}
