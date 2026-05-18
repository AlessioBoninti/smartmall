package it.smartmall.exception;

public class InvalidSlotException extends RuntimeException {
    public InvalidSlotException(String message) {
        super(message);
    }
}