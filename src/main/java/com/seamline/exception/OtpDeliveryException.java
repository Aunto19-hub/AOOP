package com.seamline.exception;

/** Thrown when Twilio can't be reached, isn't configured, or rejects a send/check request. */
public class OtpDeliveryException extends RuntimeException {

    public OtpDeliveryException(String message) {
        super(message);
    }
}
