// 
// Decompiled by Procyon v0.6.0
// 

package org.springframework.boot.loader.zip;

import java.io.InputStream;
import java.io.EOFException;
import java.nio.ByteBuffer;
import java.io.IOException;

public interface DataBlock
{
    long size() throws IOException;
    
    int read(final ByteBuffer dst, final long pos) throws IOException;
    
    default void readFully(final ByteBuffer dst, long pos) throws IOException {
        do {
            final int count = this.read(dst, pos);
            if (count <= 0) {
                throw new EOFException();
            }
            pos += count;
        } while (dst.hasRemaining());
    }
    
    default InputStream asInputStream() throws IOException {
        return new DataBlockInputStream(this);
    }
}
