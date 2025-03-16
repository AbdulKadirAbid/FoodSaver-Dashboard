// 
// Decompiled by Procyon v0.6.0
// 

package com.example.zerohunger.Utility;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class PasswordUtil
{
    private static final BCryptPasswordEncoder hash;
    
    public static String hashPassword(final String password) {
        return PasswordUtil.hash.encode((CharSequence)password);
    }
    
    public static boolean verifyPassword(final String plainPassword, final String hashedPassword) {
        return PasswordUtil.hash.matches((CharSequence)plainPassword, hashedPassword);
    }
    
    static {
        PasswordUtil.hash = new BCryptPasswordEncoder();
    }
}
