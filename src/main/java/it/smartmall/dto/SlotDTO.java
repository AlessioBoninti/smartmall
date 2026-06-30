package it.smartmall.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class SlotDTO {
    private LocalDateTime startDateTime;
    private LocalDateTime endDateTime;
    private int availableCapacity;
}