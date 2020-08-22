/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.message;

import com.google.common.collect.Lists;
import io.github.nucleuspowered.nucleus.api.text.NucleusTextTemplate;
import io.github.nucleuspowered.nucleus.services.interfaces.IChatMessageFormatterService;
import io.github.nucleuspowered.nucleus.services.interfaces.IPermissionService;
import io.github.nucleuspowered.nucleus.services.interfaces.ITextStyleService;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.event.message.MessageEvent;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.channel.MessageReceiver;

import java.util.Collection;
import java.util.List;

public class HelpOpMessageChannel implements IChatMessageFormatterService.Channel {

    private final IPermissionService permissionService;
    private final ITextStyleService textStyleService;
    @Nullable private final NucleusTextTemplate prefix;

    public HelpOpMessageChannel(@Nullable NucleusTextTemplate prefix, IPermissionService permissionService, ITextStyleService textStyleService) {
        this.permissionService = permissionService;
        this.textStyleService = textStyleService;
        this.prefix = prefix;
    }

    @Override
    public boolean willFormat() {
        return true;
    }

    @Override
    public void formatMessageEvent(CommandSource source, MessageEvent.MessageFormatter formatters) {
        if (this.prefix != null) {
            formatters.setHeader(Text.of(formatters.getHeader(), this.prefix.getForCommandSource(source)));
        }

        ITextStyleService.TextFormat format = this.textStyleService.getLastColourAndStyle(formatters.getHeader(), null);
        formatters.setBody(Text.builder().color(format.colour()).style(format.style()).append(formatters.getBody().format()).build());
    }

    @Override
    public Collection<MessageReceiver> receivers() {
        List<MessageReceiver> members = Lists.newArrayList(Sponge.getServer().getConsole());
        Sponge.getServer().getOnlinePlayers().stream()
                .filter(x -> this.permissionService.hasPermission(x, MessagePermissions.HELPOP_RECEIVE)).forEach(members::add);
        return members;
    }
}
