// 
// Decompiled by Procyon v0.6.0
// 

package com.example.zerohunger.Controller;

import com.example.zerohunger.DTO.SessionTokens;
import com.example.zerohunger.DTO.LoginResponse;
import jakarta.servlet.http.Cookie;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.HttpStatus;
import jakarta.servlet.http.HttpServletResponse;
import com.example.zerohunger.DTO.LoginRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import com.example.zerohunger.Entity.Users;
import org.springframework.beans.factory.annotation.Autowired;
import com.example.zerohunger.Service.SessionService;
import com.example.zerohunger.Service.UsersService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({ "/auth" })
public class AuthController
{
    private final UsersService usersService;
    private final SessionService sessionService;
    
    @Autowired
    public AuthController(final UsersService usersService, final SessionService sessionService) {
        this.usersService = usersService;
        this.sessionService = sessionService;
    }
    
    @PostMapping({ "/signup" })
    public ResponseEntity<?> signUp(@RequestBody final Users newUser) {
        this.usersService.addUser(newUser);
        return (ResponseEntity<?>)ResponseEntity.ok((Object)"Success");
    }
    
    @PostMapping({ "/signin" })
    public ResponseEntity<?> signIn(@RequestBody final LoginRequest request, final HttpServletResponse response) {
        final LoginResponse loginResponse = this.usersService.VerifyUser(request);
        if (!loginResponse.getResponse()) {
            return (ResponseEntity<?>)ResponseEntity.status((HttpStatusCode)HttpStatus.UNAUTHORIZED).body((Object)"Invalid email or password");
        }
        final SessionTokens tokens = this.sessionService.CreateSession(loginResponse.getUser());
        final Cookie accessTokenCookie = new Cookie("accessToken", tokens.getAccessToken());
        accessTokenCookie.setHttpOnly(true);
        accessTokenCookie.setPath("/");
        accessTokenCookie.setMaxAge(3600);
        final Cookie refreshTokenCookie = new Cookie("refreshToken", tokens.getRefreshToken());
        refreshTokenCookie.setHttpOnly(true);
        refreshTokenCookie.setPath("/");
        refreshTokenCookie.setMaxAge(604800);
        response.addCookie(accessTokenCookie);
        response.addCookie(refreshTokenCookie);
        return (ResponseEntity<?>)ResponseEntity.ok((Object)"User signed in successfully");
    }
}
