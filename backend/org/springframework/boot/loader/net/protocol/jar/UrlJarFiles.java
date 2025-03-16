// 
// Decompiled by Procyon v0.6.0
// 

package org.springframework.boot.loader.net.protocol.jar;

import java.util.HashMap;
import java.util.Map;
import java.net.URLConnection;
import java.io.IOException;
import java.util.jar.JarFile;
import java.net.URL;

class UrlJarFiles
{
    private final UrlJarFileFactory factory;
    private final Cache cache;
    
    UrlJarFiles() {
        this(new UrlJarFileFactory());
    }
    
    UrlJarFiles(final UrlJarFileFactory factory) {
        this.cache = new Cache();
        this.factory = factory;
    }
    
    JarFile getOrCreate(final boolean useCaches, final URL jarFileUrl) throws IOException {
        if (useCaches) {
            final JarFile cached = this.getCached(jarFileUrl);
            if (cached != null) {
                return cached;
            }
        }
        return this.factory.createJarFile(jarFileUrl, this::onClose);
    }
    
    JarFile getCached(final URL jarFileUrl) {
        return this.cache.get(jarFileUrl);
    }
    
    boolean cacheIfAbsent(final boolean useCaches, final URL jarFileUrl, final JarFile jarFile) {
        return useCaches && this.cache.putIfAbsent(jarFileUrl, jarFile);
    }
    
    void closeIfNotCached(final URL jarFileUrl, final JarFile jarFile) throws IOException {
        final JarFile cached = this.getCached(jarFileUrl);
        if (cached != jarFile) {
            jarFile.close();
        }
    }
    
    URLConnection reconnect(final JarFile jarFile, final URLConnection existingConnection) throws IOException {
        final Boolean useCaches = (existingConnection != null) ? Boolean.valueOf(existingConnection.getUseCaches()) : null;
        final URLConnection connection = this.openConnection(jarFile);
        if (useCaches != null && connection != null) {
            connection.setUseCaches(useCaches);
        }
        return connection;
    }
    
    private URLConnection openConnection(final JarFile jarFile) throws IOException {
        final URL url = this.cache.get(jarFile);
        return (url != null) ? url.openConnection() : null;
    }
    
    private void onClose(final JarFile jarFile) {
        this.cache.remove(jarFile);
    }
    
    void clearCache() {
        this.cache.clear();
    }
    
    private static final class Cache
    {
        private final Map<String, JarFile> jarFileUrlToJarFile;
        private final Map<JarFile, URL> jarFileToJarFileUrl;
        
        private Cache() {
            this.jarFileUrlToJarFile = new HashMap<String, JarFile>();
            this.jarFileToJarFileUrl = new HashMap<JarFile, URL>();
        }
        
        JarFile get(final URL jarFileUrl) {
            final String urlKey = JarFileUrlKey.get(jarFileUrl);
            synchronized (this) {
                return this.jarFileUrlToJarFile.get(urlKey);
            }
        }
        
        URL get(final JarFile jarFile) {
            synchronized (this) {
                return this.jarFileToJarFileUrl.get(jarFile);
            }
        }
        
        boolean putIfAbsent(final URL jarFileUrl, final JarFile jarFile) {
            final String urlKey = JarFileUrlKey.get(jarFileUrl);
            synchronized (this) {
                final JarFile cached = this.jarFileUrlToJarFile.get(urlKey);
                if (cached == null) {
                    this.jarFileUrlToJarFile.put(urlKey, jarFile);
                    this.jarFileToJarFileUrl.put(jarFile, jarFileUrl);
                    return true;
                }
                return false;
            }
        }
        
        void remove(final JarFile jarFile) {
            synchronized (this) {
                final URL removedUrl = this.jarFileToJarFileUrl.remove(jarFile);
                if (removedUrl != null) {
                    this.jarFileUrlToJarFile.remove(JarFileUrlKey.get(removedUrl));
                }
            }
        }
        
        void clear() {
            synchronized (this) {
                this.jarFileToJarFileUrl.clear();
                this.jarFileUrlToJarFile.clear();
            }
        }
    }
}
