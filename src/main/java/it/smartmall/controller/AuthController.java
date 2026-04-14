package it.smartmall.controller;

import it.smartmall.dto.LoginRequest;
import it.smartmall.dto.RegisterRequest;
import it.smartmall.dto.TokenResponse;
import it.smartmall.exception.EmailAlreadyUsedException;
import it.smartmall.exception.InvalidCredentialsException;
import it.smartmall.model.Role;
import it.smartmall.model.User;
import it.smartmall.repository.UserRepository;
import it.smartmall.security.JwtUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
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

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail(),
                            request.getPassword()
                    )
            );

            UserDetails userDetails = userDetailsService.loadUserByUsername(request.getEmail());
            String jwt = jwtUtil.generateToken(userDetails);

            return ResponseEntity.ok(new TokenResponse(jwt));

        } catch (AuthenticationException e) {
            // Lanciamo un eccezione per indicare l' erore
            throw new InvalidCredentialsException("Email o password non validi!");
        }
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

        // RESTITUIAMO UN JSON TRAMITE UNA MAPPA!
        return ResponseEntity.ok(Map.of(
                "message", "Utente registrato con successo come " + newUser.getRole().name() + "! Ora puoi fare il login."
        ));
    }
}