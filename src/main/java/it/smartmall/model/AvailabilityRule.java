package it.smartmall.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.EqualsAndHashCode;
import java.time.LocalTime;

@Entity
@Table(name = "availability_rules")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class AvailabilityRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    // Relazione: Ogni regola appartiene a un Negozio
    @ManyToOne(fetch = FetchType.LAZY) // <-- AGGIUNTO
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @Column(nullable = false)
    private Integer dayOfWeek; // Da 1 (Lunedì) a 7 (Domenica)

    @Column(nullable = false)
    private LocalTime startTime;

    @Column(nullable = false)
    private LocalTime endTime;

    @Column(nullable = false)
    private Integer slotMinutes;

    @Column(nullable = false)
    private Integer capacityPerSlot;

    @Column(nullable = false)
    private Boolean active = true;
}