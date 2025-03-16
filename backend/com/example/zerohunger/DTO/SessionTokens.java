// 
// Decompiled by Procyon v0.6.0
// 

package com.example.zerohunger.DTO;

public class SessionTokens
{
    private final String accessToken;
    private final String refreshToken;
    
    public SessionTokens(final String accessToken, final String refreshToken) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
    }
    
    public String getAccessToken() {
        return this.accessToken;
    }
    
    public String getRefreshToken() {
        return this.refreshToken;
    }
}
