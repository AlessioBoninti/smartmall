package it.smartmall.service;

import it.smartmall.dto.AvailabilityRuleRequestDTO;
import it.smartmall.exception.UnauthorizedBookingAccessException;
import it.smartmall.model.Role;
import it.smartmall.model.Store;
import it.smartmall.model.StoreStatus;
import it.smartmall.model.User;
import it.smartmall.repository.AvailabilityRuleRepository;
import it.smartmall.repository.BookingRepository;
import it.smartmall.repository.StoreRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MerchantServiceTest {

    @Mock
    private StoreRepository storeRepository;

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private AvailabilityRuleRepository availabilityRuleRepository;

    private MerchantService merchantService;

    @BeforeEach
    void setUp() {
        merchantService = new MerchantService(storeRepository, bookingRepository, availabilityRuleRepository);
    }

    @Test
    void merchantCannotCreateAvailabilityRuleForStoreOwnedByAnotherMerchant() {
        User currentMerchant = user(1L, Role.MERCHANT);
        User otherMerchant = user(2L, Role.MERCHANT);
        Store otherStore = store(10L, otherMerchant);
        AvailabilityRuleRequestDTO request = validAvailabilityRuleRequest();

        given(storeRepository.findById(otherStore.getId())).willReturn(Optional.of(otherStore));

        assertThatThrownBy(() -> merchantService.createAvailabilityRule(otherStore.getId(), request, currentMerchant))
                .isInstanceOf(UnauthorizedBookingAccessException.class);

        verify(availabilityRuleRepository, never()).save(any());
    }

    private AvailabilityRuleRequestDTO validAvailabilityRuleRequest() {
        AvailabilityRuleRequestDTO request = new AvailabilityRuleRequestDTO();
        request.setDayOfWeek(1);
        request.setMorningStartTime(LocalTime.of(9, 0));
        request.setMorningEndTime(LocalTime.of(13, 0));
        request.setAfternoonStartTime(LocalTime.of(14, 0));
        request.setAfternoonEndTime(LocalTime.of(18, 0));
        request.setClosed(false);
        request.setSlotMinutes(30);
        request.setCapacityPerSlot(2);
        request.setActive(true);
        return request;
    }

    private User user(Long id, Role role) {
        User user = new User();
        user.setId(id);
        user.setEmail("user" + id + "@test.com");
        user.setPassword("password");
        user.setRole(role);
        return user;
    }

    private Store store(Long id, User merchant) {
        Store store = new Store();
        store.setId(id);
        store.setName("Store " + id);
        store.setMerchant(merchant);
        store.setStatus(StoreStatus.ACTIVE);
        return store;
    }
}
