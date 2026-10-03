package com.seamline.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * Parameters for "Re-run simulation". Every field is optional; anything left out
 * falls back to the default plan for the line.
 */
public record RunRequest(

        String algorithm,

        @Min(value = 4, message = "Team size must be at least 4")
        @Max(value = 40, message = "Team size must be 40 or fewer")
        Integer teamSize,

        @Min(1) @Max(20)
        Integer bufferPerStation,

        @DecimalMin("0.0") @DecimalMax("0.60")
        Double variability,

        Long seed) {

    public static final String DEFAULT_ALGORITHM = "rpw";
    public static final int DEFAULT_TEAM_SIZE = 14;
    public static final int DEFAULT_BUFFER = 3;
    public static final double DEFAULT_VARIABILITY = 0.12;
    public static final long DEFAULT_SEED = 42L;

    /** Null-safe accessors so the service never repeats the fallback logic. */
    public String algorithmOrDefault() {
        return algorithm == null || algorithm.isBlank() ? DEFAULT_ALGORITHM : algorithm;
    }

    public int teamSizeOrDefault() {
        return teamSize == null ? DEFAULT_TEAM_SIZE : teamSize;
    }

    public int bufferOrDefault() {
        return bufferPerStation == null ? DEFAULT_BUFFER : bufferPerStation;
    }

    public double variabilityOrDefault() {
        return variability == null ? DEFAULT_VARIABILITY : variability;
    }

    public long seedOrDefault() {
        return seed == null ? DEFAULT_SEED : seed;
    }

    public static RunRequest defaults() {
        return new RunRequest(null, null, null, null, null);
    }
}
