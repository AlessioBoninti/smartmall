package it.smartmall.controller;

import it.smartmall.dto.*;
import it.smartmall.exception.InvalidStoreSuspensionException;
import it.smartmall.exception.RoleChangeRequestException;
import it.smartmall.exception.RoleChangeNotAllowedException;
import it.smartmall.exception.StoreNotFoundException;
import it.smartmall.exception.UserNotFoundException;
import it.smartmall.model.*;
import it.smartmall.repository.BookingRepository;
import it.smartmall.repository.RoleChangeRequestRepository;
import it.smartmall.repository.StoreRepository;
import it.smartmall.repository.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final UserRepository userRepository;
    private final StoreRepository storeRepository;
    private final BookingRepository bookingRepository;
    private final RoleChangeRequestRepository roleChangeRequestRepository;

    @GetMapping("/users")
    public ResponseEntity<List<AdminUserDTO>> getAllUsers() {
        List<AdminUserDTO> response = userRepository.findAll()
                .stream()
                .map(user -> {
                    AdminUserDTO dto = new AdminUserDTO();
                    dto.setId(user.getId());
                    dto.setEmail(user.getEmail());
                    dto.setRole(user.getRole().name());
                    return dto;
                })
                .toList();

        return ResponseEntity.ok(response);
    }

    @PatchMapping("/users/{id}/role")
    public ResponseEntity<AdminUserDTO> updateUserRole(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRoleRequest request,
            @AuthenticationPrincipal User currentUser) {

        User targetUser = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("Utente non trovato"));

        if (targetUser.getId().equals(currentUser.getId())) {
            throw new RoleChangeNotAllowedException("Non puoi cambiare il tuo stesso ruolo");
        }

        targetUser.setRole(request.getRole());
        userRepository.save(targetUser);

        AdminUserDTO response = new AdminUserDTO();
        response.setId(targetUser.getId());
        response.setEmail(targetUser.getEmail());
        response.setRole(targetUser.getRole().name());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/role-requests")
    public ResponseEntity<List<RoleChangeRequestDTO>> getRoleChangeRequests() {
        List<RoleChangeRequestDTO> response = roleChangeRequestRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::mapRoleChangeRequestToDto)
                .toList();

        return ResponseEntity.ok(response);
    }

    @PatchMapping("/role-requests/{id}/approve")
    public ResponseEntity<RoleChangeRequestDTO> approveRoleChangeRequest(
            @PathVariable Long id,
            @AuthenticationPrincipal User currentUser) {

        RoleChangeRequest request = roleChangeRequestRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("Richiesta cambio ruolo non trovata"));

        if (request.getStatus() != RoleChangeRequestStatus.PENDING) {
            throw new RoleChangeRequestException("Questa richiesta è già stata valutata");
        }

        User requester = request.getRequester();
        requester.setRole(request.getRequestedRole());
        userRepository.save(requester);

        request.setStatus(RoleChangeRequestStatus.APPROVED);
        request.setReviewedAt(LocalDateTime.now());
        request.setReviewedBy(currentUser);

        return ResponseEntity.ok(mapRoleChangeRequestToDto(roleChangeRequestRepository.save(request)));
    }

    @PatchMapping("/role-requests/{id}/reject")
    public ResponseEntity<RoleChangeRequestDTO> rejectRoleChangeRequest(
            @PathVariable Long id,
            @AuthenticationPrincipal User currentUser) {

        RoleChangeRequest request = roleChangeRequestRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("Richiesta cambio ruolo non trovata"));

        if (request.getStatus() != RoleChangeRequestStatus.PENDING) {
            throw new RoleChangeRequestException("Questa richiesta è già stata valutata");
        }

        request.setStatus(RoleChangeRequestStatus.REJECTED);
        request.setReviewedAt(LocalDateTime.now());
        request.setReviewedBy(currentUser);

        return ResponseEntity.ok(mapRoleChangeRequestToDto(roleChangeRequestRepository.save(request)));
    }

    @GetMapping("/stores")
    public ResponseEntity<List<AdminStoreDTO>> getAllStores() {
        List<AdminStoreDTO> response = storeRepository.findAll()
                .stream()
                .map(this::mapStoreToDto)
                .toList();

        return ResponseEntity.ok(response);
    }

    @PatchMapping("/stores/{id}/suspend")
    public ResponseEntity<AdminStoreDTO> suspendStore(
            @PathVariable Long id,
            @Valid @RequestBody SuspendStoreRequest request) {

        Store store = storeRepository.findById(id)
                .orElseThrow(() -> new StoreNotFoundException("Store non trovato"));

        if (request.getTo().isBefore(request.getFrom()) || request.getTo().isEqual(request.getFrom())) {
            throw new InvalidStoreSuspensionException("La data di fine sospensione deve essere successiva alla data di inizio");
        }

        store.setStatus(StoreStatus.SUSPENDED);
        store.setSuspendedFrom(request.getFrom());
        store.setSuspendedTo(request.getTo());
        store.setSuspendedReason(request.getReason());

        storeRepository.save(store);

        return ResponseEntity.ok(mapStoreToDto(store));
    }

    @PatchMapping("/stores/{id}/unsuspend")
    public ResponseEntity<AdminStoreDTO> unsuspendStore(@PathVariable Long id) {
        Store store = storeRepository.findById(id)
                .orElseThrow(() -> new StoreNotFoundException("Store non trovato"));

        store.setStatus(StoreStatus.ACTIVE);
        store.setSuspendedFrom(null);
        store.setSuspendedTo(null);
        store.setSuspendedReason(null);

        storeRepository.save(store);

        return ResponseEntity.ok(mapStoreToDto(store));
    }

    @PatchMapping("/stores/{id}/close")
    public ResponseEntity<AdminStoreDTO> closeStore(@PathVariable Long id) {
        Store store = storeRepository.findById(id)
                .orElseThrow(() -> new StoreNotFoundException("Store non trovato"));

        store.setStatus(StoreStatus.CLOSED);
        store.setSuspendedFrom(null);
        store.setSuspendedTo(null);
        store.setSuspendedReason(null);

        storeRepository.save(store);

        return ResponseEntity.ok(mapStoreToDto(store));
    }

    @PatchMapping("/stores/{id}/activate")
    public ResponseEntity<AdminStoreDTO> activateStore(@PathVariable Long id) {
        Store store = storeRepository.findById(id)
                .orElseThrow(() -> new StoreNotFoundException("Store non trovato"));

        store.setStatus(StoreStatus.ACTIVE);
        store.setSuspendedFrom(null);
        store.setSuspendedTo(null);
        store.setSuspendedReason(null);

        storeRepository.save(store);

        return ResponseEntity.ok(mapStoreToDto(store));
    }

    @GetMapping("/bookings")
    public ResponseEntity<List<AdminBookingDTO>> getAllBookings() {
        List<AdminBookingDTO> response = bookingRepository.findAll()
                .stream()
                .map(booking -> {
                    AdminBookingDTO dto = new AdminBookingDTO();
                    dto.setId(booking.getId());
                    dto.setStoreId(booking.getStore().getId());
                    dto.setStoreName(booking.getStore().getName());
                    dto.setCustomerId(booking.getCustomer().getId());
                    dto.setCustomerEmail(booking.getCustomer().getEmail());
                    dto.setStartDateTime(booking.getStartDateTime());
                    dto.setEndDateTime(booking.getEndDateTime());
                    dto.setStatus(booking.getStatus().name());
                    return dto;
                })
                .toList();

        return ResponseEntity.ok(response);
    }

    private AdminStoreDTO mapStoreToDto(Store store) {
        AdminStoreDTO dto = new AdminStoreDTO();
        dto.setId(store.getId());
        dto.setName(store.getName());
        dto.setStatus(store.getStatus().name());
        dto.setMerchantId(store.getMerchant().getId());
        dto.setMerchantEmail(store.getMerchant().getEmail());
        dto.setSuspendedFrom(store.getSuspendedFrom());
        dto.setSuspendedTo(store.getSuspendedTo());
        dto.setSuspendedReason(store.getSuspendedReason());
        return dto;
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
