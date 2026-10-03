package com.seamline.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * One chat message. A null {@link #recipient} means it was posted to the
 * company-wide group channel; otherwise it's a 1:1 direct message between
 * {@link #sender} and {@link #recipient}. {@code createdAt} (from
 * {@link BaseEntity}) doubles as the sent time. {@link #readAt} is only ever
 * set on direct messages — a group broadcast has no single "seen" state.
 */
@Entity
@Table(name = "chat_messages")
public class ChatMessage extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sender_id", nullable = false)
    private Employee sender;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipient_id")
    private Employee recipient;

    @Column(nullable = false, length = 2000)
    private String body;

    @Column(name = "read_at")
    private Instant readAt;

    protected ChatMessage() {
        // required by JPA
    }

    public ChatMessage(Employee sender, Employee recipient, String body) {
        this.sender = sender;
        this.recipient = recipient;
        this.body = body;
    }

    public Employee getSender() {
        return sender;
    }

    public Employee getRecipient() {
        return recipient;
    }

    public String getBody() {
        return body;
    }

    public Instant getReadAt() {
        return readAt;
    }
}
