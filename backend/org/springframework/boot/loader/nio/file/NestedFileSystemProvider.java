// 
// Decompiled by Procyon v0.6.0
// 

package org.springframework.boot.loader.nio.file;

import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.LinkOption;
import java.nio.file.AccessMode;
import java.nio.file.FileStore;
import java.nio.file.CopyOption;
import java.nio.file.ReadOnlyFileSystemException;
import java.nio.file.NotDirectoryException;
import java.nio.file.DirectoryStream;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.OpenOption;
import java.util.Set;
import java.nio.file.FileSystemNotFoundException;
import java.io.IOException;
import java.nio.file.FileSystemAlreadyExistsException;
import org.springframework.boot.loader.net.protocol.nested.NestedLocation;
import java.nio.file.FileSystem;
import java.net.URI;
import java.util.HashMap;
import java.nio.file.Path;
import java.util.Map;
import java.nio.file.spi.FileSystemProvider;

public class NestedFileSystemProvider extends FileSystemProvider
{
    private final Map<Path, NestedFileSystem> fileSystems;
    
    public NestedFileSystemProvider() {
        this.fileSystems = new HashMap<Path, NestedFileSystem>();
    }
    
    @Override
    public String getScheme() {
        return "nested";
    }
    
    @Override
    public FileSystem newFileSystem(final URI uri, final Map<String, ?> env) throws IOException {
        final NestedLocation location = NestedLocation.fromUri(uri);
        final Path jarPath = location.path();
        synchronized (this.fileSystems) {
            if (this.fileSystems.containsKey(jarPath)) {
                throw new FileSystemAlreadyExistsException();
            }
            final NestedFileSystem fileSystem = new NestedFileSystem(this, location.path());
            this.fileSystems.put(location.path(), fileSystem);
            return fileSystem;
        }
    }
    
    @Override
    public FileSystem getFileSystem(final URI uri) {
        final NestedLocation location = NestedLocation.fromUri(uri);
        synchronized (this.fileSystems) {
            final NestedFileSystem fileSystem = this.fileSystems.get(location.path());
            if (fileSystem == null) {
                throw new FileSystemNotFoundException();
            }
            return fileSystem;
        }
    }
    
    @Override
    public Path getPath(final URI uri) {
        final NestedLocation location = NestedLocation.fromUri(uri);
        synchronized (this.fileSystems) {
            final NestedFileSystem fileSystem = this.fileSystems.computeIfAbsent(location.path(), path -> new NestedFileSystem(this, path));
            fileSystem.installZipFileSystemIfNecessary(location.nestedEntryName());
            return fileSystem.getPath(location.nestedEntryName(), new String[0]);
        }
    }
    
    void removeFileSystem(final NestedFileSystem fileSystem) {
        synchronized (this.fileSystems) {
            this.fileSystems.remove(fileSystem.getJarPath());
        }
    }
    
    @Override
    public SeekableByteChannel newByteChannel(final Path path, final Set<? extends OpenOption> options, final FileAttribute<?>... attrs) throws IOException {
        final NestedPath nestedPath = NestedPath.cast(path);
        return new NestedByteChannel(nestedPath.getJarPath(), nestedPath.getNestedEntryName());
    }
    
    @Override
    public DirectoryStream<Path> newDirectoryStream(final Path dir, final DirectoryStream.Filter<? super Path> filter) throws IOException {
        throw new NotDirectoryException(NestedPath.cast(dir).toString());
    }
    
    @Override
    public void createDirectory(final Path dir, final FileAttribute<?>... attrs) throws IOException {
        throw new ReadOnlyFileSystemException();
    }
    
    @Override
    public void delete(final Path path) throws IOException {
        throw new ReadOnlyFileSystemException();
    }
    
    @Override
    public void copy(final Path source, final Path target, final CopyOption... options) throws IOException {
        throw new ReadOnlyFileSystemException();
    }
    
    @Override
    public void move(final Path source, final Path target, final CopyOption... options) throws IOException {
        throw new ReadOnlyFileSystemException();
    }
    
    @Override
    public boolean isSameFile(final Path path, final Path path2) throws IOException {
        return path.equals(path2);
    }
    
    @Override
    public boolean isHidden(final Path path) throws IOException {
        return false;
    }
    
    @Override
    public FileStore getFileStore(final Path path) throws IOException {
        final NestedPath nestedPath = NestedPath.cast(path);
        nestedPath.assertExists();
        return new NestedFileStore(nestedPath.getFileSystem());
    }
    
    @Override
    public void checkAccess(final Path path, final AccessMode... modes) throws IOException {
        final Path jarPath = this.getJarPath(path);
        jarPath.getFileSystem().provider().checkAccess(jarPath, modes);
    }
    
    @Override
    public <V extends FileAttributeView> V getFileAttributeView(final Path path, final Class<V> type, final LinkOption... options) {
        final Path jarPath = this.getJarPath(path);
        return jarPath.getFileSystem().provider().getFileAttributeView(jarPath, type, options);
    }
    
    @Override
    public <A extends BasicFileAttributes> A readAttributes(final Path path, final Class<A> type, final LinkOption... options) throws IOException {
        final Path jarPath = this.getJarPath(path);
        return jarPath.getFileSystem().provider().readAttributes(jarPath, type, options);
    }
    
    @Override
    public Map<String, Object> readAttributes(final Path path, final String attributes, final LinkOption... options) throws IOException {
        final Path jarPath = this.getJarPath(path);
        return jarPath.getFileSystem().provider().readAttributes(jarPath, attributes, options);
    }
    
    protected Path getJarPath(final Path path) {
        return NestedPath.cast(path).getJarPath();
    }
    
    @Override
    public void setAttribute(final Path path, final String attribute, final Object value, final LinkOption... options) throws IOException {
        throw new ReadOnlyFileSystemException();
    }
}
