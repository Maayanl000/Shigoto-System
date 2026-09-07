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
import org.springframework.security.web.authentication.session.ChangeSessionIdAuthenticationStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;

/**
 * Configures security behavior for the application.
 * Required collaborators are supplied through Lombok-generated constructor injection.
 */
@Configuration
@RequiredArgsConstructor
public class SecurityConfig {
    private final UserRepository userRepository;

    /**
     * Provides the BCrypt encoder used to hash and verify stored credentials.
     * @return a BCrypt password encoder
     */
    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Loads application users and roles for Spring Security authentication.
     * @return a user-details service backed by the application user repository
     */
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

    /**
     * Builds the DAO-backed authentication manager used by the session login flow.
     * @param userDetailsService the user details service
     * @param passwordEncoder the password encoder
     * @return an authentication manager using the repository-backed user service and configured password encoder
     */
    @Bean
    AuthenticationManager authenticationManager(
            UserDetailsService userDetailsService, PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return new ProviderManager(provider);
    }

    /**
     * Provides session-fixation protection for successful authentication.
     * @return a strategy that changes the session identifier after authentication
     */
    @Bean
    SessionAuthenticationStrategy sessionAuthenticationStrategy() {
        return new ChangeSessionIdAuthenticationStrategy();
    }

    /**
     * Configures CSRF cookies, endpoint authorization, session authentication, and logout handling.
     * @param http the Spring Security HTTP configuration builder
     * @return the configured application security filter chain
     * @throws Exception if Spring Security cannot build the filter chain
     */
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Configure cookie-backed CSRF protection while allowing login and registration bootstrap requests.
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
                        .ignoringRequestMatchers("/api/auth/register", "/api/auth/login"))
                .authorizeHttpRequests(auth -> auth
                        // Permit preflight requests and endpoints required before authentication.
                        .requestMatchers(HttpMethod.OPTIONS, "/api/**").permitAll()
                        .requestMatchers("/api/auth/register", "/api/auth/login", "/api/auth/csrf").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/users/register").permitAll()
                        // Restrict staff operations according to the authenticated HR or interviewer role.
                        .requestMatchers(HttpMethod.GET, "/api/hr/jobs").hasRole("HR")
                        .requestMatchers(HttpMethod.POST, "/api/hr/jobs").hasRole("HR")
                        .requestMatchers(HttpMethod.PUT, "/api/hr/jobs/**").hasRole("HR")
                        .requestMatchers(HttpMethod.OPTIONS, "/api/hr/applications/**").permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/api/hr/interviewers").permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/api/hr/interviews/**").permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/api/interviewer/**").permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/api/notifications/**").permitAll()
                        .requestMatchers("/api/hr/applications/**").hasRole("HR")
                        .requestMatchers("/api/hr/interviewers").hasRole("HR")
                        .requestMatchers("/api/hr/interviews/**").hasRole("HR")
                        .requestMatchers("/api/interviewer/**").hasRole("INTERVIEWER")
                        .requestMatchers(HttpMethod.GET, "/api/jobs", "/api/jobs/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/jobs").hasRole("HR")
                        .requestMatchers(HttpMethod.GET, "/api/auth/me").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/auth/logout").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/api/auth/me/profile").authenticated()
                        // Protect company-scoped application operations with the HR role.
                        .requestMatchers(HttpMethod.GET,
                                "/api/applications",
                                "/api/applications/candidate/**").hasRole("HR")
                        .requestMatchers(HttpMethod.PUT, "/api/applications/{applicationId}").hasRole("HR")
                        .requestMatchers(HttpMethod.DELETE, "/api/applications/{applicationId}").hasRole("HR")
                        .requestMatchers(HttpMethod.POST, "/api/applications").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/interviews/mine").hasRole("CANDIDATE")
                        .requestMatchers("/api/notifications/**").hasRole("CANDIDATE")
                        // Require authentication while services enforce ownership of candidate application resources.
                        .requestMatchers(HttpMethod.GET,
                                "/api/applications/mine",
                                "/api/applications/{applicationId}",
                                "/api/applications/{applicationId}/cv",
                                "/api/applications/{applicationId}/interviews").authenticated()
                        .requestMatchers(HttpMethod.PUT,
                                "/api/applications/{applicationId}/task-submission").authenticated()
                        // Reject every route that has not been explicitly authorized above.
                        .anyRequest().denyAll())
                // Return HTTP authentication and access-denied statuses instead of browser login redirects.
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)));
        return http.build();
    }
}
