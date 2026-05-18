package it.smartmall.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SuspendStoreRequest {

    @NotNull(message = "La data di inizio sospensione è obbligatoria")
    private LocalDateTime from;

    @NotNull(message = "La data di fine sospensione è obbligatoria")
    @Future(message = "La data di fine sospensione deve essere nel futuro")
    private LocalDateTime to;

    @NotBlank(message = "Il motivo della sospensione è obbligatorio")
    private String reason;
}