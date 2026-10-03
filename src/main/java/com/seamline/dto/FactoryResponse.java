package com.seamline.dto;

import com.seamline.domain.Order;
import java.time.LocalDate;

/** Payloads for the factory-wide screens (all lines, orders). */
public final class FactoryResponse {

    private FactoryResponse() {
    }

    /** One row of the "All lines" table. */
    public record Line(String lineCode,
                       String unitName,
                       String style,
                       int targetRatePerHour,
                       int outputThisShift,
                       int targetPiecesPerShift,
                       double lineEfficiency,
                       String status) {
    }

    /** One row of the "Orders" table. */
    public record OrderRow(String orderCode,
                           String buyer,
                           String style,
                           int quantity,
                           LocalDate dueDate,
                           int progress) {

        public static OrderRow from(Order order) {
            return new OrderRow(
                    order.getOrderCode(),
                    order.getBuyer(),
                    order.getStyle() == null ? null : order.getStyle().getName(),
                    order.getQuantity(),
                    order.getDueDate(),
                    order.progressPercent());
        }
    }
}
