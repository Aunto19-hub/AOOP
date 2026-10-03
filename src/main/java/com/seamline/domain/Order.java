package com.seamline.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;

/** A buyer order the factory is running against a deadline. */
@Entity
@Table(name = "orders")
public class Order extends BaseEntity {

    @Column(name = "order_code", nullable = false, unique = true, length = 40)
    private String orderCode;

    @Column(nullable = false, length = 120)
    private String buyer;

    @Column(nullable = false)
    private int quantity;

    @Column(name = "completed_quantity", nullable = false)
    private int completedQuantity;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "style_id")
    private Style style;

    protected Order() {
        // required by JPA
    }

    public Order(String orderCode, String buyer, int quantity, int completedQuantity, LocalDate dueDate) {
        this.orderCode = orderCode;
        this.buyer = buyer;
        this.quantity = quantity;
        this.completedQuantity = completedQuantity;
        this.dueDate = dueDate;
    }

    /** Progress as a whole percentage, which is what the order table shows. */
    public int progressPercent() {
        return quantity <= 0 ? 0 : (int) Math.round(completedQuantity * 100d / quantity);
    }

    public String getOrderCode() {
        return orderCode;
    }

    public String getBuyer() {
        return buyer;
    }

    public int getQuantity() {
        return quantity;
    }

    public int getCompletedQuantity() {
        return completedQuantity;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public Style getStyle() {
        return style;
    }

    public void setStyle(Style style) {
        this.style = style;
    }
}
