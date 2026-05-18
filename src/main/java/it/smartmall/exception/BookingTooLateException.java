package it.smartmall.exception;

public class BookingTooLateException extends RuntimeException {
    public BookingTooLateException(String message) {
        super(message);
    }
}