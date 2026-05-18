package it.smartmall.controller;

import it.smartmall.dto.AvailabilityRuleDTO;
import it.smartmall.dto.AvailabilityRuleRequestDTO;
import it.smartmall.dto.MerchantBookingDTO;
import it.smartmall.dto.MerchantStoreDTO;
import it.smartmall.exception.InvalidSlotException;
import it.smartmall.exception.StoreNotFoundException;
import it.smartmall.exception.UnauthorizedBookingAccessException;
import it.smartmall.model.AvailabilityRule;
import it.smartmall.model.BookingStatus;
import it.smartmall.model.Store;
import it.smartmall.model.User;
import it.smartmall.repository.AvailabilityRuleRepository;
import it.smartmall.repository.BookingRepository;
import it.smartmall.repository.StoreRepository;
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

import java.time.LocalTime;
import java.util.List;

@RestController
@RequestMapping("/api/merchant")
@RequiredArgsConstructor
public class MerchantController {

    private static final LocalTime DEFAULT_MORNING_END = LocalTime.of(13, 0);
    private static final LocalTime DEFAULT_AFTERNOON_START = LocalTime.of(14, 0);

    private final StoreRepository storeRepository;
    private final BookingRepository bookingRepository;
    private final AvailabilityRuleRepository availabilityRuleRepository;

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
                .findByStoreMerchantIdAndStatusOrderByStartDateTimeDesc(currentUser.getId(), BookingStatus.CONFIRMED)
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

    @GetMapping("/stores/{storeId}/availability-rules")
    public ResponseEntity<List<AvailabilityRuleDTO>> getAvailabilityRules(
            @PathVariable Long storeId,
            @AuthenticationPrincipal User currentUser) {

        Store store = getOwnedStore(storeId, currentUser);

        List<AvailabilityRuleDTO> response = availabilityRuleRepository
                .findByStoreIdOrderByDayOfWeekAscStartTimeAsc(store.getId())
                .stream()
                .map(this::mapAvailabilityRuleToDto)
                .toList();

        return ResponseEntity.ok(response);
    }

    @PostMapping("/stores/{storeId}/availability-rules")
    public ResponseEntity<AvailabilityRuleDTO> createAvailabilityRule(
            @PathVariable Long storeId,
            @Valid @RequestBody AvailabilityRuleRequestDTO request,
            @AuthenticationPrincipal User currentUser) {

        Store store = getOwnedStore(storeId, currentUser);
        validateAvailabilityRule(request);

        List<AvailabilityRule> existingRules = availabilityRuleRepository.findByStoreIdAndDayOfWeek(
                store.getId(),
                request.getDayOfWeek()
        );

        if (!existingRules.isEmpty()) {
            throw new InvalidSlotException("Esiste già una regola per questo giorno. Modifica quella esistente.");
        }

        AvailabilityRule rule = new AvailabilityRule();
        rule.setStore(store);
        applyAvailabilityRuleRequest(rule, request);

        return ResponseEntity.ok(mapAvailabilityRuleToDto(availabilityRuleRepository.save(rule)));
    }

    @PatchMapping("/availability-rules/{ruleId}")
    public ResponseEntity<AvailabilityRuleDTO> updateAvailabilityRule(
            @PathVariable Long ruleId,
            @Valid @RequestBody AvailabilityRuleRequestDTO request,
            @AuthenticationPrincipal User currentUser) {

        validateAvailabilityRule(request);

        AvailabilityRule rule = availabilityRuleRepository.findByIdAndStoreMerchantId(ruleId, currentUser.getId())
                .orElseThrow(() -> new StoreNotFoundException("Regola di disponibilità non trovata"));

        List<AvailabilityRule> existingRules = availabilityRuleRepository.findByStoreIdAndDayOfWeek(
                rule.getStore().getId(),
                request.getDayOfWeek()
        );

        boolean hasAnotherRuleForDay = existingRules.stream()
                .anyMatch(existingRule -> !existingRule.getId().equals(rule.getId()));

        if (hasAnotherRuleForDay) {
            throw new InvalidSlotException("Esiste già una regola per questo giorno. Modifica quella esistente.");
        }

        applyAvailabilityRuleRequest(rule, request);
        return ResponseEntity.ok(mapAvailabilityRuleToDto(availabilityRuleRepository.save(rule)));
    }

    @DeleteMapping("/availability-rules/{ruleId}")
    public ResponseEntity<Void> deleteAvailabilityRule(
            @PathVariable Long ruleId,
            @AuthenticationPrincipal User currentUser) {

        AvailabilityRule rule = availabilityRuleRepository.findByIdAndStoreMerchantId(ruleId, currentUser.getId())
                .orElseThrow(() -> new StoreNotFoundException("Regola di disponibilità non trovata"));

        availabilityRuleRepository.delete(rule);
        return ResponseEntity.noContent().build();
    }

    private MerchantStoreDTO mapStoreToDto(Store store) {
        MerchantStoreDTO dto = new MerchantStoreDTO();
        dto.setId(store.getId());
        dto.setName(store.getName());
        dto.setStatus(store.getStatus().name());
        dto.setSuspendedReason(store.getSuspendedReason());
        return dto;
    }

    private Store getOwnedStore(Long storeId, User currentUser) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new StoreNotFoundException("Store non trovato"));

        if (!store.getMerchant().getId().equals(currentUser.getId())) {
            throw new UnauthorizedBookingAccessException("Non sei il gestore di questo negozio");
        }

        return store;
    }

    private void validateAvailabilityRule(AvailabilityRuleRequestDTO request) {
        if (isClosedRequest(request)) {
            return;
        }

        if (request.getMorningStartTime() == null || request.getMorningEndTime() == null ||
                request.getAfternoonStartTime() == null || request.getAfternoonEndTime() == null) {
            throw new InvalidSlotException("Inserisci tutti gli orari oppure segna il giorno come chiuso");
        }

        if (!request.getMorningEndTime().isAfter(request.getMorningStartTime())) {
            throw new InvalidSlotException("La chiusura mattina deve essere successiva all'inizio mattina");
        }

        if (!request.getAfternoonEndTime().isAfter(request.getAfternoonStartTime())) {
            throw new InvalidSlotException("La chiusura pomeriggio deve essere successiva all'inizio pomeriggio");
        }

        if (!request.getAfternoonStartTime().isAfter(request.getMorningEndTime())) {
            throw new InvalidSlotException("L'inizio pomeriggio deve essere successivo alla chiusura mattina");
        }
    }

    private void applyAvailabilityRuleRequest(AvailabilityRule rule, AvailabilityRuleRequestDTO request) {
        boolean closed = isClosedRequest(request);

        rule.setDayOfWeek(request.getDayOfWeek());
        rule.setClosed(closed);

        if (closed) {
            rule.setStartTime(LocalTime.MIDNIGHT);
            rule.setEndTime(LocalTime.MIDNIGHT);
            rule.setMorningStartTime(null);
            rule.setMorningEndTime(null);
            rule.setAfternoonStartTime(null);
            rule.setAfternoonEndTime(null);
        } else {
            rule.setStartTime(request.getMorningStartTime());
            rule.setEndTime(request.getAfternoonEndTime());
            rule.setMorningStartTime(request.getMorningStartTime());
            rule.setMorningEndTime(request.getMorningEndTime());
            rule.setAfternoonStartTime(request.getAfternoonStartTime());
            rule.setAfternoonEndTime(request.getAfternoonEndTime());
        }

        rule.setSlotMinutes(request.getSlotMinutes());
        rule.setCapacityPerSlot(request.getCapacityPerSlot());
        rule.setActive(!closed && (request.getActive() == null || request.getActive()));
    }

    private AvailabilityRuleDTO mapAvailabilityRuleToDto(AvailabilityRule rule) {
        AvailabilityRuleDTO dto = new AvailabilityRuleDTO();
        dto.setId(rule.getId());
        dto.setStoreId(rule.getStore().getId());
        dto.setDayOfWeek(rule.getDayOfWeek());
        dto.setMorningStartTime(getMorningStartTime(rule));
        dto.setMorningEndTime(getMorningEndTime(rule));
        dto.setAfternoonStartTime(getAfternoonStartTime(rule));
        dto.setAfternoonEndTime(getAfternoonEndTime(rule));
        dto.setClosed(isClosedRule(rule));
        dto.setSlotMinutes(rule.getSlotMinutes());
        dto.setCapacityPerSlot(rule.getCapacityPerSlot());
        dto.setActive(rule.getActive());
        return dto;
    }

    private boolean isClosedRequest(AvailabilityRuleRequestDTO request) {
        return Boolean.TRUE.equals(request.getClosed());
    }

    private boolean isClosedRule(AvailabilityRule rule) {
        return Boolean.TRUE.equals(rule.getClosed());
    }

    private LocalTime getMorningStartTime(AvailabilityRule rule) {
        if (isClosedRule(rule)) {
            return null;
        }

        if (rule.getMorningStartTime() != null) {
            return rule.getMorningStartTime();
        }

        if (rule.getStartTime() == null || !rule.getStartTime().isBefore(DEFAULT_MORNING_END)) {
            return null;
        }

        return rule.getStartTime();
    }

    private LocalTime getMorningEndTime(AvailabilityRule rule) {
        if (isClosedRule(rule)) {
            return null;
        }

        if (rule.getMorningEndTime() != null) {
            return rule.getMorningEndTime();
        }

        if (rule.getStartTime() == null || rule.getEndTime() == null ||
                !rule.getStartTime().isBefore(DEFAULT_MORNING_END)) {
            return null;
        }

        return rule.getEndTime().isAfter(DEFAULT_MORNING_END) ? DEFAULT_MORNING_END : rule.getEndTime();
    }

    private LocalTime getAfternoonStartTime(AvailabilityRule rule) {
        if (isClosedRule(rule)) {
            return null;
        }

        if (rule.getAfternoonStartTime() != null) {
            return rule.getAfternoonStartTime();
        }

        if (rule.getStartTime() == null || rule.getEndTime() == null ||
                !rule.getEndTime().isAfter(DEFAULT_AFTERNOON_START)) {
            return null;
        }

        return rule.getStartTime().isAfter(DEFAULT_AFTERNOON_START)
                ? rule.getStartTime()
                : DEFAULT_AFTERNOON_START;
    }

    private LocalTime getAfternoonEndTime(AvailabilityRule rule) {
        if (isClosedRule(rule)) {
            return null;
        }

        if (rule.getAfternoonEndTime() != null) {
            return rule.getAfternoonEndTime();
        }

        if (rule.getEndTime() == null || !rule.getEndTime().isAfter(DEFAULT_AFTERNOON_START)) {
            return null;
        }

        return rule.getEndTime();
    }
}
