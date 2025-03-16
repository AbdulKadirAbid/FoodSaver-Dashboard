// 
// Decompiled by Procyon v0.6.0
// 

package com.example.zerohunger.Utility;

import io.jsonwebtoken.Claims;
import java.security.Key;
import java.util.Date;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.SignatureAlgorithm;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Component;

@Component
public class TokenUtil
{
    private final SecretKey key;
    
    public TokenUtil() {
        this.key = Keys.secretKeyFor(SignatureAlgorithm.HS256);
    }
    
    public String generateAccessToken(final Long userID) {
        return Jwts.builder().setSubject(userID.toString()).setIssuedAt(new Date()).setExpiration(new Date(System.currentTimeMillis() + 3600000L)).signWith((Key)this.key).compact();
    }
    
    public String generateRefreshToken(final Long userID) {
        return Jwts.builder().setSubject(userID.toString()).setIssuedAt(new Date()).setExpiration(new Date(System.currentTimeMillis() + 604800000L)).signWith((Key)this.key).compact();
    }
    
    public boolean validateToken(final String token) {
        try {
            Jwts.parserBuilder().setSigningKey((Key)this.key).build().parseClaimsJws(token);
            return true;
        }
        catch (final Exception e) {
            return false;
        }
    }
    
    public Long extractUserId(final String token) {
        return Long.parseLong(((Claims)Jwts.parserBuilder().setSigningKey((Key)this.key).build().parseClaimsJws(token).getBody()).getSubject());
    }
}
