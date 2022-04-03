/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.core.io;

import io.github.nucleuspowered.nucleus.core.Util;
import io.github.nucleuspowered.nucleus.api.text.NucleusTextTemplate;
import io.github.nucleuspowered.nucleus.core.services.impl.texttemplatefactory.NucleusTextTemplateImpl;
import io.github.nucleuspowered.nucleus.core.services.interfaces.INucleusTextTemplateFactory;
import io.github.nucleuspowered.nucleus.core.util.AdventureUtils;
import io.vavr.control.Option;
import io.vavr.control.Try;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.resource.Resource;
import org.spongepowered.api.resource.ResourcePath;
import org.spongepowered.api.resource.pack.Pack;
import org.spongepowered.api.resource.pack.PackType;
import org.spongepowered.api.service.pagination.PaginationList;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.MalformedInputException;
import java.nio.charset.StandardCharsets;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.plugin.PluginContainer;

/**
 * Handles loading and reading text files.
 */
public final class TextFileController {

    private static final Component padding = Component.text("-", NamedTextColor.GOLD);

    private static final List<Charset> characterSetsToTest = Arrays.asList(
            StandardCharsets.UTF_8,
            StandardCharsets.ISO_8859_1,
            StandardCharsets.US_ASCII,
            StandardCharsets.UTF_16
    );

    /**
     * The internal {@link ResourcePath} that represents the default file.
     */
    @Nullable private final Supplier<ResourcePath> resourcePath;

    /**
     * Holds the file location.
     */
    private final Path fileLocation;

    /**
     * Holds the file information.
     */
    private final List<String> fileContents = new ArrayList<>();

    /**
     * Holds the {@link NucleusTextTemplateImpl} information.
     */
    private final List<NucleusTextTemplateImpl> textTemplates = new ArrayList<>();
    private final boolean getTitle;
    private final INucleusTextTemplateFactory textTemplateFactory;
    private final PluginContainer pluginContainer;

    private long fileTimeStamp = 0;
    @Nullable private NucleusTextTemplate title;

    public TextFileController(final PluginContainer pluginContainer,
            final INucleusTextTemplateFactory textTemplateFactory, final Path fileLocation, final boolean getTitle) {
        this(pluginContainer, textTemplateFactory, null, fileLocation, getTitle);
    }

    public TextFileController(
            final PluginContainer pluginContainer,
            final INucleusTextTemplateFactory textTemplateFactory, @Nullable final String path, final Path fileLocation) {
        this(pluginContainer, textTemplateFactory, path == null ? null : () -> {
            final Pack pluginPack = Sponge.server().packRepository().pack(pluginContainer);
            return ResourcePath.of(pluginPack.id(), path);
        }, fileLocation, false);
    }

    private TextFileController(
            final PluginContainer pluginContainer,
            final INucleusTextTemplateFactory textTemplateFactory, @Nullable final Supplier<ResourcePath> resourcePath, final Path fileLocation,
            final boolean getTitle) {
        this.textTemplateFactory = textTemplateFactory;
        this.resourcePath = resourcePath;
        this.fileLocation = fileLocation;
        this.getTitle = getTitle;
        this.pluginContainer = pluginContainer;
    }

    /**
     * Loads the file and refreshes the contents of the file in memory.
     *
     * @throws IOException Thrown if there is an issue getting the file.
     */
    public void load() throws IOException {
        if (this.resourcePath != null && !Files.exists(this.fileLocation)) {
            final Pack pluginPack = Sponge.server().packRepository().pack(this.pluginContainer);
            // Create the file
            final Optional<Resource> o = pluginPack.contents().resource(PackType.server(), this.resourcePath.get());
            if (o.isPresent()) {
                try (final Resource resource = o.get()) {
                    Files.copy(resource.inputStream(), this.fileLocation);
                }
            }
        }

        final List<String> fileContents = new ArrayList<>();

        // Load the file into the list.
        MalformedInputException exception = null;
        for (final Charset charset : characterSetsToTest) {
            try {
                fileContents.addAll(Files.readAllLines(this.fileLocation, charset));
                exception = null;
                break;
            } catch (final MalformedInputException ex) {
                exception = ex;
            }
        }

        // Rethrow exception if it doesn't work.
        if (exception != null) {
            throw exception;
        }

        this.fileTimeStamp = Files.getLastModifiedTime(this.fileLocation).toMillis();
        this.fileContents.clear();
        this.fileContents.addAll(fileContents);
        this.textTemplates.clear();
    }

    public Optional<Component> getTitle(final Audience source) {
        if (this.getTitle && this.textTemplates.isEmpty() && !this.fileContents.isEmpty()) {
            // Initialisation!
            this.getFileContentsAsText();
        }

        if (this.title != null) {
            return Optional.of(this.title.getForObject(source));
        }

        return Optional.empty();
    }

    public List<Component> getTextFromNucleusTextTemplates(final Audience source) {
        return this.getFileContentsAsText().stream().map(x -> x.getForObject(source)).collect(Collectors.toList());
    }

    public void sendToAudience(final Audience src, final Component title) {

        final PaginationList.Builder pb = Util.getPaginationBuilder(src).contents(this.getTextFromNucleusTextTemplates(src));

        if (title != null && !AdventureUtils.isEmpty(title)) {
            pb.title(title).padding(padding);
        } else {
            pb.padding(Component.space());
        }

        pb.sendTo(src);
    }

    /**
     * Gets the contents of the file.
     *
     * @return An immutable {@link Collection} that contains the file contents.
     */
    private Collection<NucleusTextTemplateImpl> getFileContentsAsText() {
        this.checkFileStamp();
        if (this.textTemplates.isEmpty()) {
            final List<String> contents = new ArrayList<>(this.fileContents);
            if (this.getTitle) {
                this.title = this.getTitleFromStrings(contents);

                if (this.title != null) {
                    contents.remove(0);

                    final Iterator<String> i = contents.iterator();
                    while (i.hasNext()) {
                        final String n = i.next();
                        if (n.isEmpty() || n.matches("^\\s+$")) {
                            i.remove();
                        } else {
                            break;
                        }
                    }
                }
            }

            contents.forEach(x -> this.textTemplates.add(this.textTemplateFactory.createFromAmpersandString(x)));
        }

        return Collections.unmodifiableCollection(this.textTemplates);
    }

    @Nullable private NucleusTextTemplate getTitleFromStrings(final List<String> info) {
        if (!info.isEmpty()) {
            String sec1 = info.get(0);
            if (sec1.startsWith("#")) {
                // Get rid of the # and spaces, then limit to 50 characters.
                sec1 = sec1.replaceFirst("#\\s*", "");
                if (sec1.length() > 50) {
                    sec1 = sec1.substring(0, 50);
                }

                return this.textTemplateFactory.createFromAmpersandString(sec1);
            }
        }

        return null;
    }

    private void checkFileStamp() {
        try {
            if (this.fileContents.isEmpty() || Files.getLastModifiedTime(this.fileLocation).toMillis() > this.fileTimeStamp) {
                this.load();
            }
        } catch (final IOException e) {
            // ignored
        }
    }
}
