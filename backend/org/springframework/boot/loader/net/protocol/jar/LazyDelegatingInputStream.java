// 
// Decompiled by Procyon v0.6.0
// 

package org.springframework.boot.loader.net.protocol.jar;

import java.io.IOException;
import java.io.InputStream;

abstract class LazyDelegatingInputStream extends InputStream
{
    private volatile InputStream in;
    
    @Override
    public int read() throws IOException {
        return this.in().read();
    }
    
    @Override
    public int read(final byte[] b) throws IOException {
        return this.in().read(b);
    }
    
    @Override
    public int read(final byte[] b, final int off, final int len) throws IOException {
        return this.in().read(b, off, len);
    }
    
    @Override
    public long skip(final long n) throws IOException {
        return this.in().skip(n);
    }
    
    @Override
    public int available() throws IOException {
        return this.in().available();
    }
    
    @Override
    public boolean markSupported() {
        try {
            return this.in().markSupported();
        }
        catch (final IOException ex) {
            return false;
        }
    }
    
    @Override
    public synchronized void mark(final int readLimit) {
        try {
            this.in().mark(readLimit);
        }
        catch (final IOException ex) {}
    }
    
    @Override
    public synchronized void reset() throws IOException {
        this.in().reset();
    }
    
    private InputStream in() throws IOException {
        InputStream in = this.in;
        if (in == null) {
            synchronized (this) {
                in = this.in;
                if (in == null) {
                    in = this.getDelegateInputStream();
                    this.in = in;
                }
            }
        }
        return in;
    }
    
    @Override
    public void close() throws IOException {
        InputStream in = this.in;
        if (in != null) {
            synchronized (this) {
                in = this.in;
                if (in != null) {
                    in.close();
                }
            }
        }
    }
    
    protected abstract InputStream getDelegateInputStream() throws IOException;
}
