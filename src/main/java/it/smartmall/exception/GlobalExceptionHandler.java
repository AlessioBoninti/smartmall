package it.smartmall.exception;

import it.smartmall.dto.ErrorResponseDTO;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // 1. Risorsa non trovata (Es. Negozio inesistente) -> 404 NOT FOUND
    @ExceptionHandler(StoreNotFoundException.class)
    public ResponseEntity<ErrorResponseDTO> handleStoreNotFound(StoreNotFoundException ex, HttpServletRequest request) {
        ErrorResponseDTO error = new ErrorResponseDTO(
                LocalDateTime.now(), HttpStatus.NOT_FOUND.value(), "Not Found", ex.getMessage(), request.getRequestURI()
        );
        return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
    }

    // 2. Conflitti di business (Es. Slot pieno, Negozio sospeso) -> 409 CONFLICT
    @ExceptionHandler({SlotFullException.class, StoreSuspendedException.class})
    public ResponseEntity<ErrorResponseDTO> handleConflictExceptions(RuntimeException ex, HttpServletRequest request) {
        ErrorResponseDTO error = new ErrorResponseDTO(
                LocalDateTime.now(), HttpStatus.CONFLICT.value(), "Conflict", ex.getMessage(), request.getRequestURI()
        );
        return new ResponseEntity<>(error, HttpStatus.CONFLICT);
    }

    // 3. Accesso non autorizzato (Es. Tento di cancellare prenotazioni altrui) -> 403 FORBIDDEN
    @ExceptionHandler(UnauthorizedBookingAccessException.class)
    public ResponseEntity<ErrorResponseDTO> handleUnauthorizedAccess(UnauthorizedBookingAccessException ex, HttpServletRequest request) {
        ErrorResponseDTO error = new ErrorResponseDTO(
                LocalDateTime.now(), HttpStatus.FORBIDDEN.value(), "Forbidden", ex.getMessage(), request.getRequestURI()
        );
        return new ResponseEntity<>(error, HttpStatus.FORBIDDEN);
    }

    // 4. Tutte le altre eccezioni generiche (Es. Orari sfasati, preavviso < 5 ore) -> 400 BAD REQUEST
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponseDTO> handleRuntimeException(RuntimeException ex, HttpServletRequest request) {
        ErrorResponseDTO error = new ErrorResponseDTO(
                LocalDateTime.now(), HttpStatus.BAD_REQUEST.value(), "Bad Request", ex.getMessage(), request.getRequestURI()
        );
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    // 5. Errori di validazione (@Valid, @NotNull, @Email, ecc.) -> 400 BAD REQUEST
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDTO> handleValidationExceptions(MethodArgumentNotValidException ex, HttpServletRequest request) {

        String errorMessage = ex.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
                .collect(Collectors.joining(", "));

        ErrorResponseDTO error = new ErrorResponseDTO(
                LocalDateTime.now(), HttpStatus.BAD_REQUEST.value(), "Validation Error", errorMessage, request.getRequestURI()
        );
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }
}