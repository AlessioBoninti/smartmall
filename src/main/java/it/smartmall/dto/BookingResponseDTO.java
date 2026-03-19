package it.smartmall.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class BookingResponseDTO {
    private Long id;
    private Long storeId;
    private Long customerId;
    private LocalDateTime startDateTime;
    private LocalDateTime endDateTime;
    private String status;
}