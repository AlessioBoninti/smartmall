package it.smartmall.dto;

import lombok.Data;

@Data
public class AdminUserDTO {
    private Long id;
    private String email;
    private String role;
}