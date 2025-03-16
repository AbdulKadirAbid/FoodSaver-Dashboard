// 
// Decompiled by Procyon v0.6.0
// 

package org.springframework.boot.loader.nio.file;

import java.nio.file.Files;
import java.io.UncheckedIOException;
import java.nio.file.attribute.FileStoreAttributeView;
import java.nio.file.attribute.FileAttributeView;
import java.io.IOException;
import java.nio.file.FileStore;

class NestedFileStore extends FileStore
{
    private final NestedFileSystem fileSystem;
    
    NestedFileStore(final NestedFileSystem fileSystem) {
        this.fileSystem = fileSystem;
    }
    
    @Override
    public String name() {
        return this.fileSystem.toString();
    }
    
    @Override
    public String type() {
        return "nestedfs";
    }
    
    @Override
    public boolean isReadOnly() {
        return this.fileSystem.isReadOnly();
    }
    
    @Override
    public long getTotalSpace() throws IOException {
        return 0L;
    }
    
    @Override
    public long getUsableSpace() throws IOException {
        return 0L;
    }
    
    @Override
    public long getUnallocatedSpace() throws IOException {
        return 0L;
    }
    
    @Override
    public boolean supportsFileAttributeView(final Class<? extends FileAttributeView> type) {
        return this.getJarPathFileStore().supportsFileAttributeView(type);
    }
    
    @Override
    public boolean supportsFileAttributeView(final String name) {
        return this.getJarPathFileStore().supportsFileAttributeView(name);
    }
    
    @Override
    public <V extends FileStoreAttributeView> V getFileStoreAttributeView(final Class<V> type) {
        return this.getJarPathFileStore().getFileStoreAttributeView(type);
    }
    
    @Override
    public Object getAttribute(final String attribute) throws IOException {
        try {
            return this.getJarPathFileStore().getAttribute(attribute);
        }
        catch (final UncheckedIOException ex) {
            throw ex.getCause();
        }
    }
    
    protected FileStore getJarPathFileStore() {
        try {
            return Files.getFileStore(this.fileSystem.getJarPath());
        }
        catch (final IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }
}
