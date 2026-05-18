package it.smartmall.service;

import it.smartmall.dto.SlotDTO;
import it.smartmall.exception.StoreNotFoundException;
import it.smartmall.model.AvailabilityRule;
import it.smartmall.model.Booking;
import it.smartmall.model.BookingStatus;
import it.smartmall.model.Store;
import it.smartmall.model.StoreStatus;
import it.smartmall.repository.AvailabilityRuleRepository;
import it.smartmall.repository.BookingRepository;
import it.smartmall.repository.StoreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SlotService {

    private static final LocalTime DEFAULT_MORNING_END = LocalTime.of(13, 0);
    private static final LocalTime DEFAULT_AFTERNOON_START = LocalTime.of(14, 0);

    private final StoreRepository storeRepository;
    private final AvailabilityRuleRepository ruleRepository;
    private final BookingRepository bookingRepository;

    public List<SlotDTO> getAvailableSlots(Long storeId, LocalDate date) {
        List<SlotDTO> availableSlots = new ArrayList<>();

        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new StoreNotFoundException("Store non trovato"));

        if (store.getStatus() == StoreStatus.CLOSED || store.getStatus() == StoreStatus.SUSPENDED) {
            return availableSlots;
        }

        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(LocalTime.MAX);

        List<Booking> dailyBookings = bookingRepository.findByStoreIdAndStartDateTimeBetweenAndStatus(
                storeId, startOfDay, endOfDay, BookingStatus.CONFIRMED);

        Map<LocalDateTime, Long> bookedSeatsMap = dailyBookings.stream()
                .collect(Collectors.groupingBy(Booking::getStartDateTime, Collectors.counting()));

        int dayOfWeek = date.getDayOfWeek().getValue();
        List<AvailabilityRule> rules = ruleRepository.findByStoreIdAndDayOfWeekAndActiveTrue(storeId, dayOfWeek);

        for (AvailabilityRule rule : rules) {
            if (Boolean.TRUE.equals(rule.getClosed())) {
                continue;
            }

            addSlotsForWindow(availableSlots, bookedSeatsMap, date, rule, getMorningStartTime(rule), getMorningEndTime(rule));
            addSlotsForWindow(availableSlots, bookedSeatsMap, date, rule, getAfternoonStartTime(rule), getAfternoonEndTime(rule));
        }

        return availableSlots;
    }

    private void addSlotsForWindow(
            List<SlotDTO> availableSlots,
            Map<LocalDateTime, Long> bookedSeatsMap,
            LocalDate date,
            AvailabilityRule rule,
            LocalTime windowStart,
            LocalTime windowEnd) {

        if (windowStart == null || windowEnd == null) {
            return;
        }

        LocalTime currentSlotStart = windowStart;

        while (currentSlotStart.plusMinutes(rule.getSlotMinutes()).isBefore(windowEnd) ||
                currentSlotStart.plusMinutes(rule.getSlotMinutes()).equals(windowEnd)) {

            LocalDateTime startDateTime = LocalDateTime.of(date, currentSlotStart);
            LocalDateTime endDateTime = startDateTime.plusMinutes(rule.getSlotMinutes());

            if (startDateTime.isAfter(LocalDateTime.now())) {
                int bookedSeats = bookedSeatsMap.getOrDefault(startDateTime, 0L).intValue();
                int availableCapacity = rule.getCapacityPerSlot() - bookedSeats;

                if (availableCapacity > 0) {
                    availableSlots.add(new SlotDTO(startDateTime, endDateTime, availableCapacity));
                }
            }

            currentSlotStart = currentSlotStart.plusMinutes(rule.getSlotMinutes());
        }
    }

    private LocalTime getMorningStartTime(AvailabilityRule rule) {
        if (rule.getMorningStartTime() != null) {
            return rule.getMorningStartTime();
        }

        if (rule.getStartTime() == null || !rule.getStartTime().isBefore(DEFAULT_MORNING_END)) {
            return null;
        }

        return rule.getStartTime();
    }

    private LocalTime getMorningEndTime(AvailabilityRule rule) {
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
        if (rule.getAfternoonEndTime() != null) {
            return rule.getAfternoonEndTime();
        }

        if (rule.getEndTime() == null || !rule.getEndTime().isAfter(DEFAULT_AFTERNOON_START)) {
            return null;
        }

        return rule.getEndTime();
    }
}
