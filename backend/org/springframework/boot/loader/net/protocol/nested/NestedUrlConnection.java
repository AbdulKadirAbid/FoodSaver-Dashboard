// 
// Decompiled by Procyon v0.6.0
// 

package org.springframework.boot.loader.net.protocol.nested;

import java.io.UncheckedIOException;
import java.io.FilterInputStream;
import java.time.ZoneId;
import java.io.InputStream;
import java.io.File;
import java.security.Permission;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.time.temporal.TemporalAccessor;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.io.FilePermission;
import java.lang.ref.Cleaner;
import java.time.format.DateTimeFormatter;
import java.net.URLConnection;

class NestedUrlConnection extends URLConnection
{
    private static final DateTimeFormatter RFC_1123_DATE_TIME;
    private static final String CONTENT_TYPE = "x-java/jar";
    private final NestedUrlConnectionResources resources;
    private final Cleaner.Cleanable cleanup;
    private long lastModified;
    private FilePermission permission;
    private Map<String, List<String>> headerFields;
    
    NestedUrlConnection(final URL url) throws MalformedURLException {
        this(url, org.springframework.boot.loader.ref.Cleaner.instance);
    }
    
    NestedUrlConnection(final URL url, final org.springframework.boot.loader.ref.Cleaner cleaner) throws MalformedURLException {
        super(url);
        this.lastModified = -1L;
        final NestedLocation location = this.parseNestedLocation(url);
        this.resources = new NestedUrlConnectionResources(location);
        this.cleanup = cleaner.register(this, this.resources);
    }
    
    private NestedLocation parseNestedLocation(final URL url) throws MalformedURLException {
        try {
            return NestedLocation.fromUrl(url);
        }
        catch (final IllegalArgumentException ex) {
            throw new MalformedURLException(ex.getMessage());
        }
    }
    
    @Override
    public String getHeaderField(final String name) {
        final List<String> values = this.getHeaderFields().get(name);
        return (values != null && !values.isEmpty()) ? values.get(0) : null;
    }
    
    @Override
    public String getHeaderField(final int n) {
        final Map.Entry<String, List<String>> entry = this.getHeaderEntry(n);
        final List<String> values = (entry != null) ? entry.getValue() : null;
        return (values != null && !values.isEmpty()) ? values.get(0) : null;
    }
    
    @Override
    public String getHeaderFieldKey(final int n) {
        final Map.Entry<String, List<String>> entry = this.getHeaderEntry(n);
        return (entry != null) ? entry.getKey() : null;
    }
    
    private Map.Entry<String, List<String>> getHeaderEntry(final int n) {
        final Iterator<Map.Entry<String, List<String>>> iterator = this.getHeaderFields().entrySet().iterator();
        Map.Entry<String, List<String>> entry = null;
        for (int i = 0; i < n; ++i) {
            entry = (iterator.hasNext() ? iterator.next() : null);
        }
        return entry;
    }
    
    @Override
    public Map<String, List<String>> getHeaderFields() {
        try {
            this.connect();
        }
        catch (final IOException ex) {
            return Collections.emptyMap();
        }
        Map<String, List<String>> headerFields = this.headerFields;
        if (headerFields == null) {
            headerFields = new LinkedHashMap<String, List<String>>();
            final long contentLength = this.getContentLengthLong();
            final long lastModified = this.getLastModified();
            if (contentLength > 0L) {
                headerFields.put("content-length", List.of(String.valueOf(contentLength)));
            }
            if (this.getLastModified() > 0L) {
                headerFields.put("last-modified", List.of(NestedUrlConnection.RFC_1123_DATE_TIME.format(Instant.ofEpochMilli(lastModified))));
            }
            headerFields = Collections.unmodifiableMap((Map<? extends String, ? extends List<String>>)headerFields);
            this.headerFields = headerFields;
        }
        return headerFields;
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
            return this.resources.getContentLength();
        }
        catch (final IOException ex) {
            return -1L;
        }
    }
    
    @Override
    public String getContentType() {
        return "x-java/jar";
    }
    
    @Override
    public long getLastModified() {
        if (this.lastModified == -1L) {
            try {
                this.lastModified = Files.getLastModifiedTime(this.resources.getLocation().path(), new LinkOption[0]).toMillis();
            }
            catch (final IOException ex) {
                this.lastModified = 0L;
            }
        }
        return this.lastModified;
    }
    
    @Override
    public Permission getPermission() throws IOException {
        if (this.permission == null) {
            final File file = this.resources.getLocation().path().toFile();
            this.permission = new FilePermission(file.getCanonicalPath(), "read");
        }
        return this.permission;
    }
    
    @Override
    public InputStream getInputStream() throws IOException {
        this.connect();
        return new ConnectionInputStream(this.resources.getInputStream());
    }
    
    @Override
    public void connect() throws IOException {
        if (this.connected) {
            return;
        }
        this.resources.connect();
        this.connected = true;
    }
    
    static {
        RFC_1123_DATE_TIME = DateTimeFormatter.RFC_1123_DATE_TIME.withZone(ZoneId.of("GMT"));
    }
    
    class ConnectionInputStream extends FilterInputStream
    {
        private volatile boolean closing;
        
        ConnectionInputStream(final InputStream in) {
            super(in);
        }
        
        @Override
        public void close() throws IOException {
            if (this.closing) {
                return;
            }
            this.closing = true;
            try {
                super.close();
            }
            finally {
                try {
                    NestedUrlConnection.this.cleanup.clean();
                }
                catch (final UncheckedIOException ex) {
                    throw ex.getCause();
                }
            }
        }
    }
}
