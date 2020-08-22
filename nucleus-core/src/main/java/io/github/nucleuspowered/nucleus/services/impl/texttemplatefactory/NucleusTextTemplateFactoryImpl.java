/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.services.impl.texttemplatefactory;

import io.github.nucleuspowered.nucleus.services.INucleusServiceCollection;
import io.github.nucleuspowered.nucleus.services.interfaces.INucleusTextTemplateFactory;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import org.spongepowered.api.text.Text;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class NucleusTextTemplateFactoryImpl implements INucleusTextTemplateFactory {

    private final INucleusServiceCollection serviceCollection;
    private final NucleusTextTemplateImpl.Empty emptyInstance;

    @Inject
    public NucleusTextTemplateFactoryImpl(INucleusServiceCollection serviceCollection) {
        this.serviceCollection = serviceCollection;
        this.emptyInstance = new NucleusTextTemplateImpl.Empty(serviceCollection);
    }

    public NucleusTextTemplateImpl.Empty emptyTextTemplate() {
        return this.emptyInstance;
    }

    @Override
    public NucleusTextTemplateImpl createFromString(String string) {
        return create(string);
    }

    @Override
    public NucleusTextTemplateImpl createFromAmpersandString(String string) {
        return new NucleusTextTemplateImpl.Ampersand(string, this.serviceCollection);
    }

    @Override public NucleusTextTemplateImpl createFromAmpersandString(String string, Text prefix, Text suffix) {
        return new NucleusTextTemplateImpl.Ampersand(string, prefix, suffix, this.serviceCollection);
    }

    public NucleusTextTemplateImpl create(String string) {
        if (string.isEmpty()) {
            return this.emptyInstance;
        }

        try {
            return new NucleusTextTemplateImpl.Json(string, this.serviceCollection);
        } catch (NullPointerException e) {
            return createFromAmpersand(string);
        } catch (RuntimeException e) {
            if (e.getCause() != null && e.getCause() instanceof ObjectMappingException) {
                return createFromAmpersand(string);
            } else {
                throw e;
            }
        }
    }

    private NucleusTextTemplateImpl createFromAmpersand(String string) {
        return new NucleusTextTemplateImpl.Ampersand(string, this.serviceCollection);
    }

}
