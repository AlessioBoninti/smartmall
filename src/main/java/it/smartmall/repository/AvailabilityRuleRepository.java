package it.smartmall.repository;

import it.smartmall.model.AvailabilityRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface AvailabilityRuleRepository extends JpaRepository<AvailabilityRule, Long> {

    // Per trovare tutte le regole attive di un negozio per un determinato giorno
    List<AvailabilityRule> findByStoreIdAndDayOfWeekAndActiveTrue(Long storeId, Integer dayOfWeek);
}