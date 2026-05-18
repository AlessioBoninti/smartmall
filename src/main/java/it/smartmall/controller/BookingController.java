package it.smartmall.controller;

import it.smartmall.dto.BookingRequestDTO;
import it.smartmall.dto.BookingResponseDTO;
import it.smartmall.dto.MyBookingDTO;
import it.smartmall.model.Booking;
import it.smartmall.model.User;
import it.smartmall.service.BookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

    @GetMapping("/my")
    public ResponseEntity<List<MyBookingDTO>> getMyBookings(
            @AuthenticationPrincipal User currentUser) {

        List<MyBookingDTO> response = bookingService.getMyBookings(currentUser)
                .stream()
                .map(booking -> {
                    MyBookingDTO dto = new MyBookingDTO();
                    dto.setId(booking.getId());
                    dto.setStoreId(booking.getStore().getId());
                    dto.setStoreName(booking.getStore().getName());
                    dto.setStartDateTime(booking.getStartDateTime());
                    dto.setEndDateTime(booking.getEndDateTime());
                    dto.setStatus(booking.getStatus().name());
                    return dto;
                })
                .toList();

        return ResponseEntity.ok(response);
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