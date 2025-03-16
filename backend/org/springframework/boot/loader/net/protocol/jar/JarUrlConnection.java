// 
// Decompiled by Procyon v0.6.0
// 

package org.springframework.boot.loader.net.protocol.jar;

import java.net.URLStreamHandler;
import java.io.ByteArrayInputStream;
import org.springframework.boot.loader.net.util.UrlDecoder;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.springframework.boot.loader.jar.NestedJarFile;
import java.security.Permission;
import java.io.BufferedInputStream;
import java.util.zip.ZipEntry;
import java.io.IOException;
import java.util.jar.JarEntry;
import java.net.URLConnection;
import java.util.jar.JarFile;
import java.util.function.Supplier;
import java.net.URL;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.JarURLConnection;

final class JarUrlConnection extends JarURLConnection
{
    static final UrlJarFiles jarFiles;
    static final InputStream emptyInputStream;
    static final FileNotFoundException FILE_NOT_FOUND_EXCEPTION;
    private static final URL NOT_FOUND_URL;
    static final JarUrlConnection NOT_FOUND_CONNECTION;
    private final String entryName;
    private final Supplier<FileNotFoundException> notFound;
    private JarFile jarFile;
    private URLConnection jarFileConnection;
    private JarEntry jarEntry;
    private String contentType;
    
    private JarUrlConnection(final URL url) throws IOException {
        super(url);
        this.entryName = this.getEntryName();
        this.notFound = null;
        (this.jarFileConnection = this.getJarFileURL().openConnection()).setUseCaches(this.useCaches);
    }
    
    private JarUrlConnection(final Supplier<FileNotFoundException> notFound) throws IOException {
        super(JarUrlConnection.NOT_FOUND_URL);
        this.entryName = null;
        this.notFound = notFound;
    }
    
    @Override
    public JarFile getJarFile() throws IOException {
        this.connect();
        return this.jarFile;
    }
    
    @Override
    public JarEntry getJarEntry() throws IOException {
        this.connect();
        return this.jarEntry;
    }
    
    @Override
    public int getContentLength() {
        final long contentLength = this.getContentLengthLong();
        return (contentLength <= 2147483647L) ? ((int)contentLength) : -1;
    }
    
    @Override
    public long getContentLengthLong() {
        try {
            this.connect();
            return (this.jarEntry != null) ? this.jarEntry.getSize() : this.jarFileConnection.getContentLengthLong();
        }
        catch (final IOException ex) {
            return -1L;
        }
    }
    
    @Override
    public String getContentType() {
        if (this.contentType == null) {
            this.contentType = this.deduceContentType();
        }
        return this.contentType;
    }
    
    private String deduceContentType() {
        String type = (this.entryName != null) ? null : "x-java/jar";
        type = ((type != null) ? type : this.deduceContentTypeFromStream());
        type = ((type != null) ? type : this.deduceContentTypeFromEntryName());
        return (type != null) ? type : "content/unknown";
    }
    
    private String deduceContentTypeFromStream() {
        try {
            this.connect();
            try (final InputStream in = this.jarFile.getInputStream(this.jarEntry)) {
                return URLConnection.guessContentTypeFromStream(new BufferedInputStream(in));
            }
        }
        catch (final IOException ex) {
            return null;
        }
    }
    
    private String deduceContentTypeFromEntryName() {
        return URLConnection.guessContentTypeFromName(this.entryName);
    }
    
    @Override
    public long getLastModified() {
        return (this.jarFileConnection != null) ? this.jarFileConnection.getLastModified() : super.getLastModified();
    }
    
    @Override
    public String getHeaderField(final String name) {
        return (this.jarFileConnection != null) ? this.jarFileConnection.getHeaderField(name) : null;
    }
    
    @Override
    public Object getContent() throws IOException {
        this.connect();
        return (this.entryName != null) ? super.getContent() : this.jarFile;
    }
    
    @Override
    public Permission getPermission() throws IOException {
        return (this.jarFileConnection != null) ? this.jarFileConnection.getPermission() : null;
    }
    
    @Override
    public InputStream getInputStream() throws IOException {
        if (this.notFound != null) {
            this.throwFileNotFound();
        }
        final URL jarFileURL = this.getJarFileURL();
        if (this.entryName == null && !UrlJarFileFactory.isNestedUrl(jarFileURL)) {
            throw new IOException("no entry name specified");
        }
        if (!this.getUseCaches() && Optimizations.isEnabled(false) && this.entryName != null) {
            final JarFile cached = JarUrlConnection.jarFiles.getCached(jarFileURL);
            if (cached != null && cached.getEntry(this.entryName) != null) {
                return JarUrlConnection.emptyInputStream;
            }
        }
        this.connect();
        if (this.jarEntry == null) {
            final JarFile jarFile = this.jarFile;
            if (jarFile instanceof final NestedJarFile nestedJarFile) {
                return nestedJarFile.getRawZipDataInputStream();
            }
            this.throwFileNotFound();
        }
        return new ConnectionInputStream();
    }
    
    @Override
    public boolean getAllowUserInteraction() {
        return this.jarFileConnection != null && this.jarFileConnection.getAllowUserInteraction();
    }
    
    @Override
    public void setAllowUserInteraction(final boolean allowUserInteraction) {
        if (this.jarFileConnection != null) {
            this.jarFileConnection.setAllowUserInteraction(allowUserInteraction);
        }
    }
    
    @Override
    public boolean getUseCaches() {
        return this.jarFileConnection == null || this.jarFileConnection.getUseCaches();
    }
    
    @Override
    public void setUseCaches(final boolean useCaches) {
        if (this.jarFileConnection != null) {
            this.jarFileConnection.setUseCaches(useCaches);
        }
    }
    
    @Override
    public boolean getDefaultUseCaches() {
        return this.jarFileConnection == null || this.jarFileConnection.getDefaultUseCaches();
    }
    
    @Override
    public void setDefaultUseCaches(final boolean defaultUseCaches) {
        if (this.jarFileConnection != null) {
            this.jarFileConnection.setDefaultUseCaches(defaultUseCaches);
        }
    }
    
    @Override
    public void setIfModifiedSince(final long ifModifiedSince) {
        if (this.jarFileConnection != null) {
            this.jarFileConnection.setIfModifiedSince(ifModifiedSince);
        }
    }
    
    @Override
    public String getRequestProperty(final String key) {
        return (this.jarFileConnection != null) ? this.jarFileConnection.getRequestProperty(key) : null;
    }
    
    @Override
    public void setRequestProperty(final String key, final String value) {
        if (this.jarFileConnection != null) {
            this.jarFileConnection.setRequestProperty(key, value);
        }
    }
    
    @Override
    public void addRequestProperty(final String key, final String value) {
        if (this.jarFileConnection != null) {
            this.jarFileConnection.addRequestProperty(key, value);
        }
    }
    
    @Override
    public Map<String, List<String>> getRequestProperties() {
        return (this.jarFileConnection != null) ? this.jarFileConnection.getRequestProperties() : Collections.emptyMap();
    }
    
    @Override
    public void connect() throws IOException {
        if (this.connected) {
            return;
        }
        if (this.notFound != null) {
            this.throwFileNotFound();
        }
        final boolean useCaches = this.getUseCaches();
        final URL jarFileURL = this.getJarFileURL();
        if (this.entryName != null && Optimizations.isEnabled()) {
            this.assertCachedJarFileHasEntry(jarFileURL, this.entryName);
        }
        this.jarFile = JarUrlConnection.jarFiles.getOrCreate(useCaches, jarFileURL);
        this.jarEntry = this.getJarEntry(jarFileURL);
        final boolean addedToCache = JarUrlConnection.jarFiles.cacheIfAbsent(useCaches, jarFileURL, this.jarFile);
        if (addedToCache) {
            this.jarFileConnection = JarUrlConnection.jarFiles.reconnect(this.jarFile, this.jarFileConnection);
        }
        this.connected = true;
    }
    
    private void assertCachedJarFileHasEntry(final URL jarFileURL, final String entryName) throws FileNotFoundException {
        final JarFile cachedJarFile = JarUrlConnection.jarFiles.getCached(jarFileURL);
        if (cachedJarFile != null && cachedJarFile.getJarEntry(entryName) == null) {
            throw JarUrlConnection.FILE_NOT_FOUND_EXCEPTION;
        }
    }
    
    private JarEntry getJarEntry(final URL jarFileUrl) throws IOException {
        if (this.entryName == null) {
            return null;
        }
        final JarEntry jarEntry = this.jarFile.getJarEntry(this.entryName);
        if (jarEntry == null) {
            JarUrlConnection.jarFiles.closeIfNotCached(jarFileUrl, this.jarFile);
            this.throwFileNotFound();
        }
        return jarEntry;
    }
    
    private void throwFileNotFound() throws FileNotFoundException {
        if (Optimizations.isEnabled()) {
            throw JarUrlConnection.FILE_NOT_FOUND_EXCEPTION;
        }
        if (this.notFound != null) {
            throw this.notFound.get();
        }
        throw new FileNotFoundException("JAR entry " + this.entryName + " not found in " + this.jarFile.getName());
    }
    
    static JarUrlConnection open(final URL url) throws IOException {
        final String spec = url.getFile();
        if (spec.startsWith("nested:")) {
            final int separator = spec.indexOf("!/");
            final boolean specHasEntry = separator != -1 && separator + 2 != spec.length();
            if (specHasEntry) {
                URL jarFileUrl = new URL(spec.substring(0, separator));
                if ("runtime".equals(url.getRef())) {
                    jarFileUrl = new URL(jarFileUrl, "#runtime");
                }
                final String entryName = UrlDecoder.decode(spec.substring(separator + 2));
                final JarFile jarFile = JarUrlConnection.jarFiles.getOrCreate(true, jarFileUrl);
                JarUrlConnection.jarFiles.cacheIfAbsent(true, jarFileUrl, jarFile);
                if (!hasEntry(jarFile, entryName)) {
                    return notFoundConnection(jarFile.getName(), entryName);
                }
            }
        }
        return new JarUrlConnection(url);
    }
    
    private static boolean hasEntry(final JarFile jarFile, final String name) {
        boolean hasEntry;
        if (jarFile instanceof final NestedJarFile nestedJarFile) {
            hasEntry = nestedJarFile.hasEntry(name);
        }
        else {
            hasEntry = (jarFile.getEntry(name) != null);
        }
        return hasEntry;
    }
    
    private static JarUrlConnection notFoundConnection(final String jarFileName, final String entryName) throws IOException {
        if (Optimizations.isEnabled()) {
            return JarUrlConnection.NOT_FOUND_CONNECTION;
        }
        return new JarUrlConnection(() -> new FileNotFoundException("JAR entry " + entryName + " not found in " + jarFileName));
    }
    
    static void clearCache() {
        JarUrlConnection.jarFiles.clearCache();
    }
    
    static {
        jarFiles = new UrlJarFiles();
        emptyInputStream = new ByteArrayInputStream(new byte[0]);
        FILE_NOT_FOUND_EXCEPTION = new FileNotFoundException("Jar file or entry not found");
        try {
            NOT_FOUND_URL = new URL("jar:", null, 0, "nested:!/", new EmptyUrlStreamHandler());
            NOT_FOUND_CONNECTION = new JarUrlConnection(() -> JarUrlConnection.FILE_NOT_FOUND_EXCEPTION);
        }
        catch (final IOException ex) {
            throw new IllegalStateException(ex);
        }
    }
    
    class ConnectionInputStream extends LazyDelegatingInputStream
    {
        @Override
        public void close() throws IOException {
            try {
                super.close();
            }
            finally {
                if (!JarUrlConnection.this.getUseCaches()) {
                    JarUrlConnection.this.jarFile.close();
                }
            }
        }
        
        @Override
        protected InputStream getDelegateInputStream() throws IOException {
            return JarUrlConnection.this.jarFile.getInputStream(JarUrlConnection.this.jarEntry);
        }
    }
    
    private static final class EmptyUrlStreamHandler extends URLStreamHandler
    {
        @Override
        protected URLConnection openConnection(final URL url) {
            return null;
        }
    }
}
