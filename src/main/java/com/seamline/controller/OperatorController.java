package com.seamline.controller;

import com.seamline.dto.AddOperatorRequest;
import com.seamline.dto.CsvImportRequest;
import com.seamline.dto.OperatorResponse;
import com.seamline.repository.OperatorRepository;
import com.seamline.service.OperatorService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Read model behind the "Operators" skill matrix, plus the two write actions
 *  ("Add operator", "Import roster") an Industrial Engineer uses to build it. */
@RestController
@RequestMapping("/api/operators")
public class OperatorController {

    private final OperatorRepository operators;
    private final OperatorService operatorService;

    public OperatorController(OperatorRepository operators, OperatorService operatorService) {
        this.operators = operators;
        this.operatorService = operatorService;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<List<OperatorResponse>> all() {
        return ResponseEntity.ok(operators.findAllByOrderByCodeAsc().stream()
                .map(OperatorResponse::from)
                .toList());
    }

    /** "Add operator" — appends one operator to the roster. */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'INDUSTRIAL_ENGINEER')")
    public ResponseEntity<List<OperatorResponse>> add(@Valid @RequestBody AddOperatorRequest request) {
        return ResponseEntity.ok(operatorService.addOperator(request));
    }

    /** "Import roster" — appends a whole batch of operators at once. */
    @PostMapping("/import")
    @PreAuthorize("hasAnyRole('ADMIN', 'INDUSTRIAL_ENGINEER')")
    public ResponseEntity<List<OperatorResponse>> importOperators(@Valid @RequestBody CsvImportRequest request) {
        return ResponseEntity.ok(operatorService.importCsv(request.csv()));
    }
}
