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

        Store store = storeRepository.findByIdWithLock(dto.getStoreId())
                .orElseThrow(() -> new StoreNotFoundException("Store non trovato"));

        if (store.getStatus() == StoreStatus.CLOSED) {
            throw new StoreClosedException("Lo store è chiuso definitivamente e non accetta più prenotazioni.");
        }

        if (store.getStatus() == StoreStatus.SUSPENDED) {
            throw new StoreSuspendedException("Lo store è momentaneamente sospeso: " + store.getSuspendedReason());
        }

        LocalDateTime limiteMassimoPrenotazione = dto.getStartDateTime().minusHours(5);

        if (LocalDateTime.now().isAfter(limiteMassimoPrenotazione)) {
            throw new BookingTooLateException("Le prenotazioni devono essere effettuate con almeno 5 ore di anticipo rispetto all'orario dello slot.");
        }

        int dayOfWeek = dto.getStartDateTime().getDayOfWeek().getValue();
        LocalTime time = dto.getStartDateTime().toLocalTime();

        List<AvailabilityRule> rules = ruleRepository.findByStoreIdAndDayOfWeekAndActiveTrue(store.getId(), dayOfWeek);

        AvailabilityRule activeRule = rules.stream()
                .filter(rule -> (time.equals(rule.getStartTime()) || time.isAfter(rule.getStartTime())) && time.isBefore(rule.getEndTime()))
                .findFirst()
                .orElseThrow(() -> new InvalidSlotException("Orario non valido o fuori dalle regole di disponibilità del negozio"));

        long minutesFromStart = java.time.Duration.between(activeRule.getStartTime(), time).toMinutes();

        if (minutesFromStart % activeRule.getSlotMinutes() != 0) {
            throw new InvalidSlotException("Formato orario non valido. Gli slot per questo negozio sono ogni "
                    + activeRule.getSlotMinutes() + " minuti a partire dalle " + activeRule.getStartTime());
        }

        int capacity = activeRule.getCapacityPerSlot();

        int prenotazioniEsistenti = bookingRepository.countByStoreIdAndStartDateTimeAndStatus(
                store.getId(), dto.getStartDateTime(), BookingStatus.CONFIRMED);

        if (prenotazioniEsistenti >= capacity) {
            throw new SlotFullException("SLOT_FULL: Nessun posto disponibile per questo orario");
        }

        boolean hasAlreadyBooked = bookingRepository.existsByCustomerIdAndStoreIdAndStartDateTimeAndStatus(
                customer.getId(), store.getId(), dto.getStartDateTime(), BookingStatus.CONFIRMED);

        if (hasAlreadyBooked) {
            throw new SlotFullException("Hai già effettuato una prenotazione per questo orario in questo negozio. Non puoi occupare più posti.");
        }

        Booking booking = new Booking();
        booking.setCustomer(customer);
        booking.setStore(store);
        booking.setStartDateTime(dto.getStartDateTime());
        booking.setEndDateTime(dto.getStartDateTime().plusMinutes(activeRule.getSlotMinutes()));
        booking.setStatus(BookingStatus.CONFIRMED);

        return bookingRepository.save(booking);
    }

    @Transactional(readOnly = true)
    public List<Booking> getMyBookings(User currentUser) {
        return bookingRepository.findByCustomerIdOrderByStartDateTimeDesc(currentUser.getId());
    }

    @Transactional
    public Booking cancelBooking(Long bookingId, User currentUser) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BookingNotFoundException("Prenotazione non trovata"));

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new BookingAlreadyCancelledException("La prenotazione è già stata cancellata");
        }

        if (LocalDateTime.now().isAfter(booking.getStartDateTime())) {
            throw new PastBookingCancellationException("Non puoi cancellare una prenotazione passata o già iniziata");
        }

        LocalDateTime limiteMassimoCancellazione = booking.getStartDateTime().minusHours(12);

        if (LocalDateTime.now().isAfter(limiteMassimoCancellazione)) {
            throw new CancellationTooLateException("Le prenotazioni possono essere cancellate solo con almeno 12 ore di preavviso.");
        }

        if (currentUser.getRole() == Role.CUSTOMER) {
            if (!booking.getCustomer().getId().equals(currentUser.getId())) {
                throw new UnauthorizedBookingAccessException("Non sei autorizzato a cancellare questa prenotazione");
            }
        } else if (currentUser.getRole() == Role.MERCHANT) {
            if (!booking.getStore().getMerchant().getId().equals(currentUser.getId())) {
                throw new UnauthorizedBookingAccessException("Non sei il gestore di questo negozio");
            }
        } else {
            throw new UnauthorizedBookingAccessException("Il tuo ruolo non ti permette di cancellare prenotazioni");
        }

        booking.setStatus(BookingStatus.CANCELLED);
        return bookingRepository.save(booking);
    }
}