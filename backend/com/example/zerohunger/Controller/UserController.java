// 
// Decompiled by Procyon v0.6.0
// 

package com.example.zerohunger.Controller;

import org.springframework.web.bind.annotation.GetMapping;
import com.example.zerohunger.DTO.UserStatsDTO;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.beans.factory.annotation.Autowired;
import com.example.zerohunger.Utility.TokenUtil;
import com.example.zerohunger.Service.StatsService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({ "/user" })
public class UserController
{
    private final StatsService statsService;
    private final TokenUtil tokenUtil;
    
    @Autowired
    public UserController(final StatsService statsService, final TokenUtil tokenUtil) {
        this.statsService = statsService;
        this.tokenUtil = tokenUtil;
    }
    
    @GetMapping({ "/stats" })
    public ResponseEntity<?> getUserStats(@RequestHeader("Authorization") final String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return (ResponseEntity<?>)ResponseEntity.status((HttpStatusCode)HttpStatus.UNAUTHORIZED).body((Object)"Missing or invalid token");
        }
        final String token = authHeader.substring(7);
        if (!this.tokenUtil.validateToken(token)) {
            return (ResponseEntity<?>)ResponseEntity.status((HttpStatusCode)HttpStatus.UNAUTHORIZED).body((Object)"Invalid or expired token");
        }
        final Long userId = this.tokenUtil.extractUserId(token);
        final UserStatsDTO stats = this.statsService.getUserStats(userId);
        return (ResponseEntity<?>)ResponseEntity.ok((Object)stats);
    }
}
