package it.smartmall.service;

import it.smartmall.dto.SlotDTO;
import it.smartmall.model.AvailabilityRule;
import it.smartmall.model.BookingStatus;
import it.smartmall.model.Store;
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

@Service
@RequiredArgsConstructor
public class SlotService {

    private final StoreRepository storeRepository;
    private final AvailabilityRuleRepository ruleRepository;
    private final BookingRepository bookingRepository;

  
    public List<SlotDTO> getAvailableSlots(Long storeId, LocalDate date) {
        List<SlotDTO> availableSlots = new ArrayList<>();

        
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new RuntimeException("Store non trovato"));

        
        if (store.getSuspendedFrom() != null && store.getSuspendedTo() != null) {
            LocalDateTime now = LocalDateTime.now();
            if (now.isAfter(store.getSuspendedFrom()) && now.isBefore(store.getSuspendedTo())) {
                
                return availableSlots;
            }
        }

        
        int dayOfWeek = date.getDayOfWeek().getValue();
        List<AvailabilityRule> rules = ruleRepository.findByStoreIdAndDayOfWeekAndActiveTrue(storeId, dayOfWeek);

      
        for (AvailabilityRule rule : rules) {
            LocalTime currentSlotStart = rule.getStartTime();

            // Ciclo finché non superiamo l'orario di fine (endTime)
            while (currentSlotStart.plusMinutes(rule.getSlotMinutes()).isBefore(rule.getEndTime()) ||
                    currentSlotStart.plusMinutes(rule.getSlotMinutes()).equals(rule.getEndTime())) {

                LocalDateTime startDateTime = LocalDateTime.of(date, currentSlotStart);
                LocalDateTime endDateTime = startDateTime.plusMinutes(rule.getSlotMinutes());

               
                if (startDateTime.isAfter(LocalDateTime.now())) {

                   
                    int bookedSeats = bookingRepository.countByStoreIdAndStartDateTimeAndStatus(
                            storeId, startDateTime, BookingStatus.CONFIRMED);

                    
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