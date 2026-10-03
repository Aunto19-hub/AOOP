package com.seamline.domain;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import java.time.Instant;

/**
 * Shared identity and audit state for every persistent entity.
 *
 * <p>This is the inheritance root of the domain model: subclasses inherit the
 * surrogate key, the creation timestamp and identity semantics instead of
 * redeclaring them.</p>
 */
@MappedSuperclass
public abstract class BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public Long getId() {
        return id;
    }

    protected void setId(Long id) {
        this.id = id;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public boolean isNew() {
        return id == null;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof BaseEntity entity)) {
            return false;
        }
        if (!entityType().equals(entity.entityType())) {
            return false;
        }
        return id != null && id.equals(entity.getId());
    }

    @Override
    public int hashCode() {
        return entityType().hashCode();
    }

    /** Hibernate proxies subclass the entity, so compare the declared type. */
    private Class<?> entityType() {
        Class<?> type = getClass();
        while (type.getSimpleName().contains("HibernateProxy")) {
            type = type.getSuperclass();
        }
        return type;
    }

    @Override
    public String toString() {
        return entityType().getSimpleName() + "#" + id;
    }
}
