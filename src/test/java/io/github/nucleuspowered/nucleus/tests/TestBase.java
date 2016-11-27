/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.tests;

import io.github.nucleuspowered.nucleus.ChatUtil;
import io.github.nucleuspowered.nucleus.NameUtil;
import io.github.nucleuspowered.nucleus.Nucleus;
import io.github.nucleuspowered.nucleus.dataservices.GeneralService;
import io.github.nucleuspowered.nucleus.dataservices.ItemDataService;
import io.github.nucleuspowered.nucleus.dataservices.loaders.UserDataManager;
import io.github.nucleuspowered.nucleus.dataservices.loaders.WorldDataManager;
import io.github.nucleuspowered.nucleus.internal.EconHelper;
import io.github.nucleuspowered.nucleus.internal.InternalServiceManager;
import io.github.nucleuspowered.nucleus.internal.MixinConfigProxy;
import io.github.nucleuspowered.nucleus.internal.PermissionRegistry;
import io.github.nucleuspowered.nucleus.internal.messages.MessageProvider;
import io.github.nucleuspowered.nucleus.internal.messages.ResourceMessageProvider;
import io.github.nucleuspowered.nucleus.internal.services.WarmupManager;
import io.github.nucleuspowered.nucleus.internal.teleport.NucleusTeleportHandler;
import org.junit.BeforeClass;
import org.slf4j.Logger;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.serializer.FormattingCodeTextSerializer;
import org.spongepowered.api.text.serializer.SafeTextSerializer;
import org.spongepowered.api.text.serializer.TextSerializers;
import uk.co.drnaylor.quickstart.modulecontainers.DiscoveryModuleContainer;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Optional;

public abstract class TestBase {

    private static void setFinalStatic(Field field) throws Exception {
        field.setAccessible(true);

        Field modifiersField = Field.class.getDeclaredField("modifiers");
        modifiersField.setAccessible(true);
        modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);
    }

    private static void setFinalStaticPlain(Field field) throws Exception {
        setFinalStatic(field);

        field.set(null, new SafeTextSerializer() {
            @Override
            public Text deserialize(String input) {
                return Text.of("key");
            }

            @Override
            public String serialize(Text text) {
                return "key";
            }
        });
    }

    private static void setFinalStaticFormatters(Field field) throws Exception {
        setFinalStatic(field);

        field.set(null, new FormattingCodeTextSerializer() {
            @Override
            public char getCharacter() {
                return '&';
            }

            @Override
            public String stripCodes(String text) {
                return "test";
            }

            @Override
            public String replaceCodes(String text, char to) {
                return "test";
            }

            @Override
            public Text deserialize(String input) {
                return Text.of("key");
            }

            @Override
            public String serialize(Text text) {
                return "key";
            }
        });
    }

    @BeforeClass
    public static void testSetup() throws Exception {
        try {
            Method m = Nucleus.class.getDeclaredMethod("setNucleus", Nucleus.class);
            m.setAccessible(true);
            m.invoke(null, new NucleusTest());
        } catch (IllegalStateException e) {
            // Nope
        }

        setFinalStaticPlain(TextSerializers.class.getField("PLAIN"));
        setFinalStaticFormatters(TextSerializers.class.getField("FORMATTING_CODE"));
        setFinalStaticFormatters(TextSerializers.class.getField("LEGACY_FORMATTING_CODE"));
    }

    private static class NucleusTest extends Nucleus {

        private MessageProvider mp = new ResourceMessageProvider(ResourceMessageProvider.messagesBundle);
        private PermissionRegistry permissionRegistry = new PermissionRegistry();

        @Override
        public void saveData() {

        }

        @Override
        public Logger getLogger() {
            return null;
        }

        @Override
        public UserDataManager getUserDataManager() {
            return null;
        }

        @Override
        public WorldDataManager getWorldDataManager() {
            return null;
        }

        @Override
        public void saveSystemConfig() throws IOException {

        }

        @Override
        public void reload() {

        }

        @Override
        public WarmupManager getWarmupManager() {
            return null;
        }

        @Override
        public EconHelper getEconHelper() {
            return null;
        }

        @Override
        public PermissionRegistry getPermissionRegistry() {
            return permissionRegistry;
        }

        @Override
        public DiscoveryModuleContainer getModuleContainer() {
            return null;
        }

        @Override
        public InternalServiceManager getInternalServiceManager() {
            return null;
        }

        @Override
        public GeneralService getGeneralService() {
            return null;
        }

        @Override
        public ItemDataService getItemDataService() {
            return null;
        }

        @Override
        public NameUtil getNameUtil() {
            return null;
        }

        @Override
        public ChatUtil getChatUtil() {
            return null;
        }

        @Override
        public MessageProvider getMessageProvider() {
            return mp;
        }

        @Override
        public MessageProvider getCommandMessageProvider() {
            return null;
        }

        @Override
        public Optional<MixinConfigProxy> getMixinConfigIfAvailable() {
            return Optional.empty();
        }

        @Override
        public NucleusTeleportHandler getTeleportHandler() {
            return null;
        }

        @Override public boolean isDebugMode() {
            return true;
        }
    }
}
