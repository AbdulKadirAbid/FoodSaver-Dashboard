// 
// Decompiled by Procyon v0.6.0
// 

package org.springframework.boot.loader.zip;

import java.nio.ByteBuffer;
import java.io.IOException;

class ByteArrayDataBlock implements CloseableDataBlock
{
    private final byte[] bytes;
    private final int maxReadSize;
    
    ByteArrayDataBlock(final byte... bytes) {
        this(bytes, -1);
    }
    
    ByteArrayDataBlock(final byte[] bytes, final int maxReadSize) {
        this.bytes = bytes;
        this.maxReadSize = maxReadSize;
    }
    
    @Override
    public long size() throws IOException {
        return this.bytes.length;
    }
    
    @Override
    public int read(final ByteBuffer dst, final long pos) throws IOException {
        return this.read(dst, (int)pos);
    }
    
    private int read(final ByteBuffer dst, final int pos) {
        final int remaining = dst.remaining();
        int length = Math.min(this.bytes.length - pos, remaining);
        if (this.maxReadSize > 0 && length > this.maxReadSize) {
            length = this.maxReadSize;
        }
        dst.put(this.bytes, pos, length);
        return length;
    }
    
    @Override
    public void close() throws IOException {
    }
}
