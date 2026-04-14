package it.smartmall.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;

@Entity
@Table(name = "bookings")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    // Relazione: Molte prenotazioni sono fatte da un singolo Utente
    @ManyToOne(fetch = FetchType.LAZY) // <-- AGGIUNTO: Risparmia memoria!
    @JoinColumn(name = "customer_id", nullable = false)
    private User customer;

    // Relazione: Molte prenotazioni appartengono a un singolo Negozio
    @ManyToOne(fetch = FetchType.LAZY) // <-- AGGIUNTO: Risparmia memoria!
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @Column(nullable = false)
    private LocalDateTime startDateTime;

    @Column(nullable = false)
    private LocalDateTime endDateTime;

    // Le prenotazioni vengono create direttamente come CONFIRMED
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BookingStatus status = BookingStatus.CONFIRMED;
}