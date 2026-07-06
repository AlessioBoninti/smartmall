package it.smartmall.service;

import it.smartmall.dto.AvailabilityRuleDTO;
import it.smartmall.dto.AvailabilityRuleRequestDTO;
import it.smartmall.dto.MerchantBookingDTO;
import it.smartmall.dto.MerchantStoreDTO;
import it.smartmall.exception.InvalidSlotException;
import it.smartmall.exception.StoreNotFoundException;
import it.smartmall.exception.UnauthorizedBookingAccessException;
import it.smartmall.model.AvailabilityRule;
import it.smartmall.model.Booking;
import it.smartmall.model.BookingStatus;
import it.smartmall.model.Store;
import it.smartmall.model.User;
import it.smartmall.repository.AvailabilityRuleRepository;
import it.smartmall.repository.BookingRepository;
import it.smartmall.repository.StoreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MerchantService {

    private final StoreRepository storeRepository;
    private final BookingRepository bookingRepository;
    private final AvailabilityRuleRepository availabilityRuleRepository;

    public List<MerchantStoreDTO> getMyStores(User currentUser) {
        return storeRepository.findByMerchantId(currentUser.getId())
                .stream()
                .map(this::toMerchantStoreDTO)
                .toList();
    }

    public List<MerchantBookingDTO> getMyStoreBookings(User currentUser) {
        return bookingRepository
                .findByStoreMerchantIdAndStatusOrderByStartDateTimeDesc(currentUser.getId(), BookingStatus.CONFIRMED)
                .stream()
                .map(this::toMerchantBookingDTO)
                .toList();
    }

    public List<AvailabilityRuleDTO> getAvailabilityRules(Long storeId, User currentUser) {
        Store store = getOwnedStore(storeId, currentUser);

        return availabilityRuleRepository
                .findByStoreIdOrderByDayOfWeekAscStartTimeAsc(store.getId())
                .stream()
                .map(this::toAvailabilityRuleDTO)
                .toList();
    }

    public AvailabilityRuleDTO createAvailabilityRule(Long storeId, AvailabilityRuleRequestDTO request, User currentUser) {
        Store store = getOwnedStore(storeId, currentUser);
        validateAvailabilityRule(request);
        ensureNoRuleForSameDay(store.getId(), request.getDayOfWeek(), null);

        AvailabilityRule rule = new AvailabilityRule();
        rule.setStore(store);
        applyAvailabilityRuleRequest(rule, request);

        return toAvailabilityRuleDTO(availabilityRuleRepository.save(rule));
    }

    public AvailabilityRuleDTO updateAvailabilityRule(Long ruleId, AvailabilityRuleRequestDTO request, User currentUser) {
        validateAvailabilityRule(request);

        AvailabilityRule rule = availabilityRuleRepository.findByIdAndStoreMerchantId(ruleId, currentUser.getId())
                .orElseThrow(() -> new StoreNotFoundException("Regola di disponibilità non trovata"));

        ensureNoRuleForSameDay(rule.getStore().getId(), request.getDayOfWeek(), rule.getId());

        applyAvailabilityRuleRequest(rule, request);
        return toAvailabilityRuleDTO(availabilityRuleRepository.save(rule));
    }

    public void deleteAvailabilityRule(Long ruleId, User currentUser) {
        AvailabilityRule rule = availabilityRuleRepository.findByIdAndStoreMerchantId(ruleId, currentUser.getId())
                .orElseThrow(() -> new StoreNotFoundException("Regola di disponibilità non trovata"));

        availabilityRuleRepository.delete(rule);
    }

    private Store getOwnedStore(Long storeId, User currentUser) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new StoreNotFoundException("Store non trovato"));

        if (!store.getMerchant().getId().equals(currentUser.getId())) {
            throw new UnauthorizedBookingAccessException("Non sei il gestore di questo negozio");
        }

        return store;
    }

    private void ensureNoRuleForSameDay(Long storeId, Integer dayOfWeek, Long currentRuleId) {
        boolean hasAnotherRule = availabilityRuleRepository.findByStoreIdAndDayOfWeek(storeId, dayOfWeek)
                .stream()
                .anyMatch(rule -> currentRuleId == null || !rule.getId().equals(currentRuleId));

        if (hasAnotherRule) {
            throw new InvalidSlotException("Esiste già una regola per questo giorno. Modifica quella esistente.");
        }
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

    private MerchantStoreDTO toMerchantStoreDTO(Store store) {
        MerchantStoreDTO dto = new MerchantStoreDTO();
        dto.setId(store.getId());
        dto.setName(store.getName());
        dto.setStatus(store.getStatus().name());
        dto.setSuspendedReason(store.getSuspendedReason());
        return dto;
    }

    private MerchantBookingDTO toMerchantBookingDTO(Booking booking) {
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
    }

    private AvailabilityRuleDTO toAvailabilityRuleDTO(AvailabilityRule rule) {
        AvailabilityRuleDTO dto = new AvailabilityRuleDTO();
        dto.setId(rule.getId());
        dto.setStoreId(rule.getStore().getId());
        dto.setDayOfWeek(rule.getDayOfWeek());
        dto.setMorningStartTime(AvailabilityUtils.getMorningStartTime(rule));
        dto.setMorningEndTime(AvailabilityUtils.getMorningEndTime(rule));
        dto.setAfternoonStartTime(AvailabilityUtils.getAfternoonStartTime(rule));
        dto.setAfternoonEndTime(AvailabilityUtils.getAfternoonEndTime(rule));
        dto.setClosed(AvailabilityUtils.isClosed(rule));
        dto.setSlotMinutes(rule.getSlotMinutes());
        dto.setCapacityPerSlot(rule.getCapacityPerSlot());
        dto.setActive(rule.getActive());
        return dto;
    }

    private boolean isClosedRequest(AvailabilityRuleRequestDTO request) {
        return Boolean.TRUE.equals(request.getClosed());
    }
}
