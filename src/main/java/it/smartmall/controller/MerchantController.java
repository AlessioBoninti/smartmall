package it.smartmall.controller;

import it.smartmall.dto.AvailabilityRuleDTO;
import it.smartmall.dto.AvailabilityRuleRequestDTO;
import it.smartmall.dto.MerchantBookingDTO;
import it.smartmall.dto.MerchantStoreDTO;
import it.smartmall.model.User;
import it.smartmall.service.MerchantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/merchant")
@RequiredArgsConstructor
public class MerchantController {

    private final MerchantService merchantService;

    @GetMapping("/stores")
    public ResponseEntity<List<MerchantStoreDTO>> getMyStores(
            @AuthenticationPrincipal User currentUser) {

        return ResponseEntity.ok(merchantService.getMyStores(currentUser));
    }

    @GetMapping("/bookings")
    public ResponseEntity<List<MerchantBookingDTO>> getMyStoreBookings(
            @AuthenticationPrincipal User currentUser) {

        return ResponseEntity.ok(merchantService.getMyStoreBookings(currentUser));
    }

    @GetMapping("/stores/{storeId}/availability-rules")
    public ResponseEntity<List<AvailabilityRuleDTO>> getAvailabilityRules(
            @PathVariable Long storeId,
            @AuthenticationPrincipal User currentUser) {

        return ResponseEntity.ok(merchantService.getAvailabilityRules(storeId, currentUser));
    }

    @PostMapping("/stores/{storeId}/availability-rules")
    public ResponseEntity<AvailabilityRuleDTO> createAvailabilityRule(
            @PathVariable Long storeId,
            @Valid @RequestBody AvailabilityRuleRequestDTO request,
            @AuthenticationPrincipal User currentUser) {

        return ResponseEntity.ok(merchantService.createAvailabilityRule(storeId, request, currentUser));
    }

    @PatchMapping("/availability-rules/{ruleId}")
    public ResponseEntity<AvailabilityRuleDTO> updateAvailabilityRule(
            @PathVariable Long ruleId,
            @Valid @RequestBody AvailabilityRuleRequestDTO request,
            @AuthenticationPrincipal User currentUser) {

        return ResponseEntity.ok(merchantService.updateAvailabilityRule(ruleId, request, currentUser));
    }

    @DeleteMapping("/availability-rules/{ruleId}")
    public ResponseEntity<Void> deleteAvailabilityRule(
            @PathVariable Long ruleId,
            @AuthenticationPrincipal User currentUser) {

        merchantService.deleteAvailabilityRule(ruleId, currentUser);
        return ResponseEntity.noContent().build();
    }
}
