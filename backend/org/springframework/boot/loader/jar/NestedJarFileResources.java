// 
// Decompiled by Procyon v0.6.0
// 

package org.springframework.boot.loader.jar;

import java.util.Iterator;
import java.util.Collection;
import java.util.List;
import java.io.UncheckedIOException;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Map;
import java.util.Collections;
import java.util.WeakHashMap;
import java.io.File;
import java.util.zip.Inflater;
import java.util.Deque;
import java.io.InputStream;
import java.util.Set;
import org.springframework.boot.loader.zip.ZipContent;

class NestedJarFileResources implements Runnable
{
    private static final int INFLATER_CACHE_LIMIT = 20;
    private ZipContent zipContent;
    private ZipContent zipContentForManifest;
    private final Set<InputStream> inputStreams;
    private Deque<Inflater> inflaterCache;
    
    NestedJarFileResources(final File file, final String nestedEntryName) throws IOException {
        this.inputStreams = Collections.newSetFromMap(new WeakHashMap<InputStream, Boolean>());
        this.inflaterCache = new ArrayDeque<Inflater>();
        this.zipContent = ZipContent.open(file.toPath(), nestedEntryName);
        this.zipContentForManifest = ((this.zipContent.getKind() != ZipContent.Kind.NESTED_DIRECTORY) ? null : ZipContent.open(file.toPath()));
    }
    
    ZipContent zipContent() {
        return this.zipContent;
    }
    
    ZipContent zipContentForManifest() {
        return (this.zipContentForManifest != null) ? this.zipContentForManifest : this.zipContent;
    }
    
    void addInputStream(final InputStream inputStream) {
        synchronized (this.inputStreams) {
            this.inputStreams.add(inputStream);
        }
    }
    
    void removeInputStream(final InputStream inputStream) {
        synchronized (this.inputStreams) {
            this.inputStreams.remove(inputStream);
        }
    }
    
    Runnable createInflatorCleanupAction(final Inflater inflater) {
        return () -> this.endOrCacheInflater(inflater);
    }
    
    Inflater getOrCreateInflater() {
        final Deque<Inflater> inflaterCache = this.inflaterCache;
        if (inflaterCache != null) {
            synchronized (inflaterCache) {
                final Inflater inflater = this.inflaterCache.poll();
                if (inflater != null) {
                    return inflater;
                }
            }
        }
        return new Inflater(true);
    }
    
    private void endOrCacheInflater(final Inflater inflater) {
        final Deque<Inflater> inflaterCache = this.inflaterCache;
        if (inflaterCache != null) {
            synchronized (inflaterCache) {
                if (this.inflaterCache == inflaterCache && inflaterCache.size() < 20) {
                    inflater.reset();
                    this.inflaterCache.add(inflater);
                    return;
                }
            }
        }
        inflater.end();
    }
    
    @Override
    public void run() {
        this.releaseAll();
    }
    
    private void releaseAll() {
        IOException exceptionChain = null;
        exceptionChain = this.releaseInflators(exceptionChain);
        exceptionChain = this.releaseInputStreams(exceptionChain);
        exceptionChain = this.releaseZipContent(exceptionChain);
        exceptionChain = this.releaseZipContentForManifest(exceptionChain);
        if (exceptionChain != null) {
            throw new UncheckedIOException(exceptionChain);
        }
    }
    
    private IOException releaseInflators(final IOException exceptionChain) {
        final Deque<Inflater> inflaterCache = this.inflaterCache;
        if (inflaterCache != null) {
            try {
                synchronized (inflaterCache) {
                    inflaterCache.forEach(Inflater::end);
                }
            }
            finally {
                this.inflaterCache = null;
            }
        }
        return exceptionChain;
    }
    
    private IOException releaseInputStreams(IOException exceptionChain) {
        synchronized (this.inputStreams) {
            for (final InputStream inputStream : List.copyOf((Collection<? extends InputStream>)this.inputStreams)) {
                try {
                    inputStream.close();
                }
                catch (final IOException ex) {
                    exceptionChain = this.addToExceptionChain(exceptionChain, ex);
                }
            }
            this.inputStreams.clear();
        }
        return exceptionChain;
    }
    
    private IOException releaseZipContent(IOException exceptionChain) {
        final ZipContent zipContent = this.zipContent;
        if (zipContent != null) {
            try {
                zipContent.close();
            }
            catch (final IOException ex) {
                exceptionChain = this.addToExceptionChain(exceptionChain, ex);
            }
            finally {
                this.zipContent = null;
            }
        }
        return exceptionChain;
    }
    
    private IOException releaseZipContentForManifest(IOException exceptionChain) {
        final ZipContent zipContentForManifest = this.zipContentForManifest;
        if (zipContentForManifest != null) {
            try {
                zipContentForManifest.close();
            }
            catch (final IOException ex) {
                exceptionChain = this.addToExceptionChain(exceptionChain, ex);
            }
            finally {
                this.zipContentForManifest = null;
            }
        }
        return exceptionChain;
    }
    
    private IOException addToExceptionChain(final IOException exceptionChain, final IOException ex) {
        if (exceptionChain != null) {
            exceptionChain.addSuppressed(ex);
            return exceptionChain;
        }
        return ex;
    }
}
