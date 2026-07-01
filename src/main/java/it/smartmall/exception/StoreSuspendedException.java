package it.smartmall.exception;

public class StoreSuspendedException extends RuntimeException {
    public StoreSuspendedException(String message) {
        super(message);
    }
}