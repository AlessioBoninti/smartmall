package it.smartmall.repository;

import it.smartmall.model.Booking;
import it.smartmall.model.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    int countByStoreIdAndStartDateTimeAndStatus(Long storeId, LocalDateTime startDateTime, BookingStatus status);

    boolean existsByCustomerIdAndStoreIdAndStartDateTimeAndStatus(
            Long customerId, Long storeId, LocalDateTime startDateTime, BookingStatus status);

    List<Booking> findByStoreIdAndStartDateTimeBetweenAndStatus(
            Long storeId,
            LocalDateTime start,
            LocalDateTime end,
            BookingStatus status);

    List<Booking> findByCustomerIdOrderByStartDateTimeDesc(Long customerId);

    List<Booking> findByStoreMerchantIdOrderByStartDateTimeDesc(Long merchantId);
}