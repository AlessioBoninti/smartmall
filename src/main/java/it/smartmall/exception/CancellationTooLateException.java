package it.smartmall.exception;

public class CancellationTooLateException extends RuntimeException {
    public CancellationTooLateException(String message) {
        super(message);
    }
}