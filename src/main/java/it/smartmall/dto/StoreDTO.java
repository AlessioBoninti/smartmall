package it.smartmall.dto;

import lombok.Data;

@Data
public class StoreDTO {
    private Long id;
    private String name;
    private String status;
    private String suspendedReason;
}