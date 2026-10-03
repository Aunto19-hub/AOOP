package com.seamline.service;

import com.seamline.domain.Employee;
import com.seamline.domain.OperatorRequest;
import com.seamline.dto.OperatorRequestCreateRequest;
import com.seamline.dto.OperatorRequestResponse;
import com.seamline.dto.ResolveRequest;
import com.seamline.exception.ResourceNotFoundException;
import com.seamline.repository.OperatorRequestRepository;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Everything an operator can flag or ask for — a broken machine, a time-off
 * request, a shift swap, a suggestion — as one shared workflow: submit, then a
 * supervisor or admin closes it out with an optional note.
 */
@Service
public class OperatorRequestService {

    private final OperatorRequestRepository requests;

    public OperatorRequestService(OperatorRequestRepository requests) {
        this.requests = requests;
    }

    @Transactional
    public OperatorRequestResponse create(Employee requester, OperatorRequestCreateRequest body) {
        OperatorRequest saved = requests.save(
                new OperatorRequest(requester, body.type(), body.description().trim()));
        return OperatorRequestResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<OperatorRequestResponse> all() {
        return requests.findAllByOrderByCreatedAtDesc().stream().map(OperatorRequestResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<OperatorRequestResponse> mine(Employee requester) {
        return requests.findByRequesterOrderByCreatedAtDesc(requester).stream()
                .map(OperatorRequestResponse::from).toList();
    }

    @Transactional
    public OperatorRequestResponse resolve(Employee resolver, Long id, ResolveRequest body) {
        OperatorRequest request = requests.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Request", String.valueOf(id)));
        if (request.isResolved()) {
            throw new IllegalArgumentException("That request is already resolved");
        }
        String note = body.note() == null || body.note().isBlank() ? null : body.note().trim();
        request.resolve(resolver, note, Instant.now());
        return OperatorRequestResponse.from(request);
    }
}
