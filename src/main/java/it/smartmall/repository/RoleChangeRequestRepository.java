package it.smartmall.repository;

import it.smartmall.model.RoleChangeRequest;
import it.smartmall.model.RoleChangeRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoleChangeRequestRepository extends JpaRepository<RoleChangeRequest, Long> {

    boolean existsByRequesterIdAndStatus(Long requesterId, RoleChangeRequestStatus status);

    Optional<RoleChangeRequest> findFirstByRequesterIdOrderByCreatedAtDesc(Long requesterId);

    List<RoleChangeRequest> findAllByOrderByCreatedAtDesc();
}
