package it.smartmall.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonIgnore;

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

    // Ogni negozio è gestito da un MERCHANT (che è un User)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "merchant_id", nullable = false)
    @JsonIgnore 
    private User merchant;

    // Gestione Sospensione:
    private LocalDateTime suspendedFrom;
    private LocalDateTime suspendedTo;
    private String suspendedReason;

}