// 
// Decompiled by Procyon v0.6.0
// 

package com.example.zerohunger.Configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CorsConfig
{
    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return (WebMvcConfigurer)new CorsConfig$1(this);
    }
}
