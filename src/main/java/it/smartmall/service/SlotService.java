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

    // Generazione Slot
    public List<SlotDTO> getAvailableSlots(Long storeId, LocalDate date) {
        List<SlotDTO> availableSlots = new ArrayList<>();

        // Verifichiamo che il negozio esista
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new RuntimeException("Store non trovato"));

        //  Verifichiamo se lo store è sospeso
        if (store.getSuspendedFrom() != null && store.getSuspendedTo() != null) {
            LocalDateTime now = LocalDateTime.now();
            if (now.isAfter(store.getSuspendedFrom()) && now.isBefore(store.getSuspendedTo())) {
                // Se è sospeso, restituiamo lista vuota come da relazione
                return availableSlots;
            }
        }

        // Recuperiamo le regole del giorno (es. 1=Lunedì, ..., 7=Domenica)
        int dayOfWeek = date.getDayOfWeek().getValue();
        List<AvailabilityRule> rules = ruleRepository.findByStoreIdAndDayOfWeekAndActiveTrue(storeId, dayOfWeek);

        // Generiamo gli slot per ogni regola trovata
        for (AvailabilityRule rule : rules) {
            LocalTime currentSlotStart = rule.getStartTime();

            // Ciclo finché non superiamo l'orario di fine (endTime)
            while (currentSlotStart.plusMinutes(rule.getSlotMinutes()).isBefore(rule.getEndTime()) ||
                    currentSlotStart.plusMinutes(rule.getSlotMinutes()).equals(rule.getEndTime())) {

                LocalDateTime startDateTime = LocalDateTime.of(date, currentSlotStart);
                LocalDateTime endDateTime = startDateTime.plusMinutes(rule.getSlotMinutes());

                // Evitiamo di restituire slot nel passato
                if (startDateTime.isAfter(LocalDateTime.now())) {

                    // Contiamo quante prenotazioni CONFIRMED ci sono già per questo slot
                    int bookedSeats = bookingRepository.countByStoreIdAndStartDateTimeAndStatus(
                            storeId, startDateTime, BookingStatus.CONFIRMED);

                    // Calcoliamo i posti rimasti: capacityPerSlot - count(CONFIRMED)
                    int availableCapacity = rule.getCapacityPerSlot() - bookedSeats;

                    // Restituiamo solo gli slot con posti > 0
                    if (availableCapacity > 0) {
                        availableSlots.add(new SlotDTO(startDateTime, endDateTime, availableCapacity));
                    }
                }

                // Passiamo allo slot successivo aggiungendo i minuti (es. +30 min)
                currentSlotStart = currentSlotStart.plusMinutes(rule.getSlotMinutes());
            }
        }

        return availableSlots;
    }
}