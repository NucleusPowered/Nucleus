/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.quickstart;

import com.google.common.base.Preconditions;
import io.github.nucleuspowered.nucleus.api.core.exception.ModulesLoadedException;
import io.github.nucleuspowered.nucleus.api.core.exception.NoModuleException;
import io.github.nucleuspowered.nucleus.api.core.exception.UnremovableModuleException;
import io.github.nucleuspowered.nucleus.services.INucleusServiceCollection;
import io.github.nucleuspowered.nucleus.services.interfaces.IMessageProviderService;
import org.slf4j.Logger;
import org.spongepowered.api.plugin.PluginContainer;
import uk.co.drnaylor.quickstart.ModuleHolder;
import uk.co.drnaylor.quickstart.enums.ConstructionPhase;
import uk.co.drnaylor.quickstart.exceptions.QuickStartModuleLoaderException;
import uk.co.drnaylor.quickstart.exceptions.UndisableableModuleException;

import com.google.inject.Inject;

public class ModuleRegistrationProxyService {

    private final INucleusServiceCollection serviceCollection;
    private final ModuleHolder<?, ?> moduleHolder;

    @Inject
    public ModuleRegistrationProxyService(INucleusServiceCollection serviceCollection, ModuleHolder<?, ?> holder) {
        this.serviceCollection = serviceCollection;
        this.moduleHolder = holder;
    }

    public boolean canDisableModules() {
        return this.moduleHolder.getCurrentPhase() == ConstructionPhase.DISCOVERED;
    }

    public void removeModule(String module, PluginContainer plugin) throws ModulesLoadedException, UnremovableModuleException, NoModuleException {
        if (!canDisableModules()) {
            throw new ModulesLoadedException();
        }

        // The plugin must actually be a plugin.
        Preconditions.checkNotNull(plugin);
        Logger logger = this.serviceCollection.logger();
        IMessageProviderService messageProviderService = this.serviceCollection.messageProvider();
        try {
            this.moduleHolder.disableModule(module);
            logger.info(messageProviderService.getMessageString("nucleus.module.disabled.modulerequest",
                    plugin.getName(), plugin.getId(), module));
        } catch (IllegalStateException e) {
            throw new ModulesLoadedException();
        } catch (UndisableableModuleException e) {
            logger.warn(messageProviderService.getMessageString("nucleus.module.disabled.forceload",
                    plugin.getName(),
                    plugin.getId(),
                    module));
            logger.warn(messageProviderService.getMessageString("nucleus.module.disabled.forceloadtwo", plugin.getName()));
            throw new UnremovableModuleException();
        } catch (uk.co.drnaylor.quickstart.exceptions.NoModuleException e) {
            throw new NoModuleException();
        } catch (QuickStartModuleLoaderException e) {
            e.printStackTrace();
        }
    }
}
