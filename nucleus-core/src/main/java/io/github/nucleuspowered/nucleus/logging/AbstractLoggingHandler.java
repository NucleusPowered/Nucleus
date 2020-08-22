/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.logging;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import io.github.nucleuspowered.nucleus.services.interfaces.IMessageProviderService;
import io.github.nucleuspowered.nucleus.services.interfaces.IReloadableService;
import org.slf4j.Logger;
import org.spongepowered.api.GameState;
import org.spongepowered.api.Sponge;

import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.List;
import java.util.Locale;

import com.google.inject.Inject;

public abstract class AbstractLoggingHandler implements IReloadableService.Reloadable {

    private static final DateTimeFormatter formatter = DateTimeFormatter
            .ofLocalizedDateTime(FormatStyle.SHORT)
            .withLocale(Locale.getDefault())
            .withZone(ZoneId.systemDefault());
    private final IMessageProviderService messageProviderService;
    private final Logger slogger;
    protected DateRotatableFileLogger logger;
    private final List<String> queueEntry = Lists.newArrayList();
    private final String directoryName;
    private final String filePrefix;
    private final Object locking = new Object();

    @Inject
    public AbstractLoggingHandler(String directoryName,
            String filePrefix,
            IMessageProviderService messageProviderService,
            Logger logger) {
        this.directoryName = directoryName;
        this.filePrefix = filePrefix;
        this.messageProviderService = messageProviderService;
        this.slogger = logger;
    }

    public void queueEntry(String s) {
        if (this.logger != null) {
            synchronized (this.locking) {
                this.queueEntry.add(s);
            }
        }
    }

    public void onServerShutdown() throws IOException {
        Preconditions.checkState(Sponge.getGame().getState().equals(GameState.SERVER_STOPPED));
        onShutdown();
    }

    protected void onShutdown() throws IOException {
        if (this.logger != null) {
            this.logger.close();
            this.logger = null;
        }
    }

    protected abstract boolean enabledLog();

    public void onTick() {
        if (this.queueEntry.isEmpty()) {
            return;
        }

        List<String> l;
        synchronized (this.locking) {
            l = Lists.newArrayList(this.queueEntry);
            this.queueEntry.clear();
        }

        if (this.logger == null) {
            if (enabledLog()) {
                try {
                    createLogger();
                } catch (IOException e) {
                    this.slogger.warn(this.messageProviderService.getMessageString("commandlog.couldnotwrite"));
                    e.printStackTrace();
                    return;
                }
            } else {
                return;
            }
        }

        try {
            writeEntry(l);
        } catch (IOException e) {
            this.slogger.warn(this.messageProviderService.getMessageString("commandlog.couldnotwrite"));
            e.printStackTrace();
        }
    }

    protected void createLogger() throws IOException {
        this.logger = new DateRotatableFileLogger(this.directoryName, this.filePrefix, s -> "[" +
            formatter.format(Instant.now().atZone(ZoneOffset.systemDefault())) +
            "] " + s);
    }

    private void writeEntry(Iterable<String> entry) throws IOException {
        if (this.logger != null) {
            this.logger.logEntry(entry);
        }
    }
}
