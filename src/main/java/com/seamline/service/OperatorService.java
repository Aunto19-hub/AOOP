package com.seamline.service;

import com.seamline.domain.MachineType;
import com.seamline.domain.Operator;
import com.seamline.dto.AddOperatorRequest;
import com.seamline.dto.OperatorResponse;
import com.seamline.exception.SimulationException;
import com.seamline.repository.OperatorRepository;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Write side of the roster: adding one operator by hand, or importing a batch
 * from CSV. Both return the full, freshly-sorted roster the skill matrix draws.
 */
@Service
public class OperatorService {

    private final OperatorRepository operators;

    public OperatorService(OperatorRepository operators) {
        this.operators = operators;
    }

    @Transactional
    public List<OperatorResponse> addOperator(AddOperatorRequest request) {
        String code = request.code().trim();
        if (operators.findByCodeIgnoreCase(code).isPresent()) {
            throw new SimulationException("Operator " + code + " already exists");
        }

        Operator operator = new Operator(code, request.name().trim(), request.grade(), request.defaultEfficiency());
        applySkills(operator, request.skillsOrEmpty());
        operators.save(operator);

        return allSorted();
    }

    @Transactional
    public List<OperatorResponse> importCsv(String csv) {
        List<String[]> rows = parseRows(csv);

        for (String[] row : rows) {
            String code = row[0].trim();
            if (code.isBlank()) {
                throw new SimulationException("A CSV row is missing its operator code");
            }
            if (operators.findByCodeIgnoreCase(code).isPresent()) {
                throw new SimulationException("Operator " + code + " already exists");
            }
            String name = row[1].trim();
            if (name.isBlank()) {
                throw new SimulationException("Operator " + code + " is missing a name");
            }

            Operator operator = new Operator(code, name, parseGrade(row[2]),
                    parseEfficiency(row[3], "default efficiency"));
            String skillsField = row.length > 4 ? row[4] : "";
            applySkills(operator, parseSkillsField(skillsField));
            operators.save(operator);
        }

        return allSorted();
    }

    // ------------------------------------------------------------------

    private List<OperatorResponse> allSorted() {
        return operators.findAllByOrderByCodeAsc().stream().map(OperatorResponse::from).toList();
    }

    private void applySkills(Operator operator, Map<String, Double> skills) {
        skills.forEach((machine, efficiency) -> operator.addSkill(parseMachineType(machine), efficiency));
    }

    private MachineType parseMachineType(String raw) {
        try {
            return MachineType.valueOf(raw.trim().toUpperCase(Locale.ROOT).replace(' ', '_'));
        } catch (IllegalArgumentException ex) {
            throw new SimulationException("Unknown machine type: " + raw);
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
            throw new SimulationException("Invalid grade: " + raw);
        }
    }

    private double parseEfficiency(String raw, String fieldLabel) {
        try {
            double value = Double.parseDouble(raw.trim());
            if (value <= 0 || value > 2.0) {
                throw new NumberFormatException();
            }
            return value;
        } catch (NumberFormatException ex) {
            throw new SimulationException("Invalid " + fieldLabel + ": " + raw);
        }
    }

    private Map<String, Double> parseSkillsField(String field) {
        Map<String, Double> skills = new LinkedHashMap<>();
        if (field == null || field.isBlank()) {
            return skills;
        }
        for (String part : field.split(";")) {
            if (part.isBlank()) {
                continue;
            }
            String[] kv = part.split(":", 2);
            if (kv.length != 2) {
                throw new SimulationException("Skill entries must be machine:efficiency, got: " + part);
            }
            skills.put(kv[0].trim(), parseEfficiency(kv[1], "skill efficiency for " + kv[0].trim()));
        }
        return skills;
    }

    /**
     * code,name,grade,defaultEfficiency,skills — skills is optional, semicolon-separated
     * machine:efficiency pairs. A header row is detected (its "grade" cell won't parse
     * as a number) and skipped automatically.
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
                if (cells.length < 4) {
                    throw new SimulationException(
                            "Each CSV row needs at least code,name,grade,defaultEfficiency");
                }
                rows.add(cells);
            }
        } catch (IOException ex) {
            throw new SimulationException("Could not read the uploaded CSV");
        }
        if (rows.isEmpty()) {
            throw new SimulationException("The CSV had no operator rows");
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
