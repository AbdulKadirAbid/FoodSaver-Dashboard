// 
// Decompiled by Procyon v0.6.0
// 

package org.springframework.boot.loader.net.protocol.jar;

final class Optimizations
{
    private static final ThreadLocal<Boolean> status;
    
    private Optimizations() {
    }
    
    static void enable(final boolean readContents) {
        Optimizations.status.set(readContents);
    }
    
    static void disable() {
        Optimizations.status.remove();
    }
    
    static boolean isEnabled() {
        return Optimizations.status.get() != null;
    }
    
    static boolean isEnabled(final boolean readContents) {
        return Boolean.valueOf(readContents).equals(Optimizations.status.get());
    }
    
    static {
        status = new ThreadLocal<Boolean>();
    }
}
