package it.smartmall.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonIgnore; // <-- 1. NUOVO IMPORT

@Entity
@Table(name = "stores")
@Data
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Store {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    // Relazione: Ogni negozio è gestito da un MERCHANT (che è un User)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "merchant_id", nullable = false)
    @JsonIgnore // <-- 2. AGGIUNTO QUESTO: nasconde il merchant dal JSON
    private User merchant;

    // --- Punto 7 della Relazione: Gestione Sospensione ---
    private LocalDateTime suspendedFrom;
    private LocalDateTime suspendedTo;
    private String suspendedReason;

}