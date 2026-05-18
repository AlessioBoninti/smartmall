package it.smartmall.exception;

public class StoreClosedException extends RuntimeException {
    public StoreClosedException(String message) {
        super(message);
    }
}