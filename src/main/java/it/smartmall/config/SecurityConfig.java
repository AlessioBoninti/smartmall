package it.smartmall.config;

import it.smartmall.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod; // <-- AGGIUNTO QUESTO IMPORT
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final AuthenticationProvider authenticationProvider;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        // Chiunque può fare login e vedere gli slot liberi
                        .requestMatchers("/api/auth/**", "/api/slots/**").permitAll()

                        //Permettiamo a tutti solo di LEGGERE (GET) la lista dei negozi
                        .requestMatchers(HttpMethod.GET, "/api/stores").permitAll()

                        //TUTTO IL RESTO (prenotazioni, aggiungere/modificare negozi) richiede il Token!
                        .anyRequest().authenticated()
                )
                //Indichiamo a Spring di non usare le sessioni classiche (stateless)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authenticationProvider(authenticationProvider)
                //Impostiamo il filtro di accesso
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}