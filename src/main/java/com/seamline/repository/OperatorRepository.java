package com.seamline.repository;

import com.seamline.domain.Operator;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OperatorRepository extends JpaRepository<Operator, Long> {

    List<Operator> findAllByOrderByCodeAsc();

    Optional<Operator> findByCodeIgnoreCase(String code);
}
