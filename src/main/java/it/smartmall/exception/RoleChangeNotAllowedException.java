package it.smartmall.exception;

public class RoleChangeNotAllowedException extends RuntimeException {
    public RoleChangeNotAllowedException(String message) {
        super(message);
    }
}