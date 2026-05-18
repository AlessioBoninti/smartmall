package it.smartmall.controller;

import it.smartmall.dto.MerchantBookingDTO;
import it.smartmall.dto.MerchantStoreDTO;
import it.smartmall.model.Store;
import it.smartmall.model.User;
import it.smartmall.repository.BookingRepository;
import it.smartmall.repository.StoreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/merchant")
@RequiredArgsConstructor
public class MerchantController {

    private final StoreRepository storeRepository;
    private final BookingRepository bookingRepository;

    @GetMapping("/stores")
    public ResponseEntity<List<MerchantStoreDTO>> getMyStores(
            @AuthenticationPrincipal User currentUser) {

        List<MerchantStoreDTO> response = storeRepository.findByMerchantId(currentUser.getId())
                .stream()
                .map(this::mapStoreToDto)
                .toList();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/bookings")
    public ResponseEntity<List<MerchantBookingDTO>> getMyStoreBookings(
            @AuthenticationPrincipal User currentUser) {

        List<MerchantBookingDTO> response = bookingRepository
                .findByStoreMerchantIdOrderByStartDateTimeDesc(currentUser.getId())
                .stream()
                .map(booking -> {
                    MerchantBookingDTO dto = new MerchantBookingDTO();
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

    private MerchantStoreDTO mapStoreToDto(Store store) {
        MerchantStoreDTO dto = new MerchantStoreDTO();
        dto.setId(store.getId());
        dto.setName(store.getName());
        dto.setStatus(store.getStatus().name());
        dto.setSuspendedReason(store.getSuspendedReason());
        return dto;
    }
}