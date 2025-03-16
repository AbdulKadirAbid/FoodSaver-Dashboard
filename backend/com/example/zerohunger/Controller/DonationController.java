// 
// Decompiled by Procyon v0.6.0
// 

package com.example.zerohunger.Controller;

import org.springframework.web.bind.annotation.GetMapping;
import com.example.zerohunger.DTO.UserStatsDTO;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.beans.factory.annotation.Autowired;
import com.example.zerohunger.Service.StatsService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({ "/api/donations" })
public class DonationController
{
    @Autowired
    private StatsService statsService;
    
    @GetMapping({ "/total/{userId}" })
    public UserStatsDTO getTotalDonations(@PathVariable final Long userId) {
        return this.statsService.getUserStats(userId);
    }
}
