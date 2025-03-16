// 
// Decompiled by Procyon v0.6.0
// 

package org.springframework.boot.loader.zip;

import java.io.Closeable;
import java.nio.ByteBuffer;
import java.io.IOException;
import java.io.InputStream;

class DataBlockInputStream extends InputStream
{
    private final DataBlock dataBlock;
    private long pos;
    private long remaining;
    private volatile boolean closed;
    
    DataBlockInputStream(final DataBlock dataBlock) throws IOException {
        this.dataBlock = dataBlock;
        this.remaining = dataBlock.size();
    }
    
    @Override
    public int read() throws IOException {
        final byte[] b = { 0 };
        return (this.read(b, 0, 1) == 1) ? (b[0] & 0xFF) : -1;
    }
    
    @Override
    public int read(final byte[] b, final int off, final int len) throws IOException {
        this.ensureOpen();
        final ByteBuffer dst = ByteBuffer.wrap(b, off, len);
        final int count = this.dataBlock.read(dst, this.pos);
        if (count > 0) {
            this.pos += count;
            this.remaining -= count;
        }
        return count;
    }
    
    @Override
    public long skip(final long n) throws IOException {
        final long count = (n > 0L) ? this.maxForwardSkip(n) : this.maxBackwardSkip(n);
        this.pos += count;
        this.remaining -= count;
        return count;
    }
    
    private long maxForwardSkip(final long n) {
        final boolean willCauseOverflow = this.pos + n < 0L;
        return (willCauseOverflow || n > this.remaining) ? this.remaining : n;
    }
    
    private long maxBackwardSkip(final long n) {
        return Math.max(-this.pos, n);
    }
    
    @Override
    public int available() {
        if (this.closed) {
            return 0;
        }
        return (this.remaining < 2147483647L) ? ((int)this.remaining) : Integer.MAX_VALUE;
    }
    
    private void ensureOpen() throws IOException {
        if (this.closed) {
            throw new IOException("InputStream closed");
        }
    }
    
    @Override
    public void close() throws IOException {
        if (this.closed) {
            return;
        }
        this.closed = true;
        final DataBlock dataBlock = this.dataBlock;
        if (dataBlock instanceof final Closeable closeable) {
            closeable.close();
        }
    }
}
