package it.smartmall.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AdminBookingDTO {
    private Long id;
    private Long storeId;
    private String storeName;
    private Long customerId;
    private String customerEmail;
    private LocalDateTime startDateTime;
    private LocalDateTime endDateTime;
    private String status;
}