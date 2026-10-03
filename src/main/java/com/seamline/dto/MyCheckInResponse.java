package com.seamline.dto;

import java.time.Instant;

/** Whether the caller has already checked in today, for the "Check in" button's own state. */
public record MyCheckInResponse(boolean checkedIn, Instant checkedInAt) {
}
