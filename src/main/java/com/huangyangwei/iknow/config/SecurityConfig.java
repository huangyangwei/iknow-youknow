package com.huangyangwei.iknow.config;
import org.springframework.context.annotation.*; import org.springframework.security.config.Customizer; import org.springframework.security.config.annotation.web.builders.HttpSecurity; import org.springframework.security.core.userdetails.*; import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder; import org.springframework.security.crypto.password.PasswordEncoder; import org.springframework.security.web.SecurityFilterChain;
@Configuration public class SecurityConfig {
 @Bean SecurityFilterChain security(HttpSecurity http) throws Exception { return http.csrf(c->c.disable()).authorizeHttpRequests(a->a.requestMatchers("/actuator/health").permitAll().requestMatchers("/api/v1/admin/**").hasRole("ADMIN").anyRequest().authenticated()).httpBasic(Customizer.withDefaults()).build(); }
 @Bean UserDetailsService users(PasswordEncoder e) { return new InMemoryUserDetailsManager(User.withUsername("user").password(e.encode("changeit")).roles("USER").build(), User.withUsername("admin").password(e.encode("changeit")).roles("USER","ADMIN").build()); }
 @Bean PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }
}
