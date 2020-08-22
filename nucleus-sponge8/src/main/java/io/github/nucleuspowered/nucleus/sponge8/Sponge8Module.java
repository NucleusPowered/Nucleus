package io.github.nucleuspowered.nucleus.sponge8;

import com.google.inject.AbstractModule;
import io.github.nucleuspowered.electrolysis.IPlatform;
import io.github.nucleuspowered.nucleus.sponge8.electrolysis.SpongePlatform;

public class Sponge8Module extends AbstractModule {

    @Override
    protected void configure() {
        this.bind(IPlatform.class).toInstance(new SpongePlatform());
    }

}
