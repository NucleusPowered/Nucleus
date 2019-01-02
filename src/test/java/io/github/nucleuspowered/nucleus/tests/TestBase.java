/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.tests;

import io.github.nucleuspowered.nucleus.NameUtil;
import io.github.nucleuspowered.nucleus.Nucleus;
import io.github.nucleuspowered.nucleus.api.service.NucleusMessageTokenService;
import io.github.nucleuspowered.nucleus.config.CommandsConfig;
import io.github.nucleuspowered.nucleus.dataservices.ItemDataService;
import io.github.nucleuspowered.nucleus.dataservices.KitService;
import io.github.nucleuspowered.nucleus.dataservices.NameBanService;
import io.github.nucleuspowered.nucleus.dataservices.UserCacheService;
import io.github.nucleuspowered.nucleus.dataservices.loaders.UserDataManager;
import io.github.nucleuspowered.nucleus.dataservices.loaders.WorldDataManager;
import io.github.nucleuspowered.nucleus.dataservices.modular.ModularGeneralService;
import io.github.nucleuspowered.nucleus.internal.EconHelper;
import io.github.nucleuspowered.nucleus.internal.InternalServiceManager;
import io.github.nucleuspowered.nucleus.internal.PermissionRegistry;
import io.github.nucleuspowered.nucleus.internal.TextFileController;
import io.github.nucleuspowered.nucleus.internal.docgen.DocGenCache;
import io.github.nucleuspowered.nucleus.internal.interfaces.Reloadable;
import io.github.nucleuspowered.nucleus.internal.messages.MessageProvider;
import io.github.nucleuspowered.nucleus.internal.messages.ResourceMessageProvider;
import io.github.nucleuspowered.nucleus.internal.qsml.NucleusConfigAdapter;
import io.github.nucleuspowered.nucleus.internal.services.PermissionResolver;
import io.github.nucleuspowered.nucleus.internal.services.WarmupManager;
import io.github.nucleuspowered.nucleus.internal.teleport.NucleusTeleportHandler;
import io.github.nucleuspowered.nucleus.internal.text.TextParsingUtils;
import io.github.nucleuspowered.nucleus.modules.core.config.WarmupConfig;
import io.github.nucleuspowered.nucleus.storage.INucleusStorageManager;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.asset.Asset;
import org.spongepowered.api.event.CauseStackManager;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.cause.EventContext;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.serializer.FormattingCodeTextSerializer;
import org.spongepowered.api.text.serializer.SafeTextSerializer;
import org.spongepowered.api.text.serializer.TextSerializers;
import uk.co.drnaylor.quickstart.modulecontainers.DiscoveryModuleContainer;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@RunWith(PowerMockRunner.class)
@PrepareForTest(Sponge.class)
public abstract class TestBase {

    private static boolean complete = false;

    private static void setFinalStatic(Field field) throws Exception {
        field.setAccessible(true);

        Field modifiersField = Field.class.getDeclaredField("modifiers");
        modifiersField.setAccessible(true);
        modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);
    }

    private static void setFinalStaticPlain(Field field) throws Exception {
        setFinalStatic(field);
        SafeTextSerializer sts = Mockito.mock(SafeTextSerializer.class);
        Mockito.when(sts.serialize(Mockito.any())).thenReturn("key");
        Mockito.when(sts.deserialize(Mockito.any())).thenReturn(Text.of("key"));
        field.set(null, sts);
    }

    private static void setFinalStaticFormatters(Field field) throws Exception {
        setFinalStatic(field);
        FormattingCodeTextSerializer sts = Mockito.mock(FormattingCodeTextSerializer.class);
        Mockito.when(sts.serialize(Mockito.any())).thenReturn("key");
        Mockito.when(sts.deserialize(Mockito.any())).thenReturn(Text.of("key"));
        Mockito.when(sts.stripCodes(Mockito.anyString())).thenReturn("test");
        Mockito.when(sts.replaceCodes(Mockito.anyString(), Mockito.anyChar())).thenReturn("test");
        field.set(null, sts);
    }

    @BeforeClass
    public static void testSetup() throws Exception {
        if (complete) {
            return;
        }

        complete = true;
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

    public static void setupSpongeMock() {
        Cause mockCause = Cause.of(EventContext.empty(), "test");
        CauseStackManager csm = Mockito.mock(CauseStackManager.class);
        Mockito.when(csm.getCurrentCause()).thenReturn(mockCause);
        PowerMockito.mockStatic(Sponge.class);
        PowerMockito.when(Sponge.getCauseStackManager()).thenReturn(csm);
    }

    private static class NucleusTest extends Nucleus {

        private final MessageProvider mp = new ResourceMessageProvider(ResourceMessageProvider.messagesBundle);
        private final PermissionRegistry permissionRegistry = new PermissionRegistry();
        private final InternalServiceManager serviceManager = new InternalServiceManager();

        @Override
        public void addX(List<Text> messages, int spacing) {
            // NOOP
        }

        @Override
        public void saveData() {

        }

        @Override
        public Logger getLogger() {
            return LoggerFactory.getLogger("test");
        }

        @Override public Path getConfigDirPath() {
            return null;
        }

        @Override public Path getDataPath() {
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

        @Override public UserCacheService getUserCacheService() {
            return null;
        }

        @Override
        public void saveSystemConfig() throws IOException {

        }

        @Override
        public boolean reload() {
            return true;
        }

        @Override public boolean reloadMessages() {
            return true;
        }

        @Override
        public WarmupManager getWarmupManager() {
            return null;
        }

        @Override public WarmupConfig getWarmupConfig() {
            return null;
        }

        @Override
        public EconHelper getEconHelper() {
            return null;
        }

        @Override
        public PermissionRegistry getPermissionRegistry() {
            return this.permissionRegistry;
        }

        @Override
        public DiscoveryModuleContainer getModuleContainer() {
            return null;
        }

        @Override public boolean isModuleLoaded(String moduleId) {
            return true;
        }

        @Override public <T extends NucleusConfigAdapter<?>> Optional<T> getConfigAdapter(String id, Class<T> configAdapterClass) {
            return Optional.empty();
        }

        @Override
        public InternalServiceManager getInternalServiceManager() {
            return this.serviceManager;
        }

        @Override public Optional<Instant> getGameStartedTime() {
            return Optional.empty();
        }

        @Override
        public ModularGeneralService getGeneralService() {
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

        public TextParsingUtils getTextParsingUtils() {
            return null;
        }

        @Override
        public MessageProvider getMessageProvider() {
            return this.mp;
        }

        @Override
        public MessageProvider getCommandMessageProvider() {
            return null;
        }

        @Override public int traceUserCreations() {
            return 0;
        }

        @Override public Optional<TextFileController> getTextFileController(String getController) {
            return Optional.empty();
        }

        @Override public void addTextFileController(String id, Asset asset, Path file) throws IOException {

        }

        @Override public void registerReloadable(Reloadable reloadable) {

        }

        @Override public Optional<DocGenCache> getDocGenCache() {
            return Optional.empty();
        }

        @Override
        public NucleusTeleportHandler getTeleportHandler() {
            return null;
        }

        @Override public NucleusMessageTokenService getMessageTokenService() {
            return null;
        }

        @Override public boolean isDebugMode() {
            return true;
        }

        @Override public void printStackTraceIfDebugMode(Throwable throwable) {

        }

        @Override public KitService getKitService() {
            return null;
        }

        @Override public NameBanService getNameBanService() {
            return null;
        }

        @Override public CommandsConfig getCommandsConfig() {
            return null;
        }

        @Override public PluginContainer getPluginContainer() {
            return null;
        }

        @Override public boolean isSessionDebug() {
            return false;
        }

        @Override public void setSessionDebug(boolean debug) {
            // NOOP
        }

        @Override public PermissionResolver getPermissionResolver() {
            return PermissionResolver.SIMPLE;
        }

        @Override public boolean isServer() {
            return true;
        }

        @Override public void addStartupMessage(Text message) {
            // NOOP
        }

        @Override public boolean isPrintingSavesAndLoads() {
            return false;
        }

        @Override public INucleusStorageManager getStorageManager() {
            return null;
        }
    }
}
