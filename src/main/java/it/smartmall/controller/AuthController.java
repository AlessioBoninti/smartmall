package it.smartmall.controller;

import it.smartmall.dto.LoginRequest;
import it.smartmall.dto.RegisterRequest;
import it.smartmall.exception.EmailAlreadyUsedException;
import it.smartmall.exception.InvalidCredentialsException;
import it.smartmall.model.Role;
import it.smartmall.model.User;
import it.smartmall.repository.UserRepository;
import it.smartmall.security.JwtCookieService;
import it.smartmall.security.JwtUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final JwtUtil jwtUtil;
    private final JwtCookieService jwtCookieService;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @GetMapping("/csrf")
    public ResponseEntity<Map<String, String>> csrf(CsrfToken csrfToken) {
        String token = csrfToken.getToken();
        return ResponseEntity.ok(Map.of(
                "headerName", csrfToken.getHeaderName(),
                "token", token
        ));
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(@Valid @RequestBody LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail(),
                            request.getPassword()
                    )
            );

            UserDetails userDetails = userDetailsService.loadUserByUsername(request.getEmail());
            String jwt = jwtUtil.generateToken(userDetails);
            ResponseCookie jwtCookie = jwtCookieService.createJwtCookie(jwt);

            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, jwtCookie.toString())
                    .body(Map.of("message", "Login effettuato"));

        } catch (AuthenticationException e) {
            // Convertiamo l'errore di Spring Security in una risposta applicativa chiara.
            throw new InvalidCredentialsException("Email o password non validi!");
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout() {
        ResponseCookie clearedCookie = jwtCookieService.clearJwtCookie();
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, clearedCookie.toString())
                .body(Map.of("message", "Logout effettuato"));
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> register(@Valid @RequestBody RegisterRequest request) {

        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            // Restituiamo un JSON tramite una mappa
            throw new EmailAlreadyUsedException("Questa email è già in uso!");
        }

        User newUser = new User();
        newUser.setEmail(request.getEmail());
        newUser.setPassword(passwordEncoder.encode(request.getPassword()));
        newUser.setRole(Role.CUSTOMER);

        userRepository.save(newUser);

        return ResponseEntity.ok(Map.of(
                "message", "Utente registrato con successo come " + newUser.getRole().name() + "! Ora puoi fare il login."
        ));
    }
}
