package com.seamline.service;

import com.seamline.domain.ProductionLine;
import com.seamline.domain.ShiftRun;
import com.seamline.domain.Style;
import com.seamline.dto.CreateLineRequest;
import com.seamline.dto.FactoryResponse;
import com.seamline.repository.OrderRepository;
import com.seamline.repository.ProductionLineRepository;
import com.seamline.repository.ShiftRunRepository;
import com.seamline.repository.StyleRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Factory-wide read models: every line's latest shift, and the order book. */
@Service
public class FactoryService {

    private final ProductionLineRepository lines;
    private final ShiftRunRepository runs;
    private final OrderRepository orders;
    private final StyleRepository styles;

    public FactoryService(ProductionLineRepository lines, ShiftRunRepository runs, OrderRepository orders,
                          StyleRepository styles) {
        this.lines = lines;
        this.runs = runs;
        this.orders = orders;
        this.styles = styles;
    }

    @Transactional(readOnly = true)
    public List<FactoryResponse.Line> allLines() {
        List<FactoryResponse.Line> rows = new ArrayList<>();
        for (ProductionLine line : lines.findAllByOrderByCodeAsc()) {
            rows.add(toLine(line));
        }
        return rows;
    }

    /**
     * A new line for the factory, picked up by every management screen the
     * moment it's created — no shift has run on it yet, so it starts at zero
     * output and status WATCH until the Industrial Engineer balances and
     * simulates it.
     */
    @Transactional
    public FactoryResponse.Line createLine(CreateLineRequest request) {
        String code = request.code().trim();
        if (lines.existsByCodeIgnoreCase(code)) {
            throw new IllegalArgumentException("A line with the code '" + code + "' already exists");
        }

        ProductionLine line = new ProductionLine(code,
                request.unitName() == null || request.unitName().isBlank() ? null : request.unitName().trim(),
                480, request.targetRatePerHour());

        if (request.styleCode() != null && !request.styleCode().isBlank()) {
            Style style = styles.findByCodeIgnoreCase(request.styleCode().trim())
                    .orElseThrow(() -> new IllegalArgumentException("Unknown style code: " + request.styleCode()));
            line.setCurrentStyle(style);
        }

        return toLine(lines.save(line));
    }

    private FactoryResponse.Line toLine(ProductionLine line) {
        Optional<ShiftRun> latest = runs.findFirstByLineOrderByRunAtDescIdDesc(line);
        int output = latest.map(ShiftRun::getPiecesProduced).orElse(0);
        double efficiency = latest.map(ShiftRun::getLineEfficiency).orElse(0d);
        int target = line.targetPiecesPerShift();

        return new FactoryResponse.Line(
                line.getCode(),
                line.getUnitName(),
                line.getCurrentStyle() == null ? null : line.getCurrentStyle().getName(),
                line.getTargetRatePerHour(),
                output,
                target,
                Math.round(efficiency * 10d) / 10d,
                status(output, target, latest.isPresent()));
    }

    @Transactional(readOnly = true)
    public List<FactoryResponse.OrderRow> allOrders() {
        return orders.findAllByOrderByDueDateAsc().stream()
                .map(FactoryResponse.OrderRow::from)
                .toList();
    }

    /** A line is at risk when it is more than a tenth short of its shift target. */
    private String status(int output, int target, boolean hasRun) {
        if (!hasRun || target <= 0) {
            return "WATCH";
        }
        double ratio = output / (double) target;
        if (ratio >= 0.95) {
            return "HEALTHY";
        }
        return ratio >= 0.90 ? "WATCH" : "AT_RISK";
    }
}
