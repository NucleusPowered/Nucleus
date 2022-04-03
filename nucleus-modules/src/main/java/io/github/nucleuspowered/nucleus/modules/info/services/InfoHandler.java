/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.info.services;

import io.github.nucleuspowered.nucleus.core.io.TextFileController;
import io.github.nucleuspowered.nucleus.core.scaffold.service.ServiceBase;
import io.github.nucleuspowered.nucleus.core.services.INucleusServiceCollection;
import io.github.nucleuspowered.nucleus.core.services.interfaces.IReloadableService;
import org.apache.logging.log4j.Logger;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.resource.Resource;
import org.spongepowered.api.resource.ResourcePath;
import org.spongepowered.api.resource.pack.Pack;
import org.spongepowered.api.resource.pack.PackType;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class InfoHandler implements IReloadableService.Reloadable, ServiceBase {

    private final Map<String, TextFileController> infoFiles = new HashMap<>();
    private final Pattern validFile = Pattern.compile("[a-zA-Z0-9_.\\-]+\\.txt", Pattern.CASE_INSENSITIVE);

    public Set<String> getInfoSections() {
        return Collections.unmodifiableSet(this.infoFiles.keySet());
    }

    /**
     * Gets the text associated with the specified key, if it exists.
     *
     * @param name The name of the section to retrieve the keys from.
     * @return An {@link Optional} potentially containing the {@link TextFileController}.
     *
     */
    public Optional<TextFileController> getSection(final String name) {
        final Optional<String> os = this.infoFiles.keySet().stream().filter(name::equalsIgnoreCase).findFirst();
        return os.map(this.infoFiles::get);

    }

    private void copyIfNotExists(final Logger logger, final Pack pack, final ResourcePath path, final Path target) {
        if (Files.notExists(target)) {
            try (final Resource resource = pack.contents().requireResource(PackType.server(), path)) {
                Files.copy(resource.inputStream(), target);
            } catch (final IOException e) {
                logger.error("Could not find resource {} when it should exist.", path.toString());
            }
        }
    }

    @Override
    public void onReload(final INucleusServiceCollection serviceCollection) {
        // Get the config directory, check to see if "info/" exists.
        final Path infoDir = serviceCollection.configDir().resolve("info");
        if (!Files.exists(infoDir)) {
            try {
                Files.createDirectories(infoDir);

                final Pack pack = Sponge.server().packRepository().pack(serviceCollection.pluginContainer());
                this.copyIfNotExists(serviceCollection.logger(), pack, ResourcePath.of(pack.id(), "info.txt"), infoDir.resolve("info.txt"));
                this.copyIfNotExists(serviceCollection.logger(), pack, ResourcePath.of(pack.id(), "colors.txt"), infoDir.resolve("colors.txt"));
                this.copyIfNotExists(serviceCollection.logger(), pack, ResourcePath.of(pack.id(), "links.txt"), infoDir.resolve("links.txt"));
            } catch (final IOException e) {
                e.printStackTrace();
                return;
            }
        } else if (!Files.isDirectory(infoDir)) {
            throw new IllegalStateException("The file " + infoDir.toAbsolutePath().toString() + " should be a directory.");
        }

        // Get all txt files.
        final List<Path> files;
        try (final Stream<Path> sp = Files.list(infoDir)) {
            files = sp.filter(Files::isRegularFile)
              .filter(x -> this.validFile.matcher(x.getFileName().toString()).matches()).collect(Collectors.toList());
        } catch (final Exception ex) {
            ex.printStackTrace();
            return;
        }

        // Collect them and put the resultant controllers into a temporary map.
        final Map<String, TextFileController> mst = new HashMap<>();
        files.forEach(x -> {
            try {
                String name = x.getFileName().toString();
                name = name.substring(0, name.length() - 4);
                if (mst.keySet().stream().anyMatch(name::equalsIgnoreCase)) {
                    serviceCollection.logger().warn(
                            serviceCollection.messageProvider().getMessageString("info.load.duplicate", x.getFileName().toString()));

                    // This is a function, so return is appropriate, not break.
                    return;
                }

                final TextFileController tfc = new TextFileController(serviceCollection.pluginContainer(),
                        serviceCollection.textTemplateFactory(), x, true);
                tfc.load();
                mst.put(name, tfc);
            } catch (final IOException e) {
                e.printStackTrace();
            }
        });

        // All good - replace it all!
        this.infoFiles.clear();
        this.infoFiles.putAll(mst);
    }
}
