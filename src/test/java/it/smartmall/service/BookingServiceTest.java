package it.smartmall.service;

import it.smartmall.dto.BookingRequestDTO;
import it.smartmall.exception.StoreClosedException;
import it.smartmall.exception.StoreSuspendedException;
import it.smartmall.exception.UnauthorizedBookingAccessException;
import it.smartmall.model.Booking;
import it.smartmall.model.BookingStatus;
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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private StoreRepository storeRepository;

    @Mock
    private AvailabilityRuleRepository ruleRepository;

    private BookingService bookingService;

    @BeforeEach
    void setUp() {
        bookingService = new BookingService(bookingRepository, storeRepository, ruleRepository);
    }

    @Test
    void customerCannotCancelAnotherCustomersBooking() {
        User bookingOwner = user(1L, Role.CUSTOMER);
        User otherCustomer = user(2L, Role.CUSTOMER);
        Store store = store(10L, user(3L, Role.MERCHANT), StoreStatus.ACTIVE);
        Booking booking = booking(20L, bookingOwner, store, LocalDateTime.now().plusDays(2));

        given(bookingRepository.findById(20L)).willReturn(Optional.of(booking));

        assertThatThrownBy(() -> bookingService.cancelBooking(20L, otherCustomer))
                .isInstanceOf(UnauthorizedBookingAccessException.class);

        verify(bookingRepository, never()).save(any(Booking.class));
    }

    @ParameterizedTest
    @EnumSource(value = StoreStatus.class, names = {"SUSPENDED", "CLOSED"})
    void suspendedOrClosedStoreBlocksNewBookings(StoreStatus status) {
        Store store = store(10L, user(3L, Role.MERCHANT), status);
        User customer = user(1L, Role.CUSTOMER);
        BookingRequestDTO request = new BookingRequestDTO();
        request.setStoreId(store.getId());
        request.setStartDateTime(LocalDateTime.now().plusDays(2));

        given(storeRepository.findByIdWithLock(store.getId())).willReturn(Optional.of(store));

        Class<? extends RuntimeException> expectedException =
                status == StoreStatus.SUSPENDED ? StoreSuspendedException.class : StoreClosedException.class;

        assertThatThrownBy(() -> bookingService.createBooking(request, customer))
                .isInstanceOf(expectedException);

        verify(bookingRepository, never()).save(any(Booking.class));
        verifyNoInteractions(ruleRepository);
    }

    private User user(Long id, Role role) {
        User user = new User();
        user.setId(id);
        user.setEmail("user" + id + "@test.com");
        user.setPassword("password");
        user.setRole(role);
        return user;
    }

    private Store store(Long id, User merchant, StoreStatus status) {
        Store store = new Store();
        store.setId(id);
        store.setName("Store " + id);
        store.setMerchant(merchant);
        store.setStatus(status);
        store.setSuspendedReason("Manutenzione");
        return store;
    }

    private Booking booking(Long id, User customer, Store store, LocalDateTime startDateTime) {
        Booking booking = new Booking();
        booking.setId(id);
        booking.setCustomer(customer);
        booking.setStore(store);
        booking.setStartDateTime(startDateTime);
        booking.setEndDateTime(startDateTime.plusMinutes(30));
        booking.setStatus(BookingStatus.CONFIRMED);
        return booking;
    }
}
