// 
// Decompiled by Procyon v0.6.0
// 

package com.example.zerohunger;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableScheduling
@EntityScan(basePackages = { "com.example.zerohunger.Entity" })
public class ZerohungerApplication
{
    public static void main(final String[] args) {
        SpringApplication.run((Class)ZerohungerApplication.class, args);
    }
}
