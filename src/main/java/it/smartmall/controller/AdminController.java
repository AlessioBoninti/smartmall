package it.smartmall.controller;

import it.smartmall.dto.AdminBookingDTO;
import it.smartmall.dto.AdminStoreDTO;
import it.smartmall.dto.AdminUserDTO;
import it.smartmall.dto.RoleChangeRequestDTO;
import it.smartmall.dto.SuspendStoreRequest;
import it.smartmall.dto.UpdateUserRoleRequest;
import it.smartmall.model.User;
import it.smartmall.service.AdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/users")
    public ResponseEntity<List<AdminUserDTO>> getAllUsers() {
        return ResponseEntity.ok(adminService.getAllUsers());
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<Void> deleteUser(
            @PathVariable Long id,
            @AuthenticationPrincipal User currentUser) {

        adminService.deleteUser(id, currentUser);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/users/{id}/role")
    public ResponseEntity<AdminUserDTO> updateUserRole(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRoleRequest request,
            @AuthenticationPrincipal User currentUser) {

        return ResponseEntity.ok(adminService.updateUserRole(id, request, currentUser));
    }

    @GetMapping("/role-requests")
    public ResponseEntity<List<RoleChangeRequestDTO>> getRoleChangeRequests() {
        return ResponseEntity.ok(adminService.getRoleChangeRequests());
    }

    @PatchMapping("/role-requests/{id}/approve")
    public ResponseEntity<RoleChangeRequestDTO> approveRoleChangeRequest(
            @PathVariable Long id,
            @AuthenticationPrincipal User currentUser) {

        return ResponseEntity.ok(adminService.approveRoleChangeRequest(id, currentUser));
    }

    @PatchMapping("/role-requests/{id}/reject")
    public ResponseEntity<RoleChangeRequestDTO> rejectRoleChangeRequest(
            @PathVariable Long id,
            @AuthenticationPrincipal User currentUser) {

        return ResponseEntity.ok(adminService.rejectRoleChangeRequest(id, currentUser));
    }

    @GetMapping("/stores")
    public ResponseEntity<List<AdminStoreDTO>> getAllStores() {
        return ResponseEntity.ok(adminService.getAllStores());
    }

    @PatchMapping("/stores/{id}/suspend")
    public ResponseEntity<AdminStoreDTO> suspendStore(
            @PathVariable Long id,
            @Valid @RequestBody SuspendStoreRequest request) {

        return ResponseEntity.ok(adminService.suspendStore(id, request));
    }

    @PatchMapping("/stores/{id}/unsuspend")
    public ResponseEntity<AdminStoreDTO> unsuspendStore(@PathVariable Long id) {
        return ResponseEntity.ok(adminService.unsuspendStore(id));
    }

    @PatchMapping("/stores/{id}/close")
    public ResponseEntity<AdminStoreDTO> closeStore(@PathVariable Long id) {
        return ResponseEntity.ok(adminService.closeStore(id));
    }

    @PatchMapping("/stores/{id}/activate")
    public ResponseEntity<AdminStoreDTO> activateStore(@PathVariable Long id) {
        return ResponseEntity.ok(adminService.activateStore(id));
    }

    @GetMapping("/bookings")
    public ResponseEntity<List<AdminBookingDTO>> getAllBookings() {
        return ResponseEntity.ok(adminService.getAllBookings());
    }
}
