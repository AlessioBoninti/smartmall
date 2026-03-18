package it.smartmall.repository;

import it.smartmall.model.Booking;
import it.smartmall.model.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    
    int countByStoreIdAndStartDateTimeAndStatus(Long storeId, LocalDateTime startDateTime, BookingStatus status);


    
    boolean existsByCustomerIdAndStoreIdAndStartDateTimeAndStatus(
            Long customerId, Long storeId, LocalDateTime startDateTime, BookingStatus status);

}