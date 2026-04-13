package it.smartmall.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class StoreDTO {
    private Long id;
    private String name;

    // Si possono aggiungere info sulla sospensione se si vogliono mostrare al front-end,
    // in modo che possa mostrare un badge "Chiuso Temporaneamente"
    private boolean isSuspended;
    private String suspendedReason;
}