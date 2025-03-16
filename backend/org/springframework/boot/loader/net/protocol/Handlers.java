// 
// Decompiled by Procyon v0.6.0
// 

package org.springframework.boot.loader.net.protocol;

import java.net.URLStreamHandlerFactory;
import java.net.URL;

public final class Handlers
{
    private static final String PROTOCOL_HANDLER_PACKAGES = "java.protocol.handler.pkgs";
    private static final String PACKAGE;
    
    private Handlers() {
    }
    
    public static void register() {
        String packages = System.getProperty("java.protocol.handler.pkgs", "");
        packages = ((!packages.isEmpty() && !packages.contains(Handlers.PACKAGE)) ? (packages + "|" + Handlers.PACKAGE) : Handlers.PACKAGE);
        System.setProperty("java.protocol.handler.pkgs", packages);
        resetCachedUrlHandlers();
    }
    
    private static void resetCachedUrlHandlers() {
        try {
            URL.setURLStreamHandlerFactory(null);
        }
        catch (final Error error) {}
    }
    
    static {
        PACKAGE = Handlers.class.getPackageName();
    }
}
