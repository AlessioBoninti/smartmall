package it.smartmall.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class BookingRequestDTO {

    @NotNull(message = "L'ID dello store è obbligatorio")
    private Long storeId;

   
    @NotNull(message = "L'orario di inizio è obbligatorio")
    @Future(message = "Non puoi prenotare uno slot nel passato")
    private LocalDateTime startDateTime;

}