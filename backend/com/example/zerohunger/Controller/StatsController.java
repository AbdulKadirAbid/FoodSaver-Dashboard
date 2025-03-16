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
@RequestMapping({ "/api/stats" })
public class StatsController
{
    @Autowired
    private StatsService statsService;
    
    @GetMapping({ "/food/summary/{userId}" })
    public UserStatsDTO getFoodSummary(@PathVariable final Long userId) {
        return this.statsService.getUserStats(userId);
    }
    
    @GetMapping({ "/food/wasted/last-month/{userId}" })
    public int getFoodWastedLastMonth(@PathVariable final Long userId) {
        return this.statsService.getFoodWastedLastMonth(userId);
    }
    
    @GetMapping({ "/food/wasted/last-week/{userId}" })
    public int getFoodWastedLastWeek(@PathVariable final Long userId) {
        return this.statsService.getFoodWastedLastWeek(userId);
    }
    
    @GetMapping({ "/donations/summary/{userId}" })
    public UserStatsDTO getDonationSummary(@PathVariable final Long userId) {
        return this.statsService.getDonationStats(userId);
    }
    
    @GetMapping({ "/donations/last-month/{userId}" })
    public int getDonationsLastMonth(@PathVariable final Long userId) {
        return this.statsService.getDonationsLastMonth(userId);
    }
}
