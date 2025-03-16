// 
// Decompiled by Procyon v0.6.0
// 

package com.example.zerohunger.DTO;

public class LoginRequest
{
    private final String email;
    private final String password;
    
    public LoginRequest(final String email, final String password) {
        this.email = email;
        this.password = password;
    }
    
    public String getEmail() {
        return this.email;
    }
    
    public String getPassword() {
        return this.password;
    }
}
