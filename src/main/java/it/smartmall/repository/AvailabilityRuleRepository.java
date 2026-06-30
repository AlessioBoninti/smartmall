package it.smartmall.repository;

import it.smartmall.model.AvailabilityRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface AvailabilityRuleRepository extends JpaRepository<AvailabilityRule, Long> {

    // Per trovare tutte le regole attive di un negozio per un determinato giorno
    List<AvailabilityRule> findByStoreIdAndDayOfWeekAndActiveTrue(Long storeId, Integer dayOfWeek);

    List<AvailabilityRule> findByStoreIdOrderByDayOfWeekAscStartTimeAsc(Long storeId);

    Optional<AvailabilityRule> findByIdAndStoreMerchantId(Long id, Long merchantId);

    List<AvailabilityRule> findByStoreIdAndDayOfWeek(Long storeId, Integer dayOfWeek);

    void deleteByStoreId(Long storeId);
}
