// 
// Decompiled by Procyon v0.6.0
// 

package org.springframework.boot.loader.jar;

import java.io.EOFException;
import java.util.Arrays;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.jar.JarEntry;
import java.util.zip.Inflater;
import java.util.jar.JarInputStream;
import java.io.Closeable;

class JarEntriesStream implements Closeable
{
    private static final int BUFFER_SIZE = 4096;
    private final JarInputStream in;
    private final byte[] inBuffer;
    private final byte[] compareBuffer;
    private final Inflater inflater;
    private JarEntry entry;
    
    JarEntriesStream(final InputStream in) throws IOException {
        this.inBuffer = new byte[4096];
        this.compareBuffer = new byte[4096];
        this.inflater = new Inflater(true);
        this.in = new JarInputStream(in);
    }
    
    JarEntry getNextEntry() throws IOException {
        this.entry = this.in.getNextJarEntry();
        this.inflater.reset();
        return this.entry;
    }
    
    boolean matches(final boolean directory, final int size, final int compressionMethod, final InputStreamSupplier streamSupplier) throws IOException {
        if (this.entry.isDirectory() != directory) {
            this.fail("directory");
        }
        if (this.entry.getMethod() != compressionMethod) {
            this.fail("compression method");
        }
        if (this.entry.isDirectory()) {
            this.in.closeEntry();
            return true;
        }
        try (final DataInputStream expected = new DataInputStream(this.getInputStream(size, streamSupplier))) {
            this.assertSameContent(expected);
        }
        return true;
    }
    
    private InputStream getInputStream(final int size, final InputStreamSupplier streamSupplier) throws IOException {
        final InputStream inputStream = streamSupplier.get();
        return (this.entry.getMethod() != 8) ? inputStream : new ZipInflaterInputStream(inputStream, this.inflater, size);
    }
    
    private void assertSameContent(final DataInputStream expected) throws IOException {
        int len;
        while ((len = this.in.read(this.inBuffer)) > 0) {
            try {
                expected.readFully(this.compareBuffer, 0, len);
                if (Arrays.equals(this.inBuffer, 0, len, this.compareBuffer, 0, len)) {
                    continue;
                }
            }
            catch (final EOFException ex) {}
            this.fail("content");
        }
        if (expected.read() != -1) {
            this.fail("content");
        }
    }
    
    private void fail(final String check) {
        throw new IllegalStateException("Content mismatch when reading security info for entry '%s' (%s check)".formatted(this.entry.getName(), check));
    }
    
    @Override
    public void close() throws IOException {
        this.inflater.end();
        this.in.close();
    }
    
    @FunctionalInterface
    interface InputStreamSupplier
    {
        InputStream get() throws IOException;
    }
}
