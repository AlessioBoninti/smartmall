package it.smartmall.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AdminStoreDTO {
    private Long id;
    private String name;
    private String status;
    private Long merchantId;
    private String merchantEmail;
    private LocalDateTime suspendedFrom;
    private LocalDateTime suspendedTo;
    private String suspendedReason;
}