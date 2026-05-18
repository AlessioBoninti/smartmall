package it.smartmall.exception;

public class PastBookingCancellationException extends RuntimeException {
    public PastBookingCancellationException(String message) {
        super(message);
    }
}