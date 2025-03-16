// 
// Decompiled by Procyon v0.6.0
// 

package org.springframework.boot.loader.net.protocol.nested;

import java.io.UncheckedIOException;
import org.springframework.boot.loader.zip.CloseableDataBlock;
import java.io.IOException;
import java.io.InputStream;
import org.springframework.boot.loader.zip.ZipContent;

class NestedUrlConnectionResources implements Runnable
{
    private final NestedLocation location;
    private volatile ZipContent zipContent;
    private volatile long size;
    private volatile InputStream inputStream;
    
    NestedUrlConnectionResources(final NestedLocation location) {
        this.size = -1L;
        this.location = location;
    }
    
    NestedLocation getLocation() {
        return this.location;
    }
    
    void connect() throws IOException {
        synchronized (this) {
            if (this.zipContent == null) {
                this.zipContent = ZipContent.open(this.location.path(), this.location.nestedEntryName());
                try {
                    this.connectData();
                }
                catch (final IOException | RuntimeException ex) {
                    this.zipContent.close();
                    this.zipContent = null;
                    throw ex;
                }
            }
        }
    }
    
    private void connectData() throws IOException {
        final CloseableDataBlock data = this.zipContent.openRawZipData();
        try {
            this.size = data.size();
            this.inputStream = data.asInputStream();
        }
        catch (final IOException | RuntimeException ex) {
            data.close();
        }
    }
    
    InputStream getInputStream() throws IOException {
        synchronized (this) {
            if (this.inputStream == null) {
                throw new IOException("Nested location not found " + this.location);
            }
            return this.inputStream;
        }
    }
    
    long getContentLength() {
        return this.size;
    }
    
    @Override
    public void run() {
        this.releaseAll();
    }
    
    private void releaseAll() {
        synchronized (this) {
            if (this.zipContent != null) {
                IOException exceptionChain = null;
                try {
                    this.inputStream.close();
                }
                catch (final IOException ex) {
                    exceptionChain = this.addToExceptionChain(exceptionChain, ex);
                }
                try {
                    this.zipContent.close();
                }
                catch (final IOException ex) {
                    exceptionChain = this.addToExceptionChain(exceptionChain, ex);
                }
                this.size = -1L;
                if (exceptionChain != null) {
                    throw new UncheckedIOException(exceptionChain);
                }
            }
        }
    }
    
    private IOException addToExceptionChain(final IOException exceptionChain, final IOException ex) {
        if (exceptionChain != null) {
            exceptionChain.addSuppressed(ex);
            return exceptionChain;
        }
        return ex;
    }
}
