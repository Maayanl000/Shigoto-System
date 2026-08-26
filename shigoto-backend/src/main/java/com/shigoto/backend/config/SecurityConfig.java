package com.shigoto.backend.config;

import com.shigoto.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {
    private final UserRepository userRepository;

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    UserDetailsService userDetailsService() {
        return email -> userRepository.findByEmail(email)
                .map(user -> User.withUsername(user.getEmail())
                        .password(user.getPassword())
                        .roles(user.getRole().name())
                        .build())
                .orElseThrow(() -> new org.springframework.security.core.userdetails.UsernameNotFoundException(
                        "Invalid email or password"));
    }

    @Bean
    AuthenticationManager authenticationManager(
            UserDetailsService userDetailsService, PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return new ProviderManager(provider);
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
                        .ignoringRequestMatchers("/api/auth/register", "/api/auth/login"))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/register", "/api/auth/login", "/api/auth/csrf").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/hr/jobs").hasRole("HR")
                        .requestMatchers(HttpMethod.POST, "/api/hr/jobs").hasRole("HR")
                        .requestMatchers(HttpMethod.PUT, "/api/hr/jobs/**").hasRole("HR")
                        .requestMatchers(HttpMethod.OPTIONS, "/api/hr/applications/**").permitAll()
                        .requestMatchers("/api/hr/applications/**").hasRole("HR")
                        .requestMatchers(HttpMethod.GET, "/api/jobs", "/api/jobs/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/jobs").hasRole("HR")
                        .requestMatchers(HttpMethod.GET, "/api/auth/me").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/auth/logout").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/api/auth/me/profile").authenticated()
                        .requestMatchers(HttpMethod.GET,
                                "/api/applications",
                                "/api/applications/candidate/**",
                                "/api/users").hasRole("HR")
                        .requestMatchers(HttpMethod.PUT, "/api/applications/{applicationId}").hasRole("HR")
                        .requestMatchers(HttpMethod.DELETE, "/api/applications/{applicationId}").hasRole("HR")
                        .requestMatchers(HttpMethod.POST, "/api/interviews").hasRole("HR")
                        .requestMatchers(HttpMethod.POST, "/api/applications").authenticated()
                        .requestMatchers(HttpMethod.GET,
                                "/api/applications/mine",
                                "/api/applications/{applicationId}",
                                "/api/applications/{applicationId}/cv",
                                "/api/applications/{applicationId}/interviews").authenticated()
                        .requestMatchers(HttpMethod.PUT,
                                "/api/applications/{applicationId}/task-submission").authenticated()
                        .anyRequest().permitAll())
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)));
        return http.build();
    }
}
