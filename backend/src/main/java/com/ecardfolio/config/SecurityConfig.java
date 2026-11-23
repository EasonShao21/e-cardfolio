package com.ecardfolio.config;

import com.ecardfolio.account.UserAccountRepository;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import java.util.List;

// [AI assisted chat-015] 伺服器 session、BCrypt 密碼與 CSRF 保護；名片管理 API 要求登入。
@Configuration
public class SecurityConfig {
    @Bean PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }
    @Bean UserDetailsService userDetailsService(UserAccountRepository accounts) {
        return email -> accounts.findByEmail(email)
            .map(account -> User.withUsername(account.getEmail()).password(account.getPasswordHash()).authorities("USER").build())
            .orElseThrow(() -> new org.springframework.security.core.userdetails.UsernameNotFoundException("帳號或密碼錯誤"));
    }
    @Bean DaoAuthenticationProvider authenticationProvider(UserDetailsService users, PasswordEncoder encoder) {
        var provider = new DaoAuthenticationProvider(users);
        provider.setPasswordEncoder(encoder);
        return provider;
    }
    @Bean AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception { return config.getAuthenticationManager(); }
    @Bean CorsConfigurationSource corsConfigurationSource() {
        var cors = new CorsConfiguration();
        cors.setAllowedOriginPatterns(List.of("http://localhost:*", "http://127.0.0.1:*", "http://192.168.*.*:*", "http://10.*.*.*:*", "http://172.*.*:*"));
        cors.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        cors.setAllowedHeaders(List.of("Content-Type", "X-XSRF-TOKEN"));
        cors.setAllowCredentials(true);
        var source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", cors);
        return source;
    }
    @Bean SecurityFilterChain securityFilterChain(HttpSecurity http, DaoAuthenticationProvider provider) throws Exception {
        var csrfRepository = CookieCsrfTokenRepository.withHttpOnlyFalse();
        csrfRepository.setCookiePath("/");
        http.cors(cors -> {})
            .csrf(csrf -> csrf.csrfTokenRepository(csrfRepository).csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler()))
            .authenticationProvider(provider)
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/", "/index.html", "/assets/**", "/favicon.ico", "/card/**").permitAll()
                .requestMatchers("/api/auth/csrf", "/api/auth/register", "/api/auth/login", "/api/share-config", "/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/cards/*").permitAll()
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .anyRequest().authenticated())
            .exceptionHandling(errors -> errors.authenticationEntryPoint((request, response, exception) -> response.sendError(HttpServletResponse.SC_UNAUTHORIZED)))
            .formLogin(form -> form.disable())
            .httpBasic(basic -> basic.disable());
        return http.build();
    }
}
