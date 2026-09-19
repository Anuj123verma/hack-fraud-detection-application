package com.meridiantrust.sentinel.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.Customizer;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

/**
 * RBAC enforced server-side via method security (@PreAuthorize on
 * controllers), not left to the frontend. Two roles for the hackathon demo:
 * ANALYST (work the queue) and COMPLIANCE_ADMIN (everything ANALYST can do,
 * plus unmasked PII on alert detail). Credentials come from env vars, never
 * hardcoded - the values below are only fallback defaults for local demo use.
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public InMemoryUserDetailsManager userDetailsService(
            PasswordEncoder encoder,
            @Value("${sentinel.security.analyst-password:${ANALYST_PASSWORD:analyst123}}") String analystPassword,
            @Value("${sentinel.security.admin-password:${COMPLIANCE_ADMIN_PASSWORD:admin123}}") String adminPassword) {

        UserDetails analyst = User.withUsername("analyst")
                .password(encoder.encode(analystPassword))
                .roles("ANALYST")
                .build();

        UserDetails complianceAdmin = User.withUsername("compliance_admin")
                .password(encoder.encode(adminPassword))
                .roles("ANALYST", "COMPLIANCE_ADMIN")
                .build();

        return new InMemoryUserDetailsManager(analyst, complianceAdmin);
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/h2-console/**").permitAll()
                        .requestMatchers("/", "/index.html", "/js/**", "/css/**", "/favicon.ico").permitAll()
                        .anyRequest().authenticated())
                .httpBasic(Customizer.withDefaults());
        return http.build();
    }
}
