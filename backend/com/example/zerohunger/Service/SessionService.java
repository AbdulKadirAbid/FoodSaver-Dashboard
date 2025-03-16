// 
// Decompiled by Procyon v0.6.0
// 

package com.example.zerohunger.Service;

import com.example.zerohunger.Entity.RefreshToken;
import java.time.LocalDateTime;
import com.example.zerohunger.Entity.Sessions;
import com.example.zerohunger.DTO.SessionTokens;
import com.example.zerohunger.Entity.Users;
import org.springframework.beans.factory.annotation.Autowired;
import com.example.zerohunger.Utility.TokenUtil;
import com.example.zerohunger.Repository.RefreshTokenRepo;
import com.example.zerohunger.Repository.SessionRepo;
import org.springframework.stereotype.Service;

@Service
public class SessionService
{
    private SessionRepo seshRepo;
    private RefreshTokenRepo refreshRepo;
    private TokenUtil tokenUtil;
    
    @Autowired
    public SessionService(final SessionRepo seshRepo, final RefreshTokenRepo refreshRepo, final TokenUtil tokenUtil) {
        this.seshRepo = seshRepo;
        this.refreshRepo = refreshRepo;
        this.tokenUtil = tokenUtil;
    }
    
    public SessionTokens CreateSession(final Users userID) {
        final Sessions session = new Sessions();
        session.setUserID(userID);
        session.setExpAt(LocalDateTime.now().plusHours(1L));
        this.seshRepo.save((Object)session);
        final String accessToken = this.tokenUtil.generateAccessToken(userID.getUserID());
        final String refreshToken = this.tokenUtil.generateRefreshToken(userID.getUserID());
        final SessionTokens tokens = new SessionTokens(accessToken, refreshToken);
        final RefreshToken refresh = new RefreshToken();
        refresh.setExpAt(LocalDateTime.now().plusWeeks(1L));
        refresh.setToken(refreshToken);
        refresh.setUserID(userID);
        this.refreshRepo.save((Object)refresh);
        return tokens;
    }
    
    public Boolean CleanUpSession(final Users userID) {
        this.refreshRepo.deleteByUserID(userID);
        this.seshRepo.deleteByUserID(userID);
        return true;
    }
}
