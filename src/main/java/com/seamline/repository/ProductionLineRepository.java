package com.seamline.repository;

import com.seamline.domain.ProductionLine;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductionLineRepository extends JpaRepository<ProductionLine, Long> {

    Optional<ProductionLine> findByCodeIgnoreCase(String code);

    List<ProductionLine> findAllByOrderByCodeAsc();

    boolean existsByCodeIgnoreCase(String code);
}
