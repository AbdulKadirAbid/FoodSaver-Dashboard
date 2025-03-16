// 
// Decompiled by Procyon v0.6.0
// 

package org.springframework.boot.loader.ref;

public interface Cleaner
{
    public static final Cleaner instance = DefaultCleaner.instance;
    
    java.lang.ref.Cleaner.Cleanable register(final Object obj, final Runnable action);
}
