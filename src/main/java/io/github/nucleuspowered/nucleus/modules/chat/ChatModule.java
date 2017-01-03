/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.chat;

import io.github.nucleuspowered.nucleus.internal.qsml.module.ConfigurableModule;
import io.github.nucleuspowered.nucleus.modules.chat.config.ChatConfigAdapter;
import io.github.nucleuspowered.nucleus.modules.chat.util.TemplateUtil;
import uk.co.drnaylor.quickstart.annotations.ModuleData;

@ModuleData(id = ChatModule.ID, name = "Chat")
public class ChatModule extends ConfigurableModule<ChatConfigAdapter> {

    public final static String ID = "chat";

    @Override
    public ChatConfigAdapter createAdapter() {
        return new ChatConfigAdapter();
    }

    @Override protected void performPreTasks() throws Exception {
        super.performPreTasks();

        try {
            ChatConfigAdapter cca = plugin.getInjector().getInstance(ChatConfigAdapter.class);
            TemplateUtil templateUtil = new TemplateUtil(plugin, cca);
            serviceManager.registerService(TemplateUtil.class, templateUtil);
        } catch (Exception ex) {
            plugin.getLogger().warn("Could not load the chat module for the reason below.");
            ex.printStackTrace();
            throw ex;
        }
    }
}
