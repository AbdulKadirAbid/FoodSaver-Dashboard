// 
// Decompiled by Procyon v0.6.0
// 

package org.springframework.boot.loader.zip;

import java.nio.file.StandardOpenOption;
import java.nio.file.OpenOption;
import java.nio.channels.ClosedByInterruptException;
import java.nio.file.LinkOption;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.util.function.Supplier;
import java.nio.channels.ClosedChannelException;
import java.nio.ByteBuffer;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.boot.loader.log.DebugLogger;

class FileDataBlock implements CloseableDataBlock
{
    private static final DebugLogger debug;
    static Tracker tracker;
    private final FileAccess fileAccess;
    private final long offset;
    private final long size;
    
    FileDataBlock(final Path path) throws IOException {
        this.fileAccess = new FileAccess(path);
        this.offset = 0L;
        this.size = Files.size(path);
    }
    
    FileDataBlock(final FileAccess fileAccess, final long offset, final long size) {
        this.fileAccess = fileAccess;
        this.offset = offset;
        this.size = size;
    }
    
    @Override
    public long size() throws IOException {
        return this.size;
    }
    
    @Override
    public int read(final ByteBuffer dst, final long pos) throws IOException {
        if (pos < 0L) {
            throw new IllegalArgumentException("Position must not be negative");
        }
        this.ensureOpen(ClosedChannelException::new);
        final long remaining = this.size - pos;
        if (remaining <= 0L) {
            return -1;
        }
        int originalDestinationLimit = -1;
        if (dst.remaining() > remaining) {
            originalDestinationLimit = dst.limit();
            final long updatedLimit = dst.position() + remaining;
            dst.limit((updatedLimit > 2147483647L) ? Integer.MAX_VALUE : ((int)updatedLimit));
        }
        final int result = this.fileAccess.read(dst, this.offset + pos);
        if (originalDestinationLimit != -1) {
            dst.limit(originalDestinationLimit);
        }
        return result;
    }
    
    void open() throws IOException {
        this.fileAccess.open();
    }
    
    @Override
    public void close() throws IOException {
        this.fileAccess.close();
    }
    
     <E extends Exception> void ensureOpen(final Supplier<E> exceptionSupplier) throws E, Exception {
        this.fileAccess.ensureOpen(exceptionSupplier);
    }
    
    FileDataBlock slice(final long offset) throws IOException {
        return this.slice(offset, this.size - offset);
    }
    
    FileDataBlock slice(final long offset, final long size) {
        if (offset == 0L && size == this.size) {
            return this;
        }
        if (offset < 0L) {
            throw new IllegalArgumentException("Offset must not be negative");
        }
        if (size < 0L || offset + size > this.size) {
            throw new IllegalArgumentException("Size must not be negative and must be within bounds");
        }
        FileDataBlock.debug.log("Slicing %s at %s with size %s", this.fileAccess, offset, size);
        return new FileDataBlock(this.fileAccess, this.offset + offset, size);
    }
    
    static {
        debug = DebugLogger.get(FileDataBlock.class);
        FileDataBlock.tracker = Tracker.NONE;
    }
    
    static class FileAccess
    {
        static final int BUFFER_SIZE = 10240;
        private final Path path;
        private int referenceCount;
        private FileChannel fileChannel;
        private boolean fileChannelInterrupted;
        private RandomAccessFile randomAccessFile;
        private ByteBuffer buffer;
        private long bufferPosition;
        private int bufferSize;
        private final Object lock;
        
        FileAccess(final Path path) {
            this.bufferPosition = -1L;
            this.lock = new Object();
            if (!Files.isRegularFile(path, new LinkOption[0])) {
                throw new IllegalArgumentException(path + " must be a regular file");
            }
            this.path = path;
        }
        
        int read(final ByteBuffer dst, final long position) throws IOException {
            synchronized (this.lock) {
                if (position < this.bufferPosition || position >= this.bufferPosition + this.bufferSize) {
                    this.fillBuffer(position);
                }
                if (this.bufferSize <= 0) {
                    return this.bufferSize;
                }
                final int offset = (int)(position - this.bufferPosition);
                final int length = Math.min(this.bufferSize - offset, dst.remaining());
                dst.put(dst.position(), this.buffer, offset, length);
                dst.position(dst.position() + length);
                return length;
            }
        }
        
        private void fillBuffer(final long position) throws IOException {
            if (Thread.currentThread().isInterrupted()) {
                this.fillBufferUsingRandomAccessFile(position);
                return;
            }
            try {
                if (this.fileChannelInterrupted) {
                    this.repairFileChannel();
                    this.fileChannelInterrupted = false;
                }
                this.buffer.clear();
                this.bufferSize = this.fileChannel.read(this.buffer, position);
                this.bufferPosition = position;
            }
            catch (final ClosedByInterruptException ex) {
                this.fileChannelInterrupted = true;
                this.fillBufferUsingRandomAccessFile(position);
            }
        }
        
        private void fillBufferUsingRandomAccessFile(final long position) throws IOException {
            if (this.randomAccessFile == null) {
                this.randomAccessFile = new RandomAccessFile(this.path.toFile(), "r");
                FileDataBlock.tracker.openedFileChannel(this.path);
            }
            final byte[] bytes = new byte[10240];
            this.randomAccessFile.seek(position);
            final int len = this.randomAccessFile.read(bytes);
            this.buffer.clear();
            if (len > 0) {
                this.buffer.put(bytes, 0, len);
            }
            this.bufferSize = len;
            this.bufferPosition = position;
        }
        
        private void repairFileChannel() throws IOException {
            FileDataBlock.tracker.closedFileChannel(this.path);
            this.fileChannel = FileChannel.open(this.path, StandardOpenOption.READ);
            FileDataBlock.tracker.openedFileChannel(this.path);
        }
        
        void open() throws IOException {
            synchronized (this.lock) {
                if (this.referenceCount == 0) {
                    FileDataBlock.debug.log("Opening '%s'", this.path);
                    this.fileChannel = FileChannel.open(this.path, StandardOpenOption.READ);
                    this.buffer = ByteBuffer.allocateDirect(10240);
                    FileDataBlock.tracker.openedFileChannel(this.path);
                }
                ++this.referenceCount;
                FileDataBlock.debug.log("Reference count for '%s' incremented to %s", this.path, this.referenceCount);
            }
        }
        
        void close() throws IOException {
            synchronized (this.lock) {
                if (this.referenceCount == 0) {
                    return;
                }
                --this.referenceCount;
                if (this.referenceCount == 0) {
                    FileDataBlock.debug.log("Closing '%s'", this.path);
                    this.buffer = null;
                    this.bufferPosition = -1L;
                    this.bufferSize = 0;
                    this.fileChannel.close();
                    FileDataBlock.tracker.closedFileChannel(this.path);
                    this.fileChannel = null;
                    if (this.randomAccessFile != null) {
                        this.randomAccessFile.close();
                        FileDataBlock.tracker.closedFileChannel(this.path);
                        this.randomAccessFile = null;
                    }
                }
                FileDataBlock.debug.log("Reference count for '%s' decremented to %s", this.path, this.referenceCount);
            }
        }
        
         <E extends Exception> void ensureOpen(final Supplier<E> exceptionSupplier) throws E, Exception {
            synchronized (this.lock) {
                if (this.referenceCount == 0) {
                    throw exceptionSupplier.get();
                }
            }
        }
        
        @Override
        public String toString() {
            return this.path.toString();
        }
    }
    
    interface Tracker
    {
        public static final Tracker NONE = new Tracker() {
            @Override
            public void openedFileChannel(final Path path) {
            }
            
            @Override
            public void closedFileChannel(final Path path) {
            }
        };
        
        void openedFileChannel(final Path path);
        
        void closedFileChannel(final Path path);
    }
}
