package it.smartmall.repository;

import it.smartmall.model.Store;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StoreRepository extends JpaRepository<Store, Long> {

    List<Store> findByMerchantId(Long merchantId);

    // ECCO IL PUNTO 5: Questa query applica il "FOR UPDATE" al database!
    // Blocca la riga del negozio finché la transazione non è finita.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Store s WHERE s.id = :id")
    Optional<Store> findByIdWithLock(@Param("id") Long id);

}