
package com.library.security;

import com.library.entity.LibraryUser;
import com.library.repository.UserRepository;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;

import org.springframework.web.filter.OncePerRequestFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    PasswordEncoder passwords() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    UserDetailsService users(UserRepository users) {
        return username -> users.findByUsername(username)
                .map(u -> User.withUsername(u.getUsername())
                        .password(u.getPasswordHash())
                        .roles(u.getRole())
                        .disabled(!u.isActive())
                        .build())
                .orElseThrow(() ->
                        new UsernameNotFoundException("Invalid credentials"));
    }

    @Bean
    SecurityFilterChain security(HttpSecurity http, UserRepository users)
            throws Exception {

        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/api/auth/csrf",
                    "/api/auth/login"
                ).permitAll()
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            )

            .formLogin(form -> form
                .loginProcessingUrl("/api/auth/login")
                .successHandler((req, res, authentication) -> {
                    res.setContentType("application/json");
                    res.getWriter().write("{\"ok\":true}");
                })
                .failureHandler((req, res, exception) -> {
                    res.setStatus(401);
                    res.setContentType("application/json");
                    res.getWriter().write(
                        "{\"message\":\"Invalid credentials or inactive account\"}"
                    );
                })
            )

            .logout(logout -> logout
                .logoutUrl("/api/auth/logout")
                .logoutSuccessHandler((req, res, authentication) ->
                    res.setStatus(204))
                .deleteCookies("JSESSIONID")
            )

            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((req, res, exception) ->
                    res.sendError(401))
                .accessDeniedHandler((req, res, exception) ->
                    res.sendError(403))
            )

            .addFilterAfter(new OncePerRequestFilter() {

                @Override
                protected void doFilterInternal(
                        HttpServletRequest req,
                        HttpServletResponse res,
                        FilterChain chain)
                        throws IOException, ServletException {

                    var auth = SecurityContextHolder
                            .getContext()
                            .getAuthentication();

                    if (auth != null
                            && auth.isAuthenticated()
                            && !auth.getName().equals("anonymousUser")
                            && users.findByUsername(auth.getName())
                                    .filter(LibraryUser::isActive)
                                    .isEmpty()) {

                        req.getSession().invalidate();

                        SecurityContextHolder.clearContext();

                        res.sendError(401);
                        return;
                    }

                    chain.doFilter(req, res);
                }

            }, AnonymousAuthenticationFilter.class);

        return http.build();
    }
}
