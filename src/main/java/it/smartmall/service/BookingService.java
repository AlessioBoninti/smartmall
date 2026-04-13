package it.smartmall.service;

import it.smartmall.dto.BookingRequestDTO;
import it.smartmall.exception.*;
import it.smartmall.model.*;
import it.smartmall.repository.AvailabilityRuleRepository;
import it.smartmall.repository.BookingRepository;
import it.smartmall.repository.StoreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepository;
    private final StoreRepository storeRepository;
    private final AvailabilityRuleRepository ruleRepository;

    @Transactional
    public Booking createBooking(BookingRequestDTO dto, User customer) {

        // ROW LOCK: Blocchiamo il negozio
        Store store = storeRepository.findByIdWithLock(dto.getStoreId())
                .orElseThrow(() -> new StoreNotFoundException("Store non trovato")); // <-- ECCEZIONE SPECIFICA

        // VERIFICA SOSPENSIONE
        if (store.getSuspendedFrom() != null && store.getSuspendedTo() != null) {
            LocalDateTime now = LocalDateTime.now();
            if (now.isAfter(store.getSuspendedFrom()) && now.isBefore(store.getSuspendedTo())) {
                throw new StoreSuspendedException("Lo store è momentaneamente sospeso: " + store.getSuspendedReason()); // <-- ECCEZIONE SPECIFICA
            }
        }

        // PREAVVISO DI 5 ORE MINIME
        LocalDateTime limiteMassimoPrenotazione = dto.getStartDateTime().minusHours(5);

        if (LocalDateTime.now().isAfter(limiteMassimoPrenotazione)) {
            throw new RuntimeException("Le prenotazioni devono essere effettuate con almeno 5 ore di anticipo rispetto all'orario dello slot.");
        }

        // RECUPERO REGOLA DI DISPONIBILITÀ
        int dayOfWeek = dto.getStartDateTime().getDayOfWeek().getValue();
        LocalTime time = dto.getStartDateTime().toLocalTime();

        List<AvailabilityRule> rules = ruleRepository.findByStoreIdAndDayOfWeekAndActiveTrue(store.getId(), dayOfWeek);

        AvailabilityRule activeRule = rules.stream()
                .filter(rule -> (time.equals(rule.getStartTime()) || time.isAfter(rule.getStartTime())) && time.isBefore(rule.getEndTime()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Orario non valido o fuori dalle regole di disponibilità del negozio"));

        // CONTROLLO ALLINEAMENTO SLOT
        long minutesFromStart = java.time.Duration.between(activeRule.getStartTime(), time).toMinutes();

        if (minutesFromStart % activeRule.getSlotMinutes() != 0) {
            throw new RuntimeException("Formato orario non valido. Gli slot per questo negozio sono ogni "
                    + activeRule.getSlotMinutes() + " minuti a partire dalle " + activeRule.getStartTime());
        }

        // PREVENZIONE OVERBOOKING (Capienza REALE)
        int capacity = activeRule.getCapacityPerSlot();

        int prenotazioniEsistenti = bookingRepository.countByStoreIdAndStartDateTimeAndStatus(
                store.getId(), dto.getStartDateTime(), BookingStatus.CONFIRMED);

        if (prenotazioniEsistenti >= capacity) {
            throw new SlotFullException("SLOT_FULL: Nessun posto disponibile per questo orario"); // <-- ECCEZIONE SPECIFICA
        }

        // REGOLE ANTI-SPAM (Fair Play)
        boolean hasAlreadyBooked = bookingRepository.existsByCustomerIdAndStoreIdAndStartDateTimeAndStatus(
                customer.getId(), store.getId(), dto.getStartDateTime(), BookingStatus.CONFIRMED);

        if (hasAlreadyBooked) {
            throw new SlotFullException("Hai già effettuato una prenotazione per questo orario in questo negozio. Non puoi occupare più posti.");
        }

        // CREAZIONE PRENOTAZIONE
        Booking booking = new Booking();
        booking.setCustomer(customer);
        booking.setStore(store);
        booking.setStartDateTime(dto.getStartDateTime());
        booking.setEndDateTime(dto.getStartDateTime().plusMinutes(activeRule.getSlotMinutes())); // Durata reale!
        booking.setStatus(BookingStatus.CONFIRMED);

        return bookingRepository.save(booking);
    }

    @Transactional
    public Booking cancelBooking(Long bookingId, User currentUser) {
        // Troviamo la prenotazione
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Prenotazione non trovata"));

        // Controllo Stato: È già cancellata?
        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new RuntimeException("La prenotazione è già stata cancellata");
        }

        // Controllo Temporale: Lo slot è già passato? (Vale per entrambi)
        if (LocalDateTime.now().isAfter(booking.getStartDateTime())) {
            throw new RuntimeException("Non puoi cancellare una prenotazione passata o già iniziata");
        }

        // CONTROLLO DI SICUREZZA E OWNERSHIP (Broken Access Control)
        if (currentUser.getRole() == Role.CUSTOMER) {
            // Se sei un cliente, puoi cancellare solo le TUE prenotazioni
            if (!booking.getCustomer().getId().equals(currentUser.getId())) {
                throw new UnauthorizedBookingAccessException("Non sei autorizzato a cancellare questa prenotazione"); // <-- ECCEZIONE SPECIFICA
            }
        } else if (currentUser.getRole() == Role.MERCHANT) {
            // Se sei un negoziante, puoi cancellare solo le prenotazioni dei TUOI negozi
            if (!booking.getStore().getMerchant().getId().equals(currentUser.getId())) {
                throw new UnauthorizedBookingAccessException("Non sei il gestore di questo negozio"); // <-- ECCEZIONE SPECIFICA
            }
        } else {
            throw new UnauthorizedBookingAccessException("Il tuo ruolo non ti permette di cancellare prenotazioni"); // <-- ECCEZIONE SPECIFICA
        }

        // Eseguiamo la cancellazione
        booking.setStatus(BookingStatus.CANCELLED);
        return bookingRepository.save(booking);
    }
}