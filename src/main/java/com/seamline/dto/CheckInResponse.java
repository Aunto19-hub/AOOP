package com.seamline.dto;

import java.time.Instant;

/** One employee's check-in for the day, for the supervisor's "who's here" list. */
public record CheckInResponse(String employeeId, String fullName, String roleLabel, Instant checkedInAt) {
}
