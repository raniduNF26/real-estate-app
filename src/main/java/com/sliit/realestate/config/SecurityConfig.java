package com.sliit.realestate.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration

@EnableWebSecurity

public class SecurityConfig {



    @Autowired

    @Lazy

    private CustomAuthenticationProvider customAuthenticationProvider;

    @Autowired

    private CustomSuccessHandler customSuccessHandler;

    @Bean

    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http.authenticationProvider(customAuthenticationProvider);

        http

                .authorizeHttpRequests(authorize -> authorize

                        .requestMatchers("/css/**", "/js/**", "/img/**", "/webjars/**", "/favicon.ico").permitAll()

                        .requestMatchers("/", "/about", "/services", "/contact", "/login", "/register", "/properties", "/properties/view/**").permitAll()

                        .requestMatchers("/agent/**").hasAnyRole("AGENT", "ADMIN")

                        .requestMatchers("/client/**").hasAnyRole("CLIENT", "ADMIN")

                        .requestMatchers("/admin/**").hasRole("ADMIN")

                        .anyRequest().authenticated()

                )

                .formLogin(form -> form

                        .loginPage("/login")

                        .loginProcessingUrl("/login")

                        .successHandler(customSuccessHandler)

                        .failureUrl("/login?error=true")

                        .permitAll()

                )

                .logout(logout -> logout

                        .logoutRequestMatcher(new AntPathRequestMatcher("/logout"))

                        .logoutSuccessUrl("/login?logout")

                        .permitAll()

                );



        return http.build();

    }



    @Bean

    public PasswordEncoder passwordEncoder() {

        return new BCryptPasswordEncoder();

    }

}