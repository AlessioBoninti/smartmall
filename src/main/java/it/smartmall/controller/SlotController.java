package it.smartmall.controller;

import it.smartmall.dto.SlotDTO;
import it.smartmall.service.SlotService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/slots")
@RequiredArgsConstructor
public class SlotController {

    private final SlotService slotService;

    // Esempio chiamata: GET /api/slots?storeId=1&date=2026-03-12
    @GetMapping
    public ResponseEntity<List<SlotDTO>> getAvailableSlots(
            @RequestParam Long storeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        List<SlotDTO> slots = slotService.getAvailableSlots(storeId, date);
        return ResponseEntity.ok(slots);
    }
}