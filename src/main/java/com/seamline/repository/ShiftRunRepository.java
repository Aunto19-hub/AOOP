package com.seamline.repository;

import com.seamline.domain.ProductionLine;
import com.seamline.domain.ShiftRun;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShiftRunRepository extends JpaRepository<ShiftRun, Long> {

    Optional<ShiftRun> findFirstByLineOrderByRunAtDescIdDesc(ProductionLine line);
}
