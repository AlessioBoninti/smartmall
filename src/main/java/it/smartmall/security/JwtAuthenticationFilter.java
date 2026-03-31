package it.smartmall.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String userEmail;

        // Se non c'tè l'header "Authorization" o non inizia con "Bearer ", ignoriamo e andiamo avanti
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // Estraiamo il token (togliendo la parola "Bearer ") e l'email
        jwt = authHeader.substring(7);
        userEmail = jwtUtil.extractUsername(jwt);

        // Se l'email c'è e l'utente non è ancora loggato nel contesto di questa richiesta
        if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {

            // Peschiamo l'utente dal Database tramite l'email
            UserDetails userDetails = this.userDetailsService.loadUserByUsername(userEmail);

            // Se il token è valido, diciamo a Spring Security: "Ok, l'utente è ufficialmente loggato!"
            if (jwtUtil.isTokenValid(jwt, userDetails)) {
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities()
                );
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                // Salviamo l'autenticazione nel SecurityContext
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }

        // Passiamo la palla al prossimo filtro o al Controller
        filterChain.doFilter(request, response);
    }
}