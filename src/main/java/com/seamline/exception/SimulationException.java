package com.seamline.exception;

/** Thrown when a plan cannot be built or simulated with the given parameters. */
public class SimulationException extends RuntimeException {

    public SimulationException(String message) {
        super(message);
    }
}
