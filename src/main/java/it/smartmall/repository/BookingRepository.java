package it.smartmall.repository;

import it.smartmall.model.Booking;
import it.smartmall.model.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    // Esempio pratico di query sicura per contare i posti occupati
    int countByStoreIdAndStartDateTimeAndStatus(Long storeId, LocalDateTime startDateTime, BookingStatus status);


    // Nuovo metodo per l'Anti-Spam (Fair Play)
    boolean existsByCustomerIdAndStoreIdAndStartDateTimeAndStatus(
            Long customerId, Long storeId, LocalDateTime startDateTime, BookingStatus status);

}