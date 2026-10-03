package com.seamline.dto;

import com.seamline.domain.ChatMessage;
import java.time.Instant;

/**
 * One message, in the group channel or a direct thread. {@code readAt} is
 * always null for a group message — a broadcast has no single "seen" state.
 */
public record ChatMessageResponse(Long id,
                                  String senderEmployeeId,
                                  String senderName,
                                  Instant sentAt,
                                  String body,
                                  Instant readAt) {

    public static ChatMessageResponse from(ChatMessage message) {
        return new ChatMessageResponse(
                message.getId(),
                message.getSender().getEmployeeId(),
                message.getSender().getFullName(),
                message.getCreatedAt(),
                message.getBody(),
                message.getReadAt());
    }
}
