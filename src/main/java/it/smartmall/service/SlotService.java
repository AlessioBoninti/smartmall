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
            LocalTime currentSlotStart = rule.getStartTime();

            while (currentSlotStart.plusMinutes(rule.getSlotMinutes()).isBefore(rule.getEndTime()) ||
                    currentSlotStart.plusMinutes(rule.getSlotMinutes()).equals(rule.getEndTime())) {

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

        return availableSlots;
    }
}