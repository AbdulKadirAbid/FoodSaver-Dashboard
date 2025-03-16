// 
// Decompiled by Procyon v0.6.0
// 

package org.springframework.boot.loader.launch;

import java.lang.reflect.Constructor;
import java.util.Locale;
import java.util.function.Predicate;
import org.springframework.boot.loader.net.protocol.jar.JarUrl;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.jar.Manifest;
import java.util.Collection;
import java.util.Collections;
import java.io.FileInputStream;
import java.net.HttpURLConnection;
import java.net.URLConnection;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.io.InputStream;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.Properties;
import java.util.List;
import java.io.File;
import org.springframework.boot.loader.log.DebugLogger;
import java.util.regex.Pattern;
import java.net.URL;

public class PropertiesLauncher extends Launcher
{
    public static final String MAIN = "loader.main";
    public static final String PATH = "loader.path";
    public static final String HOME = "loader.home";
    public static final String ARGS = "loader.args";
    public static final String CONFIG_NAME = "loader.config.name";
    public static final String CONFIG_LOCATION = "loader.config.location";
    public static final String SET_SYSTEM_PROPERTIES = "loader.system";
    private static final URL[] NO_URLS;
    private static final Pattern WORD_SEPARATOR;
    private static final String NESTED_ARCHIVE_SEPARATOR;
    private static final String JAR_FILE_PREFIX = "jar:file:";
    private static final DebugLogger debug;
    private final Archive archive;
    private final File homeDirectory;
    private final List<String> paths;
    private final Properties properties;
    
    public PropertiesLauncher() throws Exception {
        this(Archive.create(Launcher.class));
    }
    
    PropertiesLauncher(final Archive archive) throws Exception {
        this.properties = new Properties();
        this.archive = archive;
        this.homeDirectory = this.getHomeDirectory();
        this.initializeProperties();
        this.paths = this.getPaths();
        this.classPathIndex = this.getClassPathIndex(this.archive);
    }
    
    protected File getHomeDirectory() throws Exception {
        return new File(this.getPropertyWithDefault("loader.home", "${user.dir}"));
    }
    
    private void initializeProperties() throws Exception {
        final List<String> configs = new ArrayList<String>();
        if (this.getProperty("loader.config.location") != null) {
            configs.add(this.getProperty("loader.config.location"));
        }
        else {
            final String[] split;
            final String[] names = split = this.getPropertyWithDefault("loader.config.name", "loader").split(",");
            for (int length = split.length, i = 0; i < length; ++i) {
                final String name = split[i];
                final String propertiesFile = name + ".properties";
                configs.add("file:" + this.homeDirectory + "/" + propertiesFile);
                configs.add("classpath:" + propertiesFile);
                configs.add("classpath:BOOT-INF/classes/" + propertiesFile);
            }
        }
        for (final String config : configs) {
            final InputStream resource = this.getResource(config);
            try {
                if (resource != null) {
                    PropertiesLauncher.debug.log("Found: %s", config);
                    this.loadResource(resource);
                    if (resource != null) {
                        resource.close();
                    }
                    return;
                }
                PropertiesLauncher.debug.log("Not found: %s", config);
                if (resource != null) {
                    resource.close();
                    continue;
                }
                continue;
            }
            catch (final Throwable t) {
                if (resource != null) {
                    try {
                        resource.close();
                    }
                    catch (final Throwable exception) {
                        t.addSuppressed(exception);
                    }
                }
                throw t;
            }
            break;
        }
    }
    
    private InputStream getResource(String config) throws Exception {
        if (config.startsWith("classpath:")) {
            return this.getClasspathResource(config.substring("classpath:".length()));
        }
        config = this.handleUrl(config);
        if (this.isUrl(config)) {
            return this.getURLResource(config);
        }
        return this.getFileResource(config);
    }
    
    private InputStream getClasspathResource(String config) {
        config = this.stripLeadingSlashes(config);
        config = "/" + config;
        PropertiesLauncher.debug.log("Trying classpath: %s", config);
        return this.getClass().getResourceAsStream(config);
    }
    
    private String handleUrl(String path) {
        if (path.startsWith("jar:file:") || path.startsWith("file:")) {
            path = URLDecoder.decode(path, StandardCharsets.UTF_8);
            if (path.startsWith("file:")) {
                path = path.substring("file:".length());
                if (path.startsWith("//")) {
                    path = path.substring(2);
                }
            }
        }
        return path;
    }
    
    private boolean isUrl(final String config) {
        return config.contains("://");
    }
    
    private InputStream getURLResource(final String config) throws Exception {
        final URL url = new URL(config);
        if (this.exists(url)) {
            final URLConnection connection = url.openConnection();
            try {
                return connection.getInputStream();
            }
            catch (final IOException ex) {
                this.disconnect(connection);
                throw ex;
            }
        }
        return null;
    }
    
    private boolean exists(final URL url) throws IOException {
        final URLConnection connection = url.openConnection();
        try {
            connection.setUseCaches(connection.getClass().getSimpleName().startsWith("JNLP"));
            if (connection instanceof final HttpURLConnection httpConnection) {
                httpConnection.setRequestMethod("HEAD");
                final int responseCode = httpConnection.getResponseCode();
                if (responseCode == 200) {
                    return true;
                }
                if (responseCode == 404) {
                    return false;
                }
            }
            return connection.getContentLength() >= 0;
        }
        finally {
            this.disconnect(connection);
        }
    }
    
    private void disconnect(final URLConnection connection) {
        if (connection instanceof final HttpURLConnection httpConnection) {
            httpConnection.disconnect();
        }
    }
    
    private InputStream getFileResource(final String config) throws Exception {
        final File file = new File(config);
        PropertiesLauncher.debug.log("Trying file: %s", config);
        return file.canRead() ? new FileInputStream(file) : null;
    }
    
    private void loadResource(final InputStream resource) throws Exception {
        this.properties.load(resource);
        this.resolvePropertyPlaceholders();
        if ("true".equalsIgnoreCase(this.getProperty("loader.system"))) {
            this.addToSystemProperties();
        }
    }
    
    private void resolvePropertyPlaceholders() {
        for (final String name : this.properties.stringPropertyNames()) {
            final String value = this.properties.getProperty(name);
            final String resolved = SystemPropertyUtils.resolvePlaceholders(this.properties, value);
            if (resolved != null) {
                this.properties.put(name, resolved);
            }
        }
    }
    
    private void addToSystemProperties() {
        PropertiesLauncher.debug.log("Adding resolved properties to System properties");
        for (final String name : this.properties.stringPropertyNames()) {
            final String value = this.properties.getProperty(name);
            System.setProperty(name, value);
        }
    }
    
    private List<String> getPaths() throws Exception {
        final String path = this.getProperty("loader.path");
        final List<String> paths = (path != null) ? this.parsePathsProperty(path) : Collections.emptyList();
        PropertiesLauncher.debug.log("Nested archive paths: %s", this.paths);
        return paths;
    }
    
    private List<String> parsePathsProperty(final String commaSeparatedPaths) {
        final List<String> paths = new ArrayList<String>();
        for (String path : commaSeparatedPaths.split(",")) {
            path = this.cleanupPath(path);
            path = (path.isEmpty() ? "/" : path);
            paths.add(path);
        }
        if (paths.isEmpty()) {
            paths.add("lib");
        }
        return paths;
    }
    
    private String cleanupPath(String path) {
        path = path.trim();
        if (path.startsWith("./")) {
            path = path.substring(2);
        }
        if (this.isArchive(path)) {
            return path;
        }
        if (path.endsWith("/*")) {
            return path.substring(0, path.length() - 1);
        }
        return (!path.endsWith("/") && !path.equals(".")) ? path : path;
    }
    
    @Override
    protected ClassLoader createClassLoader(Collection<URL> urls) throws Exception {
        final String loaderClassName = this.getProperty("loader.classLoader");
        if (this.classPathIndex != null) {
            urls = new ArrayList<URL>(urls);
            urls.addAll(this.classPathIndex.getUrls());
        }
        if (loaderClassName == null) {
            return super.createClassLoader(urls);
        }
        final ClassLoader parent = this.getClass().getClassLoader();
        ClassLoader classLoader = new LaunchedClassLoader(false, urls.toArray(new URL[0]), parent);
        PropertiesLauncher.debug.log("Classpath for custom loader: %s", urls);
        classLoader = this.wrapWithCustomClassLoader(classLoader, loaderClassName);
        PropertiesLauncher.debug.log("Using custom class loader: %s", loaderClassName);
        return classLoader;
    }
    
    private ClassLoader wrapWithCustomClassLoader(final ClassLoader parent, final String loaderClassName) throws Exception {
        final Instantiator<ClassLoader> instantiator = new Instantiator<ClassLoader>(parent, loaderClassName);
        ClassLoader loader = instantiator.declaredConstructor(ClassLoader.class).newInstance(parent);
        loader = ((loader != null) ? loader : instantiator.declaredConstructor(URL[].class, ClassLoader.class).newInstance(PropertiesLauncher.NO_URLS, parent));
        loader = ((loader != null) ? loader : instantiator.constructWithoutParameters());
        if (loader != null) {
            return loader;
        }
        throw new IllegalStateException("Unable to create class loader for " + loaderClassName);
    }
    
    @Override
    protected Archive getArchive() {
        return null;
    }
    
    @Override
    protected String getMainClass() throws Exception {
        final String mainClass = this.getProperty("loader.main", "Start-Class");
        if (mainClass == null) {
            throw new IllegalStateException("No '%s' or 'Start-Class' specified".formatted("loader.main"));
        }
        return mainClass;
    }
    
    protected String[] getArgs(final String... args) throws Exception {
        final String loaderArgs = this.getProperty("loader.args");
        return (loaderArgs != null) ? this.merge(loaderArgs.split("\\s+"), args) : args;
    }
    
    private String[] merge(final String[] a1, final String[] a2) {
        final String[] result = new String[a1.length + a2.length];
        System.arraycopy(a1, 0, result, 0, a1.length);
        System.arraycopy(a2, 0, result, a1.length, a2.length);
        return result;
    }
    
    private String getProperty(final String name) throws Exception {
        return this.getProperty(name, null, null);
    }
    
    private String getProperty(final String name, final String manifestKey) throws Exception {
        return this.getProperty(name, manifestKey, null);
    }
    
    private String getPropertyWithDefault(final String name, final String defaultValue) throws Exception {
        return this.getProperty(name, null, defaultValue);
    }
    
    private String getProperty(final String name, String manifestKey, final String defaultValue) throws Exception {
        manifestKey = ((manifestKey != null) ? manifestKey : toCamelCase(name.replace('.', '-')));
        String value = SystemPropertyUtils.getProperty(name);
        if (value != null) {
            return this.getResolvedProperty(name, manifestKey, value, "environment");
        }
        if (this.properties.containsKey(name)) {
            value = this.properties.getProperty(name);
            return this.getResolvedProperty(name, manifestKey, value, "properties");
        }
        if (this.homeDirectory != null) {
            try (final ExplodedArchive explodedArchive = new ExplodedArchive(this.homeDirectory)) {
                value = this.getManifestValue(explodedArchive, manifestKey);
                if (value != null) {
                    final String resolvedProperty = this.getResolvedProperty(name, manifestKey, value, "home directory manifest");
                    explodedArchive.close();
                    return resolvedProperty;
                }
            }
            catch (final IllegalStateException ex) {}
        }
        value = this.getManifestValue(this.archive, manifestKey);
        if (value != null) {
            return this.getResolvedProperty(name, manifestKey, value, "manifest");
        }
        return SystemPropertyUtils.resolvePlaceholders(this.properties, defaultValue);
    }
    
    String getManifestValue(final Archive archive, final String manifestKey) throws Exception {
        final Manifest manifest = archive.getManifest();
        return (manifest != null) ? manifest.getMainAttributes().getValue(manifestKey) : null;
    }
    
    private String getResolvedProperty(final String name, final String manifestKey, String value, final String from) {
        value = SystemPropertyUtils.resolvePlaceholders(this.properties, value);
        final String altName = (manifestKey != null && !manifestKey.equals(name)) ? "[%s] ".formatted(manifestKey) : "";
        PropertiesLauncher.debug.log("Property '%s'%s from %s: %s", name, altName, from, value);
        return value;
    }
    
    void close() throws Exception {
        if (this.archive != null) {
            this.archive.close();
        }
    }
    
    public static String toCamelCase(final CharSequence string) {
        if (string == null) {
            return null;
        }
        final StringBuilder result = new StringBuilder();
        final Matcher matcher = PropertiesLauncher.WORD_SEPARATOR.matcher(string);
        int pos = 0;
        while (matcher.find()) {
            result.append(capitalize(string.subSequence(pos, matcher.end()).toString()));
            pos = matcher.end();
        }
        result.append(capitalize(string.subSequence(pos, string.length()).toString()));
        return result.toString();
    }
    
    private static String capitalize(final String str) {
        return Character.toUpperCase(str.charAt(0)) + str.substring(1);
    }
    
    @Override
    protected Set<URL> getClassPathUrls() throws Exception {
        final Set<URL> urls = new LinkedHashSet<URL>();
        for (String path : this.getPaths()) {
            path = this.cleanupPath(this.handleUrl(path));
            urls.addAll(this.getClassPathUrlsForPath(path));
        }
        urls.addAll(this.getClassPathUrlsForRoot());
        PropertiesLauncher.debug.log("Using class path URLs %s", urls);
        return urls;
    }
    
    private Set<URL> getClassPathUrlsForPath(final String path) throws Exception {
        final File file = this.isAbsolutePath(path) ? new File(path) : new File(this.homeDirectory, path);
        final Set<URL> urls = new LinkedHashSet<URL>();
        if (!"/".equals(path) && file.isDirectory()) {
            try (final ExplodedArchive explodedArchive = new ExplodedArchive(file)) {
                PropertiesLauncher.debug.log("Adding classpath entries from directory %s", file);
                urls.add(file.toURI().toURL());
                urls.addAll(explodedArchive.getClassPathUrls(this::isArchive));
            }
        }
        if (!file.getPath().contains(PropertiesLauncher.NESTED_ARCHIVE_SEPARATOR) && this.isArchive(file.getName())) {
            PropertiesLauncher.debug.log("Adding classpath entries from jar/zip archive %s", path);
            urls.add(file.toURI().toURL());
        }
        final Set<URL> nested = this.getClassPathUrlsForNested(path);
        if (!nested.isEmpty()) {
            PropertiesLauncher.debug.log("Adding classpath entries from nested %s", path);
            urls.addAll(nested);
        }
        return urls;
    }
    
    private Set<URL> getClassPathUrlsForNested(String path) throws Exception {
        final boolean isJustArchive = this.isArchive(path);
        if ((!path.equals("/") && path.startsWith("/")) || (this.archive.isExploded() && this.archive.getRootDirectory().equals(this.homeDirectory))) {
            return Collections.emptySet();
        }
        File file = null;
        if (isJustArchive) {
            final File candidate = new File(this.homeDirectory, path);
            if (candidate.exists()) {
                file = candidate;
                path = "";
            }
        }
        final int separatorIndex = path.indexOf(33);
        if (separatorIndex != -1) {
            file = (path.startsWith("jar:file:") ? new File(path.substring("jar:file:".length(), separatorIndex)) : new File(this.homeDirectory, path.substring(0, separatorIndex)));
            path = path.substring(separatorIndex + 1);
            path = this.stripLeadingSlashes(path);
        }
        if (path.equals("/") || path.equals("./") || path.equals(".")) {
            path = "";
        }
        final Archive archive = (file != null) ? new JarFileArchive(file) : this.archive;
        try {
            final Set<URL> urls = new LinkedHashSet<URL>(archive.getClassPathUrls(this.includeByPrefix(path)));
            if (!isJustArchive && file != null && path.isEmpty()) {
                urls.add(JarUrl.create(file));
            }
            return urls;
        }
        finally {
            if (archive != this.archive) {
                archive.close();
            }
        }
    }
    
    private Set<URL> getClassPathUrlsForRoot() throws Exception {
        PropertiesLauncher.debug.log("Adding classpath entries from root archive %s", this.archive);
        return this.archive.getClassPathUrls(this::isIncludedOnClassPathAndNotIndexed, Archive.ALL_ENTRIES);
    }
    
    private Predicate<Archive.Entry> includeByPrefix(final String prefix) {
        return entry -> (entry.isDirectory() && entry.name().equals(prefix)) || (this.isArchive(entry) && entry.name().startsWith(prefix));
    }
    
    private boolean isArchive(final Archive.Entry entry) {
        return this.isArchive(entry.name());
    }
    
    private boolean isArchive(String name) {
        name = name.toLowerCase(Locale.ENGLISH);
        return name.endsWith(".jar") || name.endsWith(".zip");
    }
    
    private boolean isAbsolutePath(final String root) {
        return root.contains(":") || root.startsWith("/");
    }
    
    private String stripLeadingSlashes(String string) {
        while (string.startsWith("/")) {
            string = string.substring(1);
        }
        return string;
    }
    
    public static void main(String[] args) throws Exception {
        final PropertiesLauncher launcher = new PropertiesLauncher();
        args = launcher.getArgs(args);
        launcher.launch(args);
    }
    
    static {
        NO_URLS = new URL[0];
        WORD_SEPARATOR = Pattern.compile("\\W+");
        NESTED_ARCHIVE_SEPARATOR = "!" + File.separator;
        debug = DebugLogger.get(PropertiesLauncher.class);
    }
    
    record Instantiator<T>(ClassLoader parent, Class<?> type) {
        Instantiator(final ClassLoader parent, final String className) throws ClassNotFoundException {
            this(parent, Class.forName(className, true, parent));
        }
        
        T constructWithoutParameters() throws Exception {
            return this.declaredConstructor((Class<?>[])new Class[0]).newInstance(new Object[0]);
        }
        
        Using<T> declaredConstructor(final Class<?>... parameterTypes) {
            return new Using<T>(this, parameterTypes);
        }
        
        record Using<T>(Instantiator<T> instantiator, Class<?>... parameterTypes) {
            T newInstance(final Object... initargs) throws Exception {
                try {
                    final Constructor<?> constructor = this.instantiator.type().getDeclaredConstructor(this.parameterTypes);
                    constructor.setAccessible(true);
                    return (T)constructor.newInstance(initargs);
                }
                catch (final NoSuchMethodException ex) {
                    return null;
                }
            }
        }
    }
}
