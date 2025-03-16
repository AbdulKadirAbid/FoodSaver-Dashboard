// 
// Decompiled by Procyon v0.6.0
// 

package org.springframework.boot.loader.zip;

import java.nio.ByteBuffer;
import java.util.Iterator;
import java.io.IOException;
import java.util.Collection;

class VirtualDataBlock implements DataBlock
{
    private DataBlock[] parts;
    private long[] offsets;
    private long size;
    private volatile int lastReadPart;
    
    protected VirtualDataBlock() {
        this.lastReadPart = 0;
    }
    
    VirtualDataBlock(final Collection<? extends DataBlock> parts) throws IOException {
        this.lastReadPart = 0;
        this.setParts(parts);
    }
    
    protected void setParts(final Collection<? extends DataBlock> parts) throws IOException {
        this.parts = parts.toArray(DataBlock[]::new);
        this.offsets = new long[parts.size()];
        long size = 0L;
        int i = 0;
        for (final DataBlock part : parts) {
            this.offsets[i++] = size;
            size += part.size();
        }
        this.size = size;
    }
    
    @Override
    public long size() throws IOException {
        return this.size;
    }
    
    @Override
    public int read(final ByteBuffer dst, long pos) throws IOException {
        if (pos < 0L || pos >= this.size) {
            return -1;
        }
        final int lastReadPart = this.lastReadPart;
        int partIndex = 0;
        long offset = 0L;
        int result = 0;
        if (pos >= this.offsets[lastReadPart]) {
            partIndex = lastReadPart;
            offset = this.offsets[lastReadPart];
        }
        while (partIndex < this.parts.length) {
            DataBlock part;
            int count;
            for (part = this.parts[partIndex]; pos >= offset && pos < offset + part.size(); pos += count) {
                count = part.read(dst, pos - offset);
                result += Math.max(count, 0);
                if (count <= 0 || !dst.hasRemaining()) {
                    this.lastReadPart = partIndex;
                    return result;
                }
            }
            offset += part.size();
            ++partIndex;
        }
        return result;
    }
}
