package it.smartmall.dto;

import lombok.Data;

import java.time.LocalTime;

@Data
public class AvailabilityRuleDTO {
    private Long id;
    private Long storeId;
    private Integer dayOfWeek;
    private LocalTime morningStartTime;
    private LocalTime morningEndTime;
    private LocalTime afternoonStartTime;
    private LocalTime afternoonEndTime;
    private Boolean closed;
    private Integer slotMinutes;
    private Integer capacityPerSlot;
    private Boolean active;
}
