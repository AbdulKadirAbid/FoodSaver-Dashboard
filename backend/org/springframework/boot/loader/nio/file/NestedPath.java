// 
// Decompiled by Procyon v0.6.0
// 

package org.springframework.boot.loader.nio.file;

import java.nio.file.FileSystem;
import java.nio.file.ProviderMismatchException;
import org.springframework.boot.loader.zip.ZipContent;
import java.nio.file.NoSuchFileException;
import java.nio.file.Files;
import java.util.Objects;
import java.nio.file.WatchKey;
import java.nio.file.WatchEvent;
import java.nio.file.WatchService;
import java.io.IOException;
import java.nio.file.LinkOption;
import java.net.URISyntaxException;
import java.io.IOError;
import java.net.URI;
import java.nio.file.Path;

final class NestedPath implements Path
{
    private final NestedFileSystem fileSystem;
    private final String nestedEntryName;
    private volatile Boolean entryExists;
    
    NestedPath(final NestedFileSystem fileSystem, final String nestedEntryName) {
        if (fileSystem == null) {
            throw new IllegalArgumentException("'filesSystem' must not be null");
        }
        this.fileSystem = fileSystem;
        this.nestedEntryName = ((nestedEntryName != null && !nestedEntryName.isBlank()) ? nestedEntryName : null);
    }
    
    Path getJarPath() {
        return this.fileSystem.getJarPath();
    }
    
    String getNestedEntryName() {
        return this.nestedEntryName;
    }
    
    @Override
    public NestedFileSystem getFileSystem() {
        return this.fileSystem;
    }
    
    @Override
    public boolean isAbsolute() {
        return true;
    }
    
    @Override
    public Path getRoot() {
        return null;
    }
    
    @Override
    public Path getFileName() {
        return this;
    }
    
    @Override
    public Path getParent() {
        return null;
    }
    
    @Override
    public int getNameCount() {
        return 1;
    }
    
    @Override
    public Path getName(final int index) {
        if (index != 0) {
            throw new IllegalArgumentException("Nested paths only have a single element");
        }
        return this;
    }
    
    @Override
    public Path subpath(final int beginIndex, final int endIndex) {
        if (beginIndex != 0 || endIndex != 1) {
            throw new IllegalArgumentException("Nested paths only have a single element");
        }
        return this;
    }
    
    @Override
    public boolean startsWith(final Path other) {
        return this.equals(other);
    }
    
    @Override
    public boolean endsWith(final Path other) {
        return this.equals(other);
    }
    
    @Override
    public Path normalize() {
        return this;
    }
    
    @Override
    public Path resolve(final Path other) {
        throw new UnsupportedOperationException("Unable to resolve nested path");
    }
    
    @Override
    public Path relativize(final Path other) {
        throw new UnsupportedOperationException("Unable to relativize nested path");
    }
    
    @Override
    public URI toUri() {
        try {
            String uri = "nested:" + this.fileSystem.getJarPath().toUri().getRawPath();
            if (this.nestedEntryName != null) {
                uri = uri + "/!" + UriPathEncoder.encode(this.nestedEntryName);
            }
            return new URI(uri);
        }
        catch (final URISyntaxException ex) {
            throw new IOError(ex);
        }
    }
    
    @Override
    public Path toAbsolutePath() {
        return this;
    }
    
    @Override
    public Path toRealPath(final LinkOption... options) throws IOException {
        return this;
    }
    
    @Override
    public WatchKey register(final WatchService watcher, final WatchEvent.Kind<?>[] events, final WatchEvent.Modifier... modifiers) throws IOException {
        throw new UnsupportedOperationException("Nested paths cannot be watched");
    }
    
    @Override
    public int compareTo(final Path other) {
        final NestedPath otherNestedPath = cast(other);
        return this.nestedEntryName.compareTo(otherNestedPath.nestedEntryName);
    }
    
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || this.getClass() != obj.getClass()) {
            return false;
        }
        final NestedPath other = (NestedPath)obj;
        return Objects.equals(this.fileSystem, other.fileSystem) && Objects.equals(this.nestedEntryName, other.nestedEntryName);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(this.fileSystem, this.nestedEntryName);
    }
    
    @Override
    public String toString() {
        String string = this.fileSystem.getJarPath().toString();
        if (this.nestedEntryName != null) {
            string = string + this.fileSystem.getSeparator() + this.nestedEntryName;
        }
        return string;
    }
    
    void assertExists() throws NoSuchFileException {
        if (!Files.isRegularFile(this.getJarPath(), new LinkOption[0])) {
            throw new NoSuchFileException(this.toString());
        }
        Boolean entryExists = this.entryExists;
        if (entryExists == null) {
            try (final ZipContent content = ZipContent.open(this.getJarPath(), this.nestedEntryName)) {
                entryExists = true;
            }
            catch (final IOException ex) {
                entryExists = false;
            }
            this.entryExists = entryExists;
        }
        if (!entryExists) {
            throw new NoSuchFileException(this.toString());
        }
    }
    
    static NestedPath cast(final Path path) {
        if (path instanceof final NestedPath nestedPath) {
            return nestedPath;
        }
        throw new ProviderMismatchException();
    }
}
