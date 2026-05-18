package it.smartmall.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalTime;

@Data
public class AvailabilityRuleRequestDTO {

    @NotNull(message = "Il giorno della settimana e obbligatorio")
    @Min(value = 1, message = "Il giorno deve essere tra 1 e 7")
    @Max(value = 7, message = "Il giorno deve essere tra 1 e 7")
    private Integer dayOfWeek;

    private LocalTime morningStartTime;

    private LocalTime morningEndTime;

    private LocalTime afternoonStartTime;

    private LocalTime afternoonEndTime;

    private Boolean closed = false;

    @NotNull(message = "La durata dello slot e obbligatoria")
    @Min(value = 5, message = "La durata minima dello slot e 5 minuti")
    private Integer slotMinutes;

    @NotNull(message = "La capacita per slot e obbligatoria")
    @Min(value = 1, message = "La capacita deve essere almeno 1")
    private Integer capacityPerSlot;

    private Boolean active = true;
}
