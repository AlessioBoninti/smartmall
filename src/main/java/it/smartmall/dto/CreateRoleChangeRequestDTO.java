package it.smartmall.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateRoleChangeRequestDTO {

    @Size(max = 500, message = "Il motivo non può superare 500 caratteri")
    private String reason;
}
