package org.vino9.demo.genericrestcontroller.data;

public class AccessDeniedException extends Exception {
    public AccessDeniedException(String message) {
        super(message);
    }
}
