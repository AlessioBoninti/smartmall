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
                //Abilitiamo il CORS per React (Porta 5173 di Vite)
                .cors(cors -> cors.configurationSource(request -> {
                    var corsConfig = new org.springframework.web.cors.CorsConfiguration();
                    corsConfig.setAllowedOrigins(java.util.List.of("http://localhost:5173"));
                    corsConfig.setAllowedMethods(java.util.List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
                    corsConfig.setAllowedHeaders(java.util.List.of("*"));
                    return corsConfig;
                }))

                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        // VECCHIO CODICE // Chiunque può fare login e vedere gli slot libero
                        // .requestMatchers("/api/auth/**", "/api/slots/**").permitAll()

                        // NUOVO CODICE // Solo le GET sono pubbliche!
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/slots/**").permitAll()

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