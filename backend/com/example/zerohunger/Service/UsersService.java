// 
// Decompiled by Procyon v0.6.0
// 

package com.example.zerohunger.Service;

import org.springframework.scheduling.annotation.Scheduled;
import java.util.Iterator;
import java.util.List;
import com.example.zerohunger.DTO.LoginResponse;
import com.example.zerohunger.DTO.LoginRequest;
import com.example.zerohunger.Entity.userStats;
import com.example.zerohunger.Utility.PasswordUtil;
import com.example.zerohunger.Entity.Users;
import org.springframework.beans.factory.annotation.Autowired;
import com.example.zerohunger.Repository.UsersRepo;
import org.springframework.stereotype.Service;

@Service
public class UsersService
{
    private final FoodService foodService;
    private final StatsService statsService;
    private final UsersRepo usersRepo;
    
    @Autowired
    public UsersService(final FoodService foodService, final StatsService statsService, final UsersRepo usersRepo) {
        this.foodService = foodService;
        this.statsService = statsService;
        this.usersRepo = usersRepo;
    }
    
    public boolean addUser(final Users user) {
        try {
            user.setPassword(PasswordUtil.hashPassword(user.getPassword()));
            final userStats stats = new userStats();
            stats.setUserID(user);
            stats.setTotalSaved(0);
            stats.setTotalWasted(0);
            stats.setWastedLastMonth(0);
            stats.setWastedLastWeek(0);
            user.setStats(stats);
            this.usersRepo.save((Object)user);
            return true;
        }
        catch (final Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    
    public LoginResponse VerifyUser(final LoginRequest request) {
        final String email = request.getEmail();
        final String password = request.getPassword();
        final Users user = this.usersRepo.findByEmail(email);
        if (user == null) {
            return new LoginResponse(false);
        }
        final LoginResponse response = new LoginResponse(PasswordUtil.verifyPassword(password, user.getPassword()), user);
        return response;
    }
    
    @Scheduled(cron = "0 0 0 * * *")
    public void midnightStatRecount() {
        final List<Long> Ids = this.usersRepo.listUserIDs();
        this.foodService.removeOldFood();
        for (final Long x : Ids) {
            this.foodService.CountFoodSinceDate(x);
        }
    }
}
