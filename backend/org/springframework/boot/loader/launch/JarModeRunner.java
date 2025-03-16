// 
// Decompiled by Procyon v0.6.0
// 

package org.springframework.boot.loader.launch;

import java.util.Iterator;
import java.util.List;
import org.springframework.core.io.support.SpringFactoriesLoader;
import org.springframework.util.ClassUtils;
import org.springframework.boot.loader.jarmode.JarMode;

final class JarModeRunner
{
    static final String DISABLE_SYSTEM_EXIT;
    
    private JarModeRunner() {
    }
    
    static void main(final String[] args) {
        final String mode = System.getProperty("jarmode");
        final List<JarMode> candidates = SpringFactoriesLoader.loadFactories((Class)JarMode.class, ClassUtils.getDefaultClassLoader());
        for (final JarMode candidate : candidates) {
            if (candidate.accepts(mode)) {
                candidate.run(mode, args);
                return;
            }
        }
        System.err.println("Unsupported jarmode '" + mode);
        if (!Boolean.getBoolean(JarModeRunner.DISABLE_SYSTEM_EXIT)) {
            System.exit(1);
        }
    }
    
    static {
        DISABLE_SYSTEM_EXIT = JarModeRunner.class.getName() + ".DISABLE_SYSTEM_EXIT";
    }
}
