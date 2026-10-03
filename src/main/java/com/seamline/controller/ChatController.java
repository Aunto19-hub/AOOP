package com.seamline.controller;

import com.seamline.dto.ChatContactResponse;
import com.seamline.dto.ChatMessageResponse;
import com.seamline.dto.ChatSeenResponse;
import com.seamline.dto.ChatSendRequest;
import com.seamline.security.SeamlineUserDetails;
import com.seamline.service.ChatService;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Group channel + 1:1 direct messages. Open to every signed-in role, no admin gate. */
@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @GetMapping("/contacts")
    public ResponseEntity<List<ChatContactResponse>> contacts(@AuthenticationPrincipal SeamlineUserDetails principal) {
        return ResponseEntity.ok(chatService.contacts(principal.getEmployee()));
    }

    /** {@code since} (ISO instant) returns only newer messages, for polling; omit it for the recent history. */
    @GetMapping("/group")
    public ResponseEntity<List<ChatMessageResponse>> group(@RequestParam(required = false) String since) {
        return ResponseEntity.ok(chatService.groupMessages(parseInstant(since)));
    }

    @PostMapping("/group")
    public ResponseEntity<ChatMessageResponse> sendGroup(@AuthenticationPrincipal SeamlineUserDetails principal,
                                                         @Valid @RequestBody ChatSendRequest request) {
        return ResponseEntity.ok(chatService.sendGroupMessage(principal.getEmployee(), request));
    }

    @GetMapping("/direct/{employeeId}")
    public ResponseEntity<List<ChatMessageResponse>> direct(@AuthenticationPrincipal SeamlineUserDetails principal,
                                                            @PathVariable String employeeId,
                                                            @RequestParam(required = false) String since) {
        return ResponseEntity.ok(chatService.directMessages(principal.getEmployee(), employeeId, parseInstant(since)));
    }

    @PostMapping("/direct/{employeeId}")
    public ResponseEntity<ChatMessageResponse> sendDirect(@AuthenticationPrincipal SeamlineUserDetails principal,
                                                          @PathVariable String employeeId,
                                                          @Valid @RequestBody ChatSendRequest request) {
        return ResponseEntity.ok(chatService.sendDirectMessage(principal.getEmployee(), employeeId, request));
    }

    /** How far {@code employeeId} has read into messages the caller sent them — drives the "seen" tag. */
    @GetMapping("/direct/{employeeId}/seen")
    public ResponseEntity<ChatSeenResponse> seenStatus(@AuthenticationPrincipal SeamlineUserDetails principal,
                                                       @PathVariable String employeeId) {
        return ResponseEntity.ok(chatService.directSeenStatus(principal.getEmployee(), employeeId));
    }

    private Instant parseInstant(String value) {
        return value == null || value.isBlank() ? null : Instant.parse(value);
    }
}
