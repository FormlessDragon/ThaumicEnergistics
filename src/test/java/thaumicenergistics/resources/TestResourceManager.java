package thaumicenergistics.resources;

import net.minecraft.client.resources.IResource;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.data.IMetadataSection;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nullable;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class TestResourceManager implements IResourceManager {

    private final Map<String, List<Path>> namespaceRoots;
    private final Set<String> classpathFallbackNamespaces;
    private final ClassLoader classLoader;

    private TestResourceManager(Map<String, List<Path>> namespaceRoots, Set<String> classpathFallbackNamespaces,
                                ClassLoader classLoader) {
        this.namespaceRoots = namespaceRoots;
        this.classpathFallbackNamespaces = classpathFallbackNamespaces;
        this.classLoader = classLoader;
    }

    static Builder builder() {
        return new Builder();
    }

    @Override
    public Set<String> getResourceDomains() {
        Set<String> domains = new HashSet<>(this.namespaceRoots.keySet());
        domains.addAll(this.classpathFallbackNamespaces);
        return Collections.unmodifiableSet(domains);
    }

    @Override
    public IResource getResource(ResourceLocation location) throws IOException {
        List<Path> roots = this.namespaceRoots.get(location.getNamespace());
        if (roots != null) {
            for (Path root : roots) {
                Path resourcePath = resolveResourcePath(root, location);
                if (Files.isRegularFile(resourcePath)) {
                    return new FileResource(location, resourcePath, root);
                }
            }
        }

        if (this.classpathFallbackNamespaces.contains(location.getNamespace())) {
            String resourceName = classpathResourceName(location);
            URL resource = this.classLoader.getResource(resourceName);
            if (resource != null) {
                return new ClasspathResource(location, resourceName, resource);
            }
        }

        throw new FileNotFoundException(location.toString());
    }

    @Override
    public List<IResource> getAllResources(ResourceLocation location) throws IOException {
        return List.of(this.getResource(location));
    }

    private static Path resolveResourcePath(Path root, ResourceLocation location) throws IOException {
        Path namespaceRoot = root.resolve("assets").resolve(location.getNamespace()).normalize();
        Path resourcePath = namespaceRoot.resolve(location.getPath()).normalize();
        if (!resourcePath.startsWith(namespaceRoot)) {
            throw new IOException("Resource path escapes namespace root: " + location);
        }
        return resourcePath;
    }

    static final class Builder {

        private final Map<String, List<Path>> namespaceRoots = new HashMap<>();
        private final Set<String> classpathFallbackNamespaces = new HashSet<>();

        Builder addNamespaceRoot(String namespace, Path root) {
            Path normalizedRoot = root.toAbsolutePath().normalize();
            if (!Files.isDirectory(normalizedRoot)) {
                throw new IllegalStateException("Missing test resource root: " + normalizedRoot);
            }
            this.namespaceRoots.computeIfAbsent(namespace, ignored -> new ArrayList<>()).add(normalizedRoot);
            return this;
        }

        Builder addClasspathFallback(String namespace) {
            this.classpathFallbackNamespaces.add(namespace);
            return this;
        }

        TestResourceManager build() {
            if (this.namespaceRoots.isEmpty() && this.classpathFallbackNamespaces.isEmpty()) {
                throw new IllegalStateException("No test resources registered");
            }
            return new TestResourceManager(copyNamespaceRoots(), Set.copyOf(this.classpathFallbackNamespaces),
                    TestResourceManager.class.getClassLoader());
        }

        private Map<String, List<Path>> copyNamespaceRoots() {
            Map<String, List<Path>> copy = new HashMap<>();
            for (Map.Entry<String, List<Path>> entry : this.namespaceRoots.entrySet()) {
                copy.put(entry.getKey(), List.copyOf(entry.getValue()));
            }
            return Map.copyOf(copy);
        }
    }

    private static String classpathResourceName(ResourceLocation location) {
        return "assets/" + location.getNamespace() + "/" + location.getPath();
    }

    private static final class FileResource implements IResource {

        private final ResourceLocation location;
        private final Path path;
        private final Path root;

        private FileResource(ResourceLocation location, Path path, Path root) {
            this.location = location;
            this.path = path;
            this.root = root;
        }

        @Override
        public ResourceLocation getResourceLocation() {
            return this.location;
        }

        @Override
        public InputStream getInputStream() {
            try {
                return Files.newInputStream(this.path);
            } catch (IOException e) {
                throw new UncheckedIOException("Failed to open test resource: " + this.path, e);
            }
        }

        @Override
        public boolean hasMetadata() {
            return false;
        }

        @Nullable
        @Override
        public <T extends IMetadataSection> T getMetadata(String sectionName) {
            return null;
        }

        @Override
        public String getResourcePackName() {
            return this.root.toString();
        }

        @Override
        public void close() {
        }
    }

    private static final class ClasspathResource implements IResource {

        private final ResourceLocation location;
        private final String resourceName;
        private final URL resource;

        private ClasspathResource(ResourceLocation location, String resourceName, URL resource) {
            this.location = location;
            this.resourceName = resourceName;
            this.resource = resource;
        }

        @Override
        public ResourceLocation getResourceLocation() {
            return this.location;
        }

        @Override
        public InputStream getInputStream() {
            try {
                return this.resource.openStream();
            } catch (IOException e) {
                throw new UncheckedIOException("Failed to open classpath test resource: " + this.resourceName, e);
            }
        }

        @Override
        public boolean hasMetadata() {
            return false;
        }

        @Nullable
        @Override
        public <T extends IMetadataSection> T getMetadata(String sectionName) {
            return null;
        }

        @Override
        public String getResourcePackName() {
            return "classpath:" + this.resourceName;
        }

        @Override
        public void close() {
        }
    }
}
