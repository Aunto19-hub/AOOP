package com.seamline.exception;

/** Thrown when sign-in credentials — password or Google token — do not check out. */
public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException() {
        super("Email or password is incorrect");
    }

    public InvalidCredentialsException(String message) {
        super(message);
    }
}
