package org.vino9.demo.genericrestcontroller.data;

public class EntityNotExistException extends Exception {
    public EntityNotExistException(String message) {
        super(message);
    }
}
