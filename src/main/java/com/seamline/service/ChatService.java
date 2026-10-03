package com.seamline.service;

import com.seamline.domain.ChatMessage;
import com.seamline.domain.Employee;
import com.seamline.dto.ChatContactResponse;
import com.seamline.dto.ChatMessageResponse;
import com.seamline.dto.ChatSeenResponse;
import com.seamline.dto.ChatSendRequest;
import com.seamline.exception.ResourceNotFoundException;
import com.seamline.repository.ChatMessageRepository;
import com.seamline.repository.EmployeeRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The company-wide group channel, plus 1:1 direct messages between any two
 * employees — open to every role, no approval or line assignment needed. The
 * front end polls {@code since} the last message it has, so a full reload only
 * happens once per conversation.
 */
@Service
public class ChatService {

    private static final int RECENT_LIMIT = 50;

    private final ChatMessageRepository messages;
    private final EmployeeRepository employees;

    public ChatService(ChatMessageRepository messages, EmployeeRepository employees) {
        this.messages = messages;
        this.employees = employees;
    }

    @Transactional(readOnly = true)
    public List<ChatContactResponse> contacts(Employee viewer) {
        return employees.findAll().stream()
                .filter(e -> !e.getId().equals(viewer.getId()))
                .filter(Employee::isEnabled)
                .sorted(Comparator.comparing(Employee::getFullName))
                .map(e -> new ChatContactResponse(e.getEmployeeId(), e.getFullName(), e.getRole().getLabel(),
                        messages.existsBySenderAndRecipientAndReadAtIsNull(e, viewer)))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ChatMessageResponse> groupMessages(Instant since) {
        List<ChatMessage> rows;
        if (since == null) {
            rows = new ArrayList<>(messages.findRecentGroup(PageRequest.of(0, RECENT_LIMIT)));
            Collections.reverse(rows);
        } else {
            rows = messages.findByRecipientIsNullAndCreatedAtAfterOrderByCreatedAtAsc(since);
        }
        return rows.stream().map(ChatMessageResponse::from).toList();
    }

    @Transactional
    public ChatMessageResponse sendGroupMessage(Employee sender, ChatSendRequest request) {
        ChatMessage saved = messages.save(new ChatMessage(sender, null, request.body().trim()));
        return ChatMessageResponse.from(saved);
    }

    @Transactional
    public List<ChatMessageResponse> directMessages(Employee viewer, String otherEmployeeId, Instant since) {
        Employee other = resolveEmployee(otherEmployeeId);
        messages.markThreadRead(viewer, other, Instant.now());
        List<ChatMessage> rows;
        if (since == null) {
            rows = new ArrayList<>(messages.findRecentThread(viewer, other, PageRequest.of(0, RECENT_LIMIT)));
            Collections.reverse(rows);
        } else {
            rows = messages.findThreadSince(viewer, other, since);
        }
        return rows.stream().map(ChatMessageResponse::from).toList();
    }

    /** How far {@code otherEmployeeId} has read into the messages {@code viewer} sent them. */
    @Transactional(readOnly = true)
    public ChatSeenResponse directSeenStatus(Employee viewer, String otherEmployeeId) {
        Employee other = resolveEmployee(otherEmployeeId);
        return new ChatSeenResponse(messages.findLastReadAt(viewer, other));
    }

    @Transactional
    public ChatMessageResponse sendDirectMessage(Employee sender, String otherEmployeeId, ChatSendRequest request) {
        Employee other = resolveEmployee(otherEmployeeId);
        if (other.getId().equals(sender.getId())) {
            throw new IllegalArgumentException("You can't message yourself");
        }
        ChatMessage saved = messages.save(new ChatMessage(sender, other, request.body().trim()));
        return ChatMessageResponse.from(saved);
    }

    private Employee resolveEmployee(String employeeId) {
        return employees.findByEmployeeIdIgnoreCase(employeeId)
                .orElseThrow(() -> ResourceNotFoundException.of("Employee", employeeId));
    }
}
