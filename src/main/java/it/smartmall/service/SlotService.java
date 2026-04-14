package it.smartmall.service;

import it.smartmall.dto.SlotDTO;
import it.smartmall.exception.StoreNotFoundException;
import it.smartmall.model.AvailabilityRule;
import it.smartmall.model.Booking;
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
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SlotService {

    private final StoreRepository storeRepository;
    private final AvailabilityRuleRepository ruleRepository;
    private final BookingRepository bookingRepository;

    // Generazione Slot
    public List<SlotDTO> getAvailableSlots(Long storeId, LocalDate date) {
        List<SlotDTO> availableSlots = new ArrayList<>();

        // 1. Verifichiamo che il negozio esista
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new StoreNotFoundException("Store non trovato")); // Corretto anche l'errore dell'eccezione generica!

        // 2. Verifichiamo se lo store è sospeso
        if (store.getSuspendedFrom() != null && store.getSuspendedTo() != null) {
            LocalDateTime now = LocalDateTime.now();
            if (now.isAfter(store.getSuspendedFrom()) && now.isBefore(store.getSuspendedTo())) {
                return availableSlots;
            }
        }

        // Definiamo inizio e fine della giornata richiesta
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(LocalTime.MAX);

        // Facciamo UNA SOLA QUERY per prendere tutte le prenotazioni di oggi
        List<Booking> dailyBookings = bookingRepository.findByStoreIdAndStartDateTimeBetweenAndStatus(
                storeId, startOfDay, endOfDay, BookingStatus.CONFIRMED);

        // Raggruppiamo le prenotazioni per orario in una Mappa: Mappa<Orario, NumeroPrenotazioni>
        Map<LocalDateTime, Long> bookedSeatsMap = dailyBookings.stream()
                .collect(Collectors.groupingBy(Booking::getStartDateTime, Collectors.counting()));


        // Recuperiamo le regole del giorno
        int dayOfWeek = date.getDayOfWeek().getValue();
        List<AvailabilityRule> rules = ruleRepository.findByStoreIdAndDayOfWeekAndActiveTrue(storeId, dayOfWeek);

        // Generiamo gli slot
        for (AvailabilityRule rule : rules) {
            LocalTime currentSlotStart = rule.getStartTime();

            while (currentSlotStart.plusMinutes(rule.getSlotMinutes()).isBefore(rule.getEndTime()) ||
                    currentSlotStart.plusMinutes(rule.getSlotMinutes()).equals(rule.getEndTime())) {

                LocalDateTime startDateTime = LocalDateTime.of(date, currentSlotStart);
                LocalDateTime endDateTime = startDateTime.plusMinutes(rule.getSlotMinutes());

                if (startDateTime.isAfter(LocalDateTime.now())) {

                    // INVECE DI INTERROGARE IL DB, LEGGIAMO DALLA MAPPA (Tempo: 0 ms)
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