package com.seamline.service;

import com.seamline.domain.MachineType;
import com.seamline.domain.Operation;
import com.seamline.domain.Style;
import com.seamline.dto.AddOperationRequest;
import com.seamline.dto.StyleResponse;
import com.seamline.exception.ResourceNotFoundException;
import com.seamline.exception.SimulationException;
import com.seamline.repository.StyleRepository;
import com.seamline.service.balancing.GreedyTopologicalStrategy;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Write side of the style bulletin: adding one operation by hand, or importing
 * a batch from CSV. Both are two steps — persist the new operation rows, then
 * link the precedence graph now that every code (including ones in the same
 * batch) resolves to a real row — the same order {@code DataSeeder} uses to
 * build a style from scratch.
 */
@Service
public class StyleService {

    private final StyleRepository styles;
    private final GreedyTopologicalStrategy topologyCheck;

    public StyleService(StyleRepository styles, GreedyTopologicalStrategy topologyCheck) {
        this.styles = styles;
        this.topologyCheck = topologyCheck;
    }

    @Transactional
    public StyleResponse addOperation(String styleCode, AddOperationRequest request) {
        Style style = findStyle(styleCode);
        String code = request.code().trim();

        if (findOperation(style, code) != null) {
            throw new SimulationException("Operation " + code + " already exists on " + style.getCode());
        }

        Operation operation = new Operation(code, request.name().trim(), request.smv(),
                parseMachineType(request.machine()), request.minGrade(), nextSequenceNo(style));
        style.addOperation(operation);
        linkPredecessors(style, operation, request.predecessorsOrEmpty());
        assertAcyclic(style);

        return StyleResponse.from(styles.save(style));
    }

    @Transactional
    public StyleResponse importCsv(String styleCode, String csv) {
        Style style = findStyle(styleCode);
        List<String[]> rows = parseRows(csv);
        List<String> codesInOrder = new ArrayList<>(rows.size());

        // Pass 1: persist every new operation so each one has a real ID, the
        // same way a forward-referenced predecessor could never resolve otherwise.
        for (String[] row : rows) {
            String code = row[0].trim();
            if (code.isBlank()) {
                throw new SimulationException("A CSV row is missing its operation code");
            }
            if (findOperation(style, code) != null) {
                throw new SimulationException("Operation " + code + " already exists on " + style.getCode());
            }
            String name = row[1].trim();
            if (name.isBlank()) {
                throw new SimulationException("Operation " + code + " is missing a name");
            }
            style.addOperation(new Operation(code, name, parseSmv(row[2]),
                    parseMachineType(row[3]), parseGrade(row[4]), nextSequenceNo(style)));
            codesInOrder.add(code);
        }
        styles.saveAndFlush(style);

        // Pass 2: link the precedence graph now that every code in the batch resolves.
        for (int i = 0; i < rows.size(); i++) {
            String[] row = rows.get(i);
            String predecessorField = row.length > 5 ? row[5] : "";
            Operation operation = findOperation(style, codesInOrder.get(i));
            linkPredecessors(style, operation, splitPredecessors(predecessorField));
        }

        assertAcyclic(style);
        return StyleResponse.from(styles.save(style));
    }

    // ------------------------------------------------------------------

    private Style findStyle(String code) {
        return styles.findByCodeIgnoreCase(code)
                .orElseThrow(() -> ResourceNotFoundException.of("Style", code));
    }

    private Operation findOperation(Style style, String code) {
        return style.getOperations().stream()
                .filter(o -> o.getCode().equalsIgnoreCase(code))
                .findFirst().orElse(null);
    }

    private int nextSequenceNo(Style style) {
        return style.getOperations().stream().mapToInt(Operation::getSequenceNo).max().orElse(0) + 1;
    }

    private void linkPredecessors(Style style, Operation operation, List<String> predecessorCodes) {
        for (String rawCode : predecessorCodes) {
            String code = rawCode.trim();
            if (code.isEmpty()) {
                continue;
            }
            if (code.equalsIgnoreCase(operation.getCode())) {
                throw new SimulationException("Operation " + operation.getCode() + " cannot depend on itself");
            }
            Operation predecessor = findOperation(style, code);
            if (predecessor == null) {
                throw new SimulationException("Unknown predecessor code: " + code);
            }
            operation.addPredecessor(predecessor);
        }
    }

    /** Reuses the balancer's own cycle detection: a style that can't be
     *  topologically sorted can't be balanced or simulated either. */
    private void assertAcyclic(Style style) {
        topologyCheck.order(style.getOperations());
    }

    private MachineType parseMachineType(String raw) {
        try {
            return MachineType.valueOf(raw.trim().toUpperCase(Locale.ROOT).replace(' ', '_'));
        } catch (IllegalArgumentException ex) {
            throw new SimulationException("Unknown machine type: " + raw);
        }
    }

    private double parseSmv(String raw) {
        try {
            double value = Double.parseDouble(raw.trim());
            if (value <= 0 || value > 10) {
                throw new NumberFormatException();
            }
            return value;
        } catch (NumberFormatException ex) {
            throw new SimulationException("Invalid SMV: " + raw);
        }
    }

    private int parseGrade(String raw) {
        try {
            int value = Integer.parseInt(raw.trim());
            if (value < 1 || value > 5) {
                throw new NumberFormatException();
            }
            return value;
        } catch (NumberFormatException ex) {
            throw new SimulationException("Invalid minimum grade: " + raw);
        }
    }

    private List<String> splitPredecessors(String field) {
        if (field == null || field.isBlank()) {
            return List.of();
        }
        List<String> codes = new ArrayList<>();
        for (String part : field.split("[;|]")) {
            if (!part.isBlank()) {
                codes.add(part.trim());
            }
        }
        return codes;
    }

    /**
     * code,name,smv,machine,minGrade,predecessors — predecessors is optional and
     * semicolon-separated. A header row is detected (its "smv" cell won't parse as
     * a number) and skipped automatically.
     */
    private List<String[]> parseRows(String csv) {
        List<String[]> rows = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new StringReader(csv == null ? "" : csv))) {
            String line;
            boolean first = true;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) {
                    continue;
                }
                String[] cells = line.split(",", -1);
                if (first) {
                    first = false;
                    if (looksLikeHeader(cells)) {
                        continue;
                    }
                }
                if (cells.length < 5) {
                    throw new SimulationException(
                            "Each CSV row needs at least code,name,smv,machine,minGrade");
                }
                rows.add(cells);
            }
        } catch (IOException ex) {
            throw new SimulationException("Could not read the uploaded CSV");
        }
        if (rows.isEmpty()) {
            throw new SimulationException("The CSV had no operation rows");
        }
        return rows;
    }

    private boolean looksLikeHeader(String[] cells) {
        if (cells.length < 3) {
            return true;
        }
        try {
            Double.parseDouble(cells[2].trim());
            return false;
        } catch (NumberFormatException ex) {
            return true;
        }
    }
}
