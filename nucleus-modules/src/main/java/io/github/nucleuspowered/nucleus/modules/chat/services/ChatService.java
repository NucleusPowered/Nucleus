/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.chat.services;

import io.github.nucleuspowered.nucleus.modules.chat.config.ChatConfig;
import io.github.nucleuspowered.nucleus.modules.chat.config.ChatTemplateConfig;
import io.github.nucleuspowered.nucleus.scaffold.service.ServiceBase;
import io.github.nucleuspowered.nucleus.services.INucleusServiceCollection;
import io.github.nucleuspowered.nucleus.services.interfaces.IReloadableService;
import org.spongepowered.api.service.permission.Subject;

/**
 * Contains the logic for caching templates and the template selection logic.
 */
public class ChatService implements IReloadableService.Reloadable, ServiceBase {

    private ChatConfig config = new ChatConfig();

    public ChatTemplateConfig getTemplateNow(Subject subject) {
        if (!this.config.isUseGroupTemplates()) {
            return this.config.getDefaultTemplate();
        }

        return subject.getOption("nucleus.chat.group").map(x -> this.config.getGroupTemplates().get(x)).orElse(this.config.getDefaultTemplate());
    }

    @Override
    public void onReload(INucleusServiceCollection serviceCollection) {
        this.config = serviceCollection.moduleDataProvider().getModuleConfig(ChatConfig.class);
    }
}
