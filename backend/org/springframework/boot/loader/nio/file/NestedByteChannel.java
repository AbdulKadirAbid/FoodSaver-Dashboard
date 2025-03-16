// 
// Decompiled by Procyon v0.6.0
// 

package org.springframework.boot.loader.nio.file;

import org.springframework.boot.loader.zip.DataBlock;
import org.springframework.boot.loader.zip.CloseableDataBlock;
import org.springframework.boot.loader.zip.ZipContent;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.NonWritableChannelException;
import java.nio.ByteBuffer;
import java.io.UncheckedIOException;
import java.io.IOException;
import java.nio.file.Path;
import java.lang.ref.Cleaner;
import java.nio.channels.SeekableByteChannel;

class NestedByteChannel implements SeekableByteChannel
{
    private long position;
    private final Resources resources;
    private final Cleaner.Cleanable cleanup;
    private final long size;
    private volatile boolean closed;
    
    NestedByteChannel(final Path path, final String nestedEntryName) throws IOException {
        this(path, nestedEntryName, org.springframework.boot.loader.ref.Cleaner.instance);
    }
    
    NestedByteChannel(final Path path, final String nestedEntryName, final org.springframework.boot.loader.ref.Cleaner cleaner) throws IOException {
        this.resources = new Resources(path, nestedEntryName);
        this.cleanup = cleaner.register(this, this.resources);
        this.size = this.resources.getData().size();
    }
    
    @Override
    public boolean isOpen() {
        return !this.closed;
    }
    
    @Override
    public void close() throws IOException {
        if (this.closed) {
            return;
        }
        this.closed = true;
        try {
            this.cleanup.clean();
        }
        catch (final UncheckedIOException ex) {
            throw ex.getCause();
        }
    }
    
    @Override
    public int read(final ByteBuffer dst) throws IOException {
        this.assertNotClosed();
        int total = 0;
        while (dst.remaining() > 0) {
            final int count = this.resources.getData().read(dst, this.position);
            if (count <= 0) {
                return (total != 0) ? 0 : count;
            }
            total += count;
            this.position += count;
        }
        return total;
    }
    
    @Override
    public int write(final ByteBuffer src) throws IOException {
        throw new NonWritableChannelException();
    }
    
    @Override
    public long position() throws IOException {
        this.assertNotClosed();
        return this.position;
    }
    
    @Override
    public SeekableByteChannel position(final long position) throws IOException {
        this.assertNotClosed();
        if (position < 0L || position >= this.size) {
            throw new IllegalArgumentException("Position must be in bounds");
        }
        this.position = position;
        return this;
    }
    
    @Override
    public long size() throws IOException {
        this.assertNotClosed();
        return this.size;
    }
    
    @Override
    public SeekableByteChannel truncate(final long size) throws IOException {
        throw new NonWritableChannelException();
    }
    
    private void assertNotClosed() throws ClosedChannelException {
        if (this.closed) {
            throw new ClosedChannelException();
        }
    }
    
    static class Resources implements Runnable
    {
        private final ZipContent zipContent;
        private final CloseableDataBlock data;
        
        Resources(final Path path, final String nestedEntryName) throws IOException {
            this.zipContent = ZipContent.open(path, nestedEntryName);
            this.data = this.zipContent.openRawZipData();
        }
        
        DataBlock getData() {
            return this.data;
        }
        
        @Override
        public void run() {
            this.releaseAll();
        }
        
        private void releaseAll() {
            IOException exception = null;
            try {
                this.data.close();
            }
            catch (final IOException ex) {
                exception = ex;
            }
            try {
                this.zipContent.close();
            }
            catch (final IOException ex) {
                if (exception != null) {
                    ex.addSuppressed(exception);
                }
                exception = ex;
            }
            if (exception != null) {
                throw new UncheckedIOException(exception);
            }
        }
    }
}
