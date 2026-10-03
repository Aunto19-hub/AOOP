package com.seamline.repository;

import com.seamline.domain.Style;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StyleRepository extends JpaRepository<Style, Long> {

    Optional<Style> findByCodeIgnoreCase(String code);
}
