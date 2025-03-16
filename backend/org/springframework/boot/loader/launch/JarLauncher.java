// 
// Decompiled by Procyon v0.6.0
// 

package org.springframework.boot.loader.launch;

public class JarLauncher extends ExecutableArchiveLauncher
{
    public JarLauncher() throws Exception {
    }
    
    protected JarLauncher(final Archive archive) throws Exception {
        super(archive);
    }
    
    public static void main(final String[] args) throws Exception {
        new JarLauncher().launch(args);
    }
}
