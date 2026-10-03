package com.seamline.controller;

import com.seamline.dto.AddOperationRequest;
import com.seamline.dto.CsvImportRequest;
import com.seamline.dto.StyleResponse;
import com.seamline.exception.ResourceNotFoundException;
import com.seamline.repository.StyleRepository;
import com.seamline.service.StyleService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Read model behind the "Style breakdown" screen, plus the two write actions
 *  ("Add operation", "Import from CSV") an Industrial Engineer uses to build it. */
@RestController
@RequestMapping("/api/styles")
public class StyleController {

    private final StyleRepository styles;
    private final StyleService styleService;

    public StyleController(StyleRepository styles, StyleService styleService) {
        this.styles = styles;
        this.styleService = styleService;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<List<StyleResponse>> all() {
        return ResponseEntity.ok(styles.findAll().stream().map(StyleResponse::from).toList());
    }

    @GetMapping("/{code}")
    @Transactional(readOnly = true)
    public ResponseEntity<StyleResponse> byCode(@PathVariable String code) {
        return styles.findByCodeIgnoreCase(code)
                .map(StyleResponse::from)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> ResourceNotFoundException.of("Style", code));
    }

    /** "Add operation" — appends one operation to the style's bulletin. */
    @PostMapping("/{code}/operations")
    @PreAuthorize("hasAnyRole('ADMIN', 'INDUSTRIAL_ENGINEER')")
    public ResponseEntity<StyleResponse> addOperation(@PathVariable String code,
                                                      @Valid @RequestBody AddOperationRequest request) {
        return ResponseEntity.ok(styleService.addOperation(code, request));
    }

    /** "Import from CSV" — appends a whole batch of operations at once. */
    @PostMapping("/{code}/operations/import")
    @PreAuthorize("hasAnyRole('ADMIN', 'INDUSTRIAL_ENGINEER')")
    public ResponseEntity<StyleResponse> importOperations(@PathVariable String code,
                                                           @Valid @RequestBody CsvImportRequest request) {
        return ResponseEntity.ok(styleService.importCsv(code, request.csv()));
    }
}
