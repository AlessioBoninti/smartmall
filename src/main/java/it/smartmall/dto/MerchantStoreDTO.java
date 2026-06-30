package it.smartmall.dto;

import lombok.Data;

@Data
public class MerchantStoreDTO {
    private Long id;
    private String name;
    private String status;
    private String suspendedReason;
}