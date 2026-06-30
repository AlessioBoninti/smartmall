package it.smartmall.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class MyBookingDTO {
    private Long id;
    private Long storeId;
    private String storeName;
    private LocalDateTime startDateTime;
    private LocalDateTime endDateTime;
    private String status;
}