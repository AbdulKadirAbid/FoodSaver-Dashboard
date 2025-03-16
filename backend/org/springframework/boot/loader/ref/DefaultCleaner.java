// 
// Decompiled by Procyon v0.6.0
// 

package org.springframework.boot.loader.ref;

import java.util.function.BiConsumer;

class DefaultCleaner implements Cleaner
{
    static final DefaultCleaner instance;
    static BiConsumer<Object, java.lang.ref.Cleaner.Cleanable> tracker;
    private final java.lang.ref.Cleaner cleaner;
    
    DefaultCleaner() {
        this.cleaner = java.lang.ref.Cleaner.create();
    }
    
    @Override
    public java.lang.ref.Cleaner.Cleanable register(final Object obj, final Runnable action) {
        final java.lang.ref.Cleaner.Cleanable cleanable = (action != null) ? this.cleaner.register(obj, action) : null;
        if (DefaultCleaner.tracker != null) {
            DefaultCleaner.tracker.accept(obj, cleanable);
        }
        return cleanable;
    }
    
    static {
        instance = new DefaultCleaner();
    }
}
