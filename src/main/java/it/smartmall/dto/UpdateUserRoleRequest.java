package it.smartmall.dto;

import jakarta.validation.constraints.NotNull;
import it.smartmall.model.Role;
import lombok.Data;

@Data
public class UpdateUserRoleRequest {

    @NotNull(message = "Il ruolo è obbligatorio")
    private Role role;
}