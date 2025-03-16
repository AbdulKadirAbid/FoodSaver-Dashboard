// 
// Decompiled by Procyon v0.6.0
// 

package org.springframework.boot.loader.nio.file;

import java.nio.file.ClosedFileSystemException;
import java.nio.file.WatchService;
import java.nio.file.attribute.UserPrincipalLookupService;
import java.nio.file.PathMatcher;
import java.nio.file.FileStore;
import java.io.IOException;
import java.util.stream.Stream;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.Objects;
import java.nio.file.spi.FileSystemProvider;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.FileSystems;
import java.util.Collections;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.nio.file.Path;
import java.util.Set;
import java.nio.file.FileSystem;

class NestedFileSystem extends FileSystem
{
    private static final Set<String> SUPPORTED_FILE_ATTRIBUTE_VIEWS;
    private static final String FILE_SYSTEMS_CLASS_NAME;
    private static final Object EXISTING_FILE_SYSTEM;
    private final NestedFileSystemProvider provider;
    private final Path jarPath;
    private volatile boolean closed;
    private final Map<String, Object> zipFileSystems;
    
    NestedFileSystem(final NestedFileSystemProvider provider, final Path jarPath) {
        this.zipFileSystems = new HashMap<String, Object>();
        if (provider == null || jarPath == null) {
            throw new IllegalArgumentException("Provider and JarPath must not be null");
        }
        this.provider = provider;
        this.jarPath = jarPath;
    }
    
    void installZipFileSystemIfNecessary(final String nestedEntryName) {
        try {
            final boolean seen;
            synchronized (this.zipFileSystems) {
                seen = (this.zipFileSystems.putIfAbsent(nestedEntryName, NestedFileSystem.EXISTING_FILE_SYSTEM) != null);
            }
            if (!seen) {
                final URI uri = new URI("jar:nested:" + this.jarPath.toUri().getPath() + "/!" + nestedEntryName);
                if (!this.hasFileSystem(uri)) {
                    final FileSystem zipFileSystem = FileSystems.newFileSystem(uri, Collections.emptyMap());
                    synchronized (this.zipFileSystems) {
                        this.zipFileSystems.put(nestedEntryName, zipFileSystem);
                    }
                }
            }
        }
        catch (final Exception ex) {}
    }
    
    private boolean hasFileSystem(final URI uri) {
        try {
            FileSystems.getFileSystem(uri);
            return true;
        }
        catch (final FileSystemNotFoundException ex) {
            return this.isCreatingNewFileSystem();
        }
    }
    
    private boolean isCreatingNewFileSystem() {
        final StackTraceElement[] stackTrace;
        final StackTraceElement[] stack = stackTrace = Thread.currentThread().getStackTrace();
        for (final StackTraceElement element : stackTrace) {
            if (NestedFileSystem.FILE_SYSTEMS_CLASS_NAME.equals(element.getClassName())) {
                return "newFileSystem".equals(element.getMethodName());
            }
        }
        return false;
    }
    
    @Override
    public FileSystemProvider provider() {
        return this.provider;
    }
    
    Path getJarPath() {
        return this.jarPath;
    }
    
    @Override
    public void close() throws IOException {
        if (this.closed) {
            return;
        }
        this.closed = true;
        synchronized (this.zipFileSystems) {
            final Stream<Object> stream = this.zipFileSystems.values().stream();
            final Class<FileSystem> obj = FileSystem.class;
            Objects.requireNonNull(obj);
            final Stream<Object> filter = stream.filter(obj::isInstance);
            final Class<FileSystem> obj2 = FileSystem.class;
            Objects.requireNonNull(obj2);
            filter.map((Function<? super Object, ?>)obj2::cast).forEach((Consumer<? super Object>)this::closeZipFileSystem);
        }
        this.provider.removeFileSystem(this);
    }
    
    private void closeZipFileSystem(final FileSystem zipFileSystem) {
        try {
            zipFileSystem.close();
        }
        catch (final Exception ex) {}
    }
    
    @Override
    public boolean isOpen() {
        return !this.closed;
    }
    
    @Override
    public boolean isReadOnly() {
        return true;
    }
    
    @Override
    public String getSeparator() {
        return "/!";
    }
    
    @Override
    public Iterable<Path> getRootDirectories() {
        this.assertNotClosed();
        return (Iterable<Path>)Collections.emptySet();
    }
    
    @Override
    public Iterable<FileStore> getFileStores() {
        this.assertNotClosed();
        return (Iterable<FileStore>)Collections.emptySet();
    }
    
    @Override
    public Set<String> supportedFileAttributeViews() {
        this.assertNotClosed();
        return NestedFileSystem.SUPPORTED_FILE_ATTRIBUTE_VIEWS;
    }
    
    @Override
    public Path getPath(final String first, final String... more) {
        this.assertNotClosed();
        if (more.length != 0) {
            throw new IllegalArgumentException("Nested paths must contain a single element");
        }
        return new NestedPath(this, first);
    }
    
    @Override
    public PathMatcher getPathMatcher(final String syntaxAndPattern) {
        throw new UnsupportedOperationException("Nested paths do not support path matchers");
    }
    
    @Override
    public UserPrincipalLookupService getUserPrincipalLookupService() {
        throw new UnsupportedOperationException("Nested paths do not have a user principal lookup service");
    }
    
    @Override
    public WatchService newWatchService() throws IOException {
        throw new UnsupportedOperationException("Nested paths do not support the WatchService");
    }
    
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || this.getClass() != obj.getClass()) {
            return false;
        }
        final NestedFileSystem other = (NestedFileSystem)obj;
        return this.jarPath.equals(other.jarPath);
    }
    
    @Override
    public int hashCode() {
        return this.jarPath.hashCode();
    }
    
    @Override
    public String toString() {
        return this.jarPath.toAbsolutePath().toString();
    }
    
    private void assertNotClosed() {
        if (this.closed) {
            throw new ClosedFileSystemException();
        }
    }
    
    static {
        SUPPORTED_FILE_ATTRIBUTE_VIEWS = Set.of("basic");
        FILE_SYSTEMS_CLASS_NAME = FileSystems.class.getName();
        EXISTING_FILE_SYSTEM = new Object();
    }
}
