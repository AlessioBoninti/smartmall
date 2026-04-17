package it.smartmall.controller;

import it.smartmall.dto.MeResponseDTO;
import it.smartmall.model.User;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/me")
public class UserController {

    @GetMapping
    public ResponseEntity<MeResponseDTO> getCurrentUser(@AuthenticationPrincipal User currentUser) {

        // Trasformiamo l'utente loggato nel DTO da inviare a React
        MeResponseDTO response = MeResponseDTO.builder()
                .id(currentUser.getId())
                .email(currentUser.getEmail())
                .role(currentUser.getRole().name())
                .build();

        return ResponseEntity.ok(response);
    }
}