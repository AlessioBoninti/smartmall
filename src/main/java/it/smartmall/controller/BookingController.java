package it.smartmall.controller;

import it.smartmall.dto.BookingRequestDTO;
import it.smartmall.dto.BookingResponseDTO;
import it.smartmall.model.Booking;
import it.smartmall.model.User;
import it.smartmall.service.BookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    @PostMapping
    public ResponseEntity<BookingResponseDTO> createBooking(
            @Valid @RequestBody BookingRequestDTO requestDTO,
            @AuthenticationPrincipal User currentUser) {

        Booking createdBooking = bookingService.createBooking(requestDTO, currentUser);

        BookingResponseDTO responseDTO = new BookingResponseDTO();
        responseDTO.setId(createdBooking.getId());
        responseDTO.setStoreId(createdBooking.getStore().getId());
        responseDTO.setCustomerId(createdBooking.getCustomer().getId());
        responseDTO.setStartDateTime(createdBooking.getStartDateTime());
        responseDTO.setEndDateTime(createdBooking.getEndDateTime());
        responseDTO.setStatus(createdBooking.getStatus().name());

        return ResponseEntity.ok(responseDTO);
    }

    @PatchMapping("/{id}/cancel")
    public ResponseEntity<BookingResponseDTO> cancelBooking(
            @PathVariable Long id,
            @AuthenticationPrincipal User currentUser) {

        Booking cancelledBooking = bookingService.cancelBooking(id, currentUser);

        BookingResponseDTO responseDTO = new BookingResponseDTO();
        responseDTO.setId(cancelledBooking.getId());
        responseDTO.setStoreId(cancelledBooking.getStore().getId());
        responseDTO.setCustomerId(cancelledBooking.getCustomer().getId());
        responseDTO.setStartDateTime(cancelledBooking.getStartDateTime());
        responseDTO.setEndDateTime(cancelledBooking.getEndDateTime());
        responseDTO.setStatus(cancelledBooking.getStatus().name());

        return ResponseEntity.ok(responseDTO);
    }
}