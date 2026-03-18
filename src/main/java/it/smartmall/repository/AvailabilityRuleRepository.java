package it.smartmall.repository;

import it.smartmall.model.AvailabilityRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface AvailabilityRuleRepository extends JpaRepository<AvailabilityRule, Long> {

    
    List<AvailabilityRule> findByStoreIdAndDayOfWeekAndActiveTrue(Long storeId, Integer dayOfWeek);
}