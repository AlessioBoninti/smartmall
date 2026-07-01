package it.smartmall.controller;

import it.smartmall.dto.CreateRoleChangeRequestDTO;
import it.smartmall.dto.MeResponseDTO;
import it.smartmall.dto.RoleChangeRequestDTO;
import it.smartmall.exception.RoleChangeRequestException;
import it.smartmall.model.Role;
import it.smartmall.model.RoleChangeRequest;
import it.smartmall.model.RoleChangeRequestStatus;
import it.smartmall.model.User;
import it.smartmall.repository.RoleChangeRequestRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/me")
@RequiredArgsConstructor
public class UserController {

    private final RoleChangeRequestRepository roleChangeRequestRepository;

    @GetMapping
    public ResponseEntity<MeResponseDTO> getCurrentUser(@AuthenticationPrincipal User currentUser) {

        MeResponseDTO response = MeResponseDTO.builder()
                .id(currentUser.getId())
                .email(currentUser.getEmail())
                .role(currentUser.getRole().name())
                .build();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/merchant-request")
    public ResponseEntity<RoleChangeRequestDTO> getMyMerchantRequest(@AuthenticationPrincipal User currentUser) {
        return roleChangeRequestRepository.findFirstByRequesterIdOrderByCreatedAtDesc(currentUser.getId())
                .map(request -> ResponseEntity.ok(mapRoleChangeRequestToDto(request)))
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @PostMapping("/merchant-request")
    public ResponseEntity<RoleChangeRequestDTO> requestMerchantRole(
            @Valid @RequestBody CreateRoleChangeRequestDTO request,
            @AuthenticationPrincipal User currentUser) {

        if (currentUser.getRole() != Role.CUSTOMER) {
            throw new RoleChangeRequestException("Solo un customer può richiedere il passaggio a merchant");
        }

        boolean hasPendingRequest = roleChangeRequestRepository.existsByRequesterIdAndStatus(
                currentUser.getId(),
                RoleChangeRequestStatus.PENDING
        );

        if (hasPendingRequest) {
            throw new RoleChangeRequestException("Hai già una richiesta merchant in attesa di valutazione");
        }

        RoleChangeRequest roleChangeRequest = new RoleChangeRequest();
        roleChangeRequest.setRequester(currentUser);
        roleChangeRequest.setRequestedRole(Role.MERCHANT);
        roleChangeRequest.setReason(request.getReason());

        RoleChangeRequest savedRequest = roleChangeRequestRepository.save(roleChangeRequest);
        return ResponseEntity.ok(mapRoleChangeRequestToDto(savedRequest));
    }

    private RoleChangeRequestDTO mapRoleChangeRequestToDto(RoleChangeRequest request) {
        RoleChangeRequestDTO dto = new RoleChangeRequestDTO();
        dto.setId(request.getId());
        dto.setRequesterId(request.getRequester().getId());
        dto.setRequesterEmail(request.getRequester().getEmail());
        dto.setRequestedRole(request.getRequestedRole().name());
        dto.setStatus(request.getStatus().name());
        dto.setReason(request.getReason());
        dto.setCreatedAt(request.getCreatedAt());
        dto.setReviewedAt(request.getReviewedAt());

        if (request.getReviewedBy() != null) {
            dto.setReviewedById(request.getReviewedBy().getId());
            dto.setReviewedByEmail(request.getReviewedBy().getEmail());
        }

        return dto;
    }
}
