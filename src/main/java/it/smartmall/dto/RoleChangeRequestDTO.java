package it.smartmall.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class RoleChangeRequestDTO {
    private Long id;
    private Long requesterId;
    private String requesterEmail;
    private String requestedRole;
    private String status;
    private String reason;
    private LocalDateTime createdAt;
    private LocalDateTime reviewedAt;
    private Long reviewedById;
    private String reviewedByEmail;
}
