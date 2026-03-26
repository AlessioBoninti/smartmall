package it.smartmall.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "bookings")
@Data
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Relazione: Molte prenotazioni sono fatte da un singolo Utente
    @ManyToOne
    @JoinColumn(name = "customer_id", nullable = false)
    private User customer;

    // Relazione: Molte prenotazioni appartengono a un singolo Negozio
    @ManyToOne
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @Column(nullable = false)
    private LocalDateTime startDateTime; // Quando inizia lo slot

    @Column(nullable = false)
    private LocalDateTime endDateTime;   // Quando finisce lo slot

    // Punto 3: Vengono create direttamente come CONFIRMED
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BookingStatus status = BookingStatus.CONFIRMED;

}