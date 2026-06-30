package it.smartmall.repository;

import it.smartmall.model.RoleChangeRequest;
import it.smartmall.model.RoleChangeRequestStatus;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoleChangeRequestRepository extends JpaRepository<RoleChangeRequest, Long> {

    boolean existsByRequesterIdAndStatus(Long requesterId, RoleChangeRequestStatus status);

    Optional<RoleChangeRequest> findFirstByRequesterIdOrderByCreatedAtDesc(Long requesterId);

    List<RoleChangeRequest> findAllByOrderByCreatedAtDesc();

    void deleteByRequesterId(Long requesterId);

    @Modifying
    @Query("UPDATE RoleChangeRequest r SET r.reviewedBy = null WHERE r.reviewedBy.id = :userId")
    void clearReviewedById(@Param("userId") Long userId);
}
