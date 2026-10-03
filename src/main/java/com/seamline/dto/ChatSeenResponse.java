package com.seamline.dto;

import java.time.Instant;

/** How far a direct-message peer has read into messages sent to them; null means none yet. */
public record ChatSeenResponse(Instant readAt) {
}
