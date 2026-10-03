package com.seamline.exception;

/** Thrown when a line, style or run referenced by the client does not exist. */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public static ResourceNotFoundException of(String type, String key) {
        return new ResourceNotFoundException(type + " '" + key + "' was not found");
    }
}
