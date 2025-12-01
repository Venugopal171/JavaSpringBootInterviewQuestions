package com.JavaInterviewQuestions.JavaInterviewQuestions.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf
                .ignoringRequestMatchers("/payment/**", "/webhook/**", "/h2-console/**") // Allow all payment endpoints & webhooks
            )
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/", 
                    "/register", 
                    "/payment/**", 
                    "/webhook/**",
                    "/success", 
                    "/css/**", 
                    "/js/**", 
                    "/images/**",
                    "/h2-console/**"
                ).permitAll()
                .anyRequest().authenticated()
            )
            .headers(headers -> headers
                .frameOptions(frame -> frame.disable()) // Disable for H2 console iframes
            );
        
        return http.build();
    }
}

