package com.seamline.dto;

/** One entry in the "start a direct message" list. {@code hasUnread} drives the Messenger-style unread dot. */
public record ChatContactResponse(String employeeId, String fullName, String roleLabel, boolean hasUnread) {
}
